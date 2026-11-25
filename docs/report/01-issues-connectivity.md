# Issues Encountered: Mất kết nối trong lúc xử lý giao dịch chấm công

Phần này trả lời yêu cầu **"Propose architectural enhancements ... explain how it mitigates ... how it should be incorporated"** của đề bài M2 đối với vấn đề **I-1: Hệ thống mất kết nối trong lúc đang xử lý giao dịch chấm công** (Team B viết chính; bằng chứng phối hợp từ Team A và Team D).

---

## 1. Vấn đề

Trong một lượt quét QR tại cổng, yêu cầu đi qua nhiều bước tuần tự qua các đường mạng và kho lưu trữ khác nhau (`Điện thoại → Kiosk → Gate Station → User/QR → Attendance → Turnstile → RabbitMQ → Central`). Khi kết nối bị gián đoạn **giữa chừng** lúc giao dịch đang chạy, hệ thống đối mặt với bảy điểm đứt cụ thể:

| Điểm đứt | Tình huống thực tế | Rủi ro nếu không xử lý |
| --- | --- | --- |
| **P1** | Kiosk gửi `POST /kiosk/v1/scans` nhưng không nhận được phản hồi từ Edge (đứt mạng hoặc timeout khi trả lời) | Kiosk không biết Edge đã xử lý chưa; gửi lại hoặc quét lại có thể mở cổng 2 lần hoặc tạo 2 bản ghi |
| **P2** | Edge đã xác thực QR và giữ chỗ token (`jti`) trong Redis/Attendance (`RESERVED`), nhưng crash hoặc lỗi trước khi gửi lệnh mở cổng | Token bị kẹt ở trạng thái giữ chỗ, nhân viên không thể quét lại dù cổng chưa từng mở |
| **P3** | Edge đã gửi lệnh mở cổng tới Turnstile nhưng mất kết nối hoặc quá thời gian chờ phản hồi | Không biết cổng đã mở hay chưa; nếu tự động gửi lại lệnh sẽ mở cổng 2 lần; nếu hủy bỏ thì người đã đi qua bị mất công |
| **P4** | Edge đã gửi lệnh mở cổng (`GATE_COMMANDED`) thì tiến trình Edge bị dừng/crash trước khi ghi kết quả | Sau khi khởi động lại, không có cơ chế nhận biết lệnh đã phát ra hay chưa, gây mất bản ghi chấm công |
| **P5** | Turnstile đã mở, Edge đã ghi hoặc đang ghi kết quả, nhưng phản hồi về kiosk bị rớt (hoặc Edge không lưu kịp kết quả sau khi cổng đã mở) | Kiosk báo lỗi hoặc chưa đồng bộ được trạng thái dù cổng đã mở |
| **P6** | Mất LAN giữa kiosk và máy chủ biên trước hoặc trong lúc quét | Nhân viên bị chặn tại cổng khi đứt mạng cục bộ |
| **P7** | Đứt WAN hoặc RabbitMQ dừng hoạt động sau khi `attendance` đã commit lượt quét | Sự kiện `AttendanceConfirmed` / `GateResultRecorded` bị mất hoặc phát trùng khi broker hoạt động lại |

---

## 2. Vì sao thiết kế M1 chưa đủ

Theo phân tích tại `docs/adr/README.md` (mục 1), bản thiết kế M1 đã đề cập hai tình huống ngoại biên là chế độ offline tại kiosk khi mất mạng từ đầu (**P6**, mục 14.5 và UC-08 ngoại lệ 2.x) và Outbox đẩy lại khi đứt WAN (**P7**), đồng thời nhắc chung trạng thái `PENDING_GATE`/`UNKNOWN` cho trường hợp cổng không phản hồi (**P3**). Tuy nhiên, M1 còn bốn khoảng trống khiến hệ thống chưa chứng minh được tiêu chí **"không mất, không trùng"** khi mất kết nối giữa chừng:

1. **Chưa có máy trạng thái tường minh cho một lượt quét:** Không phân định rõ ranh giới giữa bước giữ chỗ (`RESERVED`), bước đã ra lệnh vật lý (`GATE_COMMANDED`) và bước đã mở (`GATE_OPENED`).
2. **Thiếu quy tắc phục hồi ở từng điểm đứt P1, P2, P4, P5:** Chưa chỉ rõ khi crash trước hoặc sau lệnh mở cổng thì thành phần nào dọn dẹp giữ chỗ, thành phần nào đối soát, và khi nào được phép hoặc cấm gửi lại lệnh.
3. **Kiosk chưa ghi nhật ký trước khi gọi mạng:** "Offline Queue" ở M1 chỉ ghi khi kiosk *đã biết* mất mạng, nên nếu đứt kết nối ngay giữa lúc gọi API thì kiosk không giữ được dấu vết lượt quét.
4. **Chưa có khóa idempotency xuyên suốt và chưa có bằng chứng kiểm thử thực thi** (unit test, integration test trên PostgreSQL/RabbitMQ thật và kịch bản fault-injection).

---

## 3. Giải pháp kiến trúc (ADR-11 & ADR-13)

Hệ thống áp dụng mô hình **offline-first, store-and-forward** kết hợp **máy trạng thái tường minh** (`AccessAttempt`) và **khóa idempotency xuyên suốt** (`attempt_id`):

1. **Một khóa idempotency xuyên suốt (`attempt_id` – UUID v7):**
   - Kiosk sinh `attempt_id` ngay khi đọc mã QR và dùng làm: `Idempotency-Key` của `POST /kiosk/v1/scans`, khóa chính `attempt_id` trong bảng `access_attempts` và `attendance_transactions`, khóa lệnh gửi Turnstile (`command_key = attempt_id`, header `Idempotency-Key = cmd-ts-<attempt_id>`), và `eventId` của `EventEnvelope` trong outbox.
   - Mọi thao tác ghi tại `attendance` (`recordAttempt`, `recordDenied`, `markGateCommanded`, `recordGateResult`, `saveIfAbsent`) đều idempotent theo `attempt_id`: gửi lại cùng khóa chỉ trả về trạng thái đã lưu (`duplicate = true`), không mở cổng lại và không tính công lần hai.
2. **Ghi trước khi gọi mạng tại Kiosk (Write-ahead Kiosk Journal):**
   - Kiosk ghi **mọi** lượt quét vào SQLite Journal cục bộ (chỉ lưu SHA-256 của token, không lưu token thô) với `journal_seq` tăng dần **trước** khi gọi `gate-station`. Một lượt chỉ được đánh dấu `handed_over = 1` khi nhận được quyết định từ Edge (`OPENED`, `DENIED`, `PENDING`).
3. **Ghi trước khi tác động ra ngoài tại Edge (`GATE_COMMANDED` write-ahead):**
   - Trước khi `RecordScanUseCase` (tại `gate-station`) gửi lệnh mở tới Turnstile, `attendance` phải lưu xong trạng thái `GATE_COMMANDED` vào PostgreSQL. Nếu máy chủ Edge chết ngay sau khi gửi lệnh, lúc khởi động lại job phục hồi biết chắc lệnh có thể đã rời máy chủ.
4. **Hết giờ chờ là "chưa biết", tuyệt đối không gửi lại lệnh mở cổng:**
   - Khi Turnstile timeout (`GATEWAY_TIMEOUT` / read timeout 0,8 giây), `HttpTurnstileAdapter` chỉ thử lại **1 lần** nếu là lỗi kết nối TCP sớm (không thử lại nếu đã gửi request và đang chờ đọc), sau đó chuẩn hóa thành `GateResult.UNKNOWN`.
   - `GateResultApplier` chuyển `AccessAttempt` sang `GATE_UNKNOWN` (trả `HTTP 202 PENDING` cho kiosk) và **không bao giờ phát lại lệnh mở cổng**. Job `ReconcileUnknownUseCase` chạy nền hỏi lại trạng thái thực tế của lệnh cuối cùng từ Turnstile qua `gate-station` (`GET /gates/{id}/status`) để chốt `COMMITTED` (nếu cổng đã mở) hoặc `GATE_FAILED` (nếu cổng chưa mở).
5. **Transactional Outbox và Publisher Confirm (ADR-04, ADR-15):**
   - Mọi thay đổi trạng thái và sự kiện (`AccessAttemptRecorded`, `GateResultRecorded`, `AttendanceConfirmed`, `AuditRecorded`) được ghi vào `outbox_events` trong **cùng một local transaction PostgreSQL** thông qua `SpringTransactionRunner`.
   - `PublishOutboxUseCase` chỉ đánh dấu `PUBLISHED` sau khi RabbitMQ xác nhận (`ConfirmType.CORRELATED`); gặp lỗi broker sẽ dừng ngay ở sự kiện lỗi để bảo toàn thứ tự ghi (`seq`) và thử lại ở chu kỳ sau. Phía nhận khử trùng theo `message_id` trong bảng `processed_messages`.

---

## 4. Cách giảm thiểu từng kịch bản (P1 – P7)

| Điểm đứt | Kịch bản | Quy tắc giảm thiểu trong mã | Bằng chứng kiểm thử |
| --- | --- | --- | --- |
| **P1** | Kiosk gửi scan nhưng mất phản hồi từ Edge | Kiosk giữ bản ghi trong SQLite Journal và gửi lại tối đa số lần thử với **cùng `attempt_id`** (nếu gửi lại từ journal khi chưa quyết định thì gửi hash để tra trạng thái). Edge trả lại kết quả đã lưu (`replay`) hoặc `IN_PROGRESS` (kiosk hỏi lại tối đa 6 lần × 300 ms, không mở cổng cục bộ). | `Adr11ResilienceTest.should_create_one_attempt_and_one_event_when_the_same_attempt_id_is_sent_twice`, `RecordScanUseCaseTest.should_answer_a_retry_from_the_stored_result_and_open_the_gate_only_once`, `RecordScanUseCaseTest.should_report_in_progress_when_a_retry_arrives_while_the_first_request_is_unfinished`, `KioskAppTest.should_retry_with_the_same_attempt_id_when_the_edge_does_not_answer` |
| **P2** | Giữ chỗ `RESERVED` xong thì crash trước khi ra lệnh cổng | `RecoverStaleAttemptsUseCase` quét các lượt ở trạng thái `RESERVED` quá TTL 30 giây (`RecoveryPolicy`), chuyển sang `DENIED` (`SYSTEM_RETRY`) và ghi outbox `AuditRecorded` (`RESERVATION_RELEASED`); khóa giữ chỗ Redis cũng có TTL (10 giây) nên nhân viên quét lại được. | `Adr11ResilienceTest.should_release_a_stale_reservation_as_denied_system_retry`, `RecordScanUseCaseTest.should_release_everything_and_fail_when_attendance_is_down_before_the_command` |
| **P3** | Đã gửi lệnh mở cổng, Turnstile không trả lời | Ghi `GATE_UNKNOWN`, trả `PENDING` (`TURNSTILE_TIMEOUT_RETRY_SCAN`), giữ khóa `jti` chặn dùng lại. `ReconcileUnknownUseCase` hỏi `IGateStatusQuery` để chốt `COMMITTED` + công hoặc `GATE_FAILED` (`TURNSTILE_TIMEOUT_RETRY_SCAN`). | `Adr11ResilienceTest.should_keep_unknown_then_commit_when_reconcile_confirms_the_gate_opened`, `Adr11ResilienceTest.should_fail_the_attempt_when_reconcile_confirms_the_gate_never_opened`, `RecordScanUseCaseTest.should_record_unknown_on_timeout_and_never_resend_the_open_command_on_retry`, `make demo-timeout` |
| **P4** | Crash ngay sau `GATE_COMMANDED` | Vì `GATE_COMMANDED` đã ghi bền vững trước khi gọi Turnstile, `RecoverStaleAttemptsUseCase` chuyển mọi lượt `GATE_COMMANDED` quá thời gian ân hạn (10 giây) sang `GATE_UNKNOWN` để `ReconcileUnknownUseCase` đối soát như P3. | `Adr11ResilienceTest.should_recover_a_crashed_attempt_through_unknown_into_committed`, `Adr11ResilienceTest.should_not_touch_a_commanded_attempt_inside_the_grace_period` |
| **P5** | Cổng đã mở nhưng Edge không lưu được kết quả (hoặc kiosk không nhận được) | `RecordScanUseCase.onOpened` trả `UNCONFIRMED`; hàng `GATE_COMMANDED` được phục hồi qua `GATE_UNKNOWN` → đối soát chốt công (không mất công). Nếu đã commit mà rớt phản hồi về kiosk, gửi lại cùng `attempt_id` nhận lại `ALLOWED`. | `RecordScanUseCaseTest.should_answer_unconfirmed_when_the_gate_opened_but_the_result_could_not_be_saved`, `KioskAppTest.should_retry_an_unfinished_scan_with_the_hash_never_the_raw_token` |
| **P6** | Kiosk mất mạng tới máy chủ biên | `KioskApp` tự xác thực token bằng khóa thiết bị đã đồng bộ, kiểm tra replay cache cục bộ (2 phút), ghi journal (`OFFLINE_OPENED`), mở cổng qua Local Gate Link; khi có mạng lại, `flush` gọi `POST /kiosk/v1/scans/offline-batch` → `IngestOfflineBatchUseCase` nhập theo `journal_seq`. | `KioskAppTest.should_verify_locally_open_through_the_local_link_and_journal_when_the_edge_stays_down`, `KioskAppTest.should_hand_over_offline_rows_in_journal_order_when_the_edge_returns`, `Adr11ResilienceTest.should_ingest_each_attempt_once_and_flag_a_token_reused_across_kiosks`, `Adr11ResilienceTest.should_flag_anti_passback_when_an_offline_record_repeats_the_last_direction`, `make demo-offline` |
| **P7** | Đứt WAN hoặc RabbitMQ dừng giữa lúc phát outbox | `PublishOutboxUseCase` giữ hàng `PENDING` nếu chưa nhận publisher confirm và dừng tại lỗi đầu tiên để giữ thứ tự; khi RabbitMQ chạy lại, toàn bộ sự kiện chưa phát được gửi lên exchange `tmc.events` theo đúng thứ tự, hàng đã `PUBLISHED` không bị phát lại; consumer khử trùng bằng `JdbcProcessedMessageStore`. | `PersistenceAndBrokerIT.should_roll_back_attempt_and_outbox_when_use_case_throws_after_writes`, `OutboxBrokerRestartIT.should_keep_outbox_pending_while_broker_is_stopped_and_deliver_in_order_once_restarted`, `MessagingInfraIT.should_round_trip_outbox_to_rabbit_to_an_idempotent_consumer`, `Adr11ResilienceTest.should_keep_events_pending_while_the_broker_is_down_and_publish_them_once_it_returns` |

---

## 5. Đưa vào kiến trúc (Incorporation into Architecture)

| View | Thay đổi so với M1 | Vị trí trong mã hiện tại |
| --- | --- | --- |
| **Module View** | Tách rõ trách nhiệm điều phối cổng (`gate-station`) và sổ cái trạng thái/outbox (`attendance`, ADR-13). Tại `attendance`, thêm aggregate `AccessAttempt` (`AttemptStatus`), `GateResultApplier`, `IngestOfflineBatchUseCase`, `RecoverStaleAttemptsUseCase`, `ReconcileUnknownUseCase`, `PublishOutboxUseCase`, các port `IAttemptRepository`, `IOutboxRepository`, `ITransactionRunner`, `IGateStatusQuery`. | `services/attendance/src/main/java/vn/tmc/attendance/...` (`domain/`, `application/`, `adapter/`, `bootstrap/`), `platform/messaging/...` |
| **Component & Connector View** | Kiosk thêm SQLite Journal (`RECEIVED`, `OFFLINE_OPENED`, `OFFLINE_DENIED`) thay cho "Offline Queue" thụ động. `attendance` thêm `MaintenanceScheduler` (outbox 1 giây, phục hồi 5 giây, đối soát 10 giây) và gọi `gate-station` qua cổng `IGateStatusQuery` (`HttpGateStatusQuery`) để hỏi trạng thái lệnh cuối từ Turnstile. Mọi service phát sự kiện qua một topic exchange `tmc.events` với khóa `<nguồn>.<EventType>` (ADR-15). | `docs/architecture/c4-edge.md`, `MaintenanceScheduler.java`, `HttpGateStatusQuery.java`, `TmcExchange.java` |
| **Deployment View** | Tại mỗi nhà máy (Edge): `gate-station` (:8081), `attendance` (:8082) cùng PostgreSQL Edge (`access_attempts`, `attendance_transactions`, `attendance_corrections`, `outbox_events`) và Redis (`jti` reservation, anti-passback). Tại Central: RabbitMQ (`tmc.events`, hàng đợi + DLQ cho `reporting`, `hr-integration`, `audit-admin` kèm bảng `processed_messages`). | `docker/docker-compose.yml`, `services/attendance/src/main/resources/db/migration/V1__attendance_schema.sql`, `V2__attendance_corrections.sql` |
| **API View** | Phía Edge Gateway (`KioskController`): `POST /kiosk/v1/scans` bổ sung header `Idempotency-Key` = `attempt_id`, trường `journal_seq`, phản hồi `202 PENDING`/`IN_PROGRESS` và `200 UNCONFIRMED`; `POST /kiosk/v1/scans/offline-batch` nhận lô offline. Phía `attendance` (`InternalAttendanceController`): `POST /internal/v1/attempts`, `POST /internal/v1/attempts/{id}/gate-commanded` (điểm ghi trước), `POST /internal/v1/attempts/{id}/gate-result`, `POST /internal/v1/offline-batch` (`ACCEPTED`, `DUPLICATE`, `ANOMALY`). | `docs/api/internal-attendance.openapi.yaml`, `InternalAttendanceController.java`, `KioskController.java` |

---

## 6. Đánh đổi và rủi ro còn lại

1. **Đánh đổi thiết kế:**
   - **Độ phức tạp trạng thái và tác vụ nền:** Thêm bảng `access_attempts`, bảng `outbox_events`, ba tác vụ nền định kỳ (`MaintenanceScheduler`) và cơ chế giám sát chỉ số vận hành B-08 (`tmc.attendance.outbox.stale`, `tmc.attendance.attempts.gate_unknown` tại `/actuator/metrics`).
   - **Nhất quán sau cùng cho lượt `GATE_UNKNOWN`:** Trạng thái `GATE_UNKNOWN` có thể tồn tại vài giây đến khi job đối soát chạy (hoặc chờ Turnstile phản hồi); nếu Turnstile không trả lời được (`UNDETERMINED`), lượt quét cần bảo vệ xác minh thủ công (UC-06).
   - **Không dùng 2PC / Saga bù trừ vật lý:** Không thể đóng cổng lại sau khi nhân viên đã đi qua, nên chọn write-ahead (`GATE_COMMANDED`) + đối soát thay cho 2PC để giữ thời gian phản hồi online dưới 2 giây.
2. **Các khoảng trống và giới hạn đã biết (nêu trung thực theo mã hiện tại):**
   - **Anti-passback sau khi nhập offline (`docs/architecture/implementation-notes.md` mục 3):** `IngestOfflineBatchUseCase` kiểm tra anti-passback trong `attendance` và gắn cờ `ANTI_PASSBACK_OFFLINE` cho HR tra cứu qua `GET /hr/v1/anomalies` (B-03), nhưng **chưa tự động đồng bộ ngược** trạng thái `last` về `IAntiPassbackStore` của `gate-station` (phần phối hợp A-02).
   - **Giới hạn thu hồi trên kiosk đang mất mạng:** Khi kiosk offline, việc xác thực chỉ dựa trên khóa và trạng thái đã đồng bộ lần cuối; nhân viên mới bị thu hồi hoặc nghỉ việc sau lần đồng bộ cuối vẫn có thể qua cổng offline và chỉ bị phát hiện/gắn bất thường lúc `flush` (đã thừa nhận ở M1 mục 10.4).
   - **Trạng thái chiếu ở Central (`reporting`, `hr-integration`, `audit-admin`):** Dấu khử trùng `processed_messages` và DLQ đã chạy với PostgreSQL/RabbitMQ (`MessagingInfraIT`), nhưng read model/nhật ký ở các consumer phía Central còn bản in-memory (phụ thuộc các story D-04, D-06, D-08).
   - **Hiệu năng (NFR-06) và demo trên Docker đầy đủ:** Theo `docs/runbook/demo-scenarios.md` và `docs/handover/02-chay-local.md`, hai kịch bản `demo-offline` và `demo-timeout` đã được kiểm chứng trên stack `local` qua HTTP thật; việc đo tải đỉnh 2 giây (A-09) và chạy 3 kịch bản demo trên stack Docker đầy đủ ở profile mặc định (I-6) chưa có bằng chứng kiểm chứng trong báo cáo này.

---

## 7. Bằng chứng thực thi

| Loại bằng chứng | Tên lớp / Kịch bản | Nội dung đã xác nhận |
| --- | --- | --- |
| **Unit test máy trạng thái & phục hồi (Attendance)** | `AccessAttemptTest`, `Adr11ResilienceTest`, `ManualOpenTest`, `InternalApiFlowTest`, `AnomalyQueueTest`, `OperationalMetricsTest`, `ActuatorMetricsApiTest`, `ArchitectureTest` | Đầy đủ chuyển trạng thái hợp lệ/bất hợp lệ (`409`), 6 ca bắt buộc ADR-11 (P1–P7), mở thủ công không tự tính công, hàng đợi bất thường HR (`TOKEN_REUSED_OFFLINE`, `ANTI_PASSBACK_OFFLINE`, `403`), 2 chỉ số Micrometer qua `/actuator/metrics`, tuân thủ luật kiến trúc lục giác (`./gradlew :services:attendance:test` → `BUILD SUCCESSFUL`). |
| **Integration test PostgreSQL thật (B-01)** | `PersistenceAndBrokerIT` (`11/11` tests pass trên PostgreSQL 16 Testcontainers) | Flyway `V1` & `V2`, upsert `access_attempts`, `saveIfAbsent` chống đếm công hai lần, thứ tự `outbox_events`, đếm stale outbox & `GATE_UNKNOWN`, truy vấn `anomalies` lọc theo nhà máy & thời gian, và `should_roll_back_attempt_and_outbox_when_use_case_throws_after_writes` chứng minh rollback nguyên tử cả `access_attempts` lẫn `outbox_events` khi use case ném lỗi (`./gradlew :services:attendance:integrationTest` → `BUILD SUCCESSFUL`). |
| **Integration test RabbitMQ stop/restart thật (B-02)** | `OutboxBrokerRestartIT` (`1/1` test pass) & `MessagingInfraIT` | `should_keep_outbox_pending_while_broker_is_stopped_and_deliver_in_order_once_restarted`: dừng container RabbitMQ thật → `PublishOutboxUseCase.run()` giữ nguyên `PENDING`; khởi động lại RabbitMQ → phát đủ 3 sự kiện lên exchange `tmc.events` (`attendance.<EventType>`) đúng thứ tự ghi, và hàng đã `PUBLISHED` trước đó không bị phát lại. |
| **Unit test phía Gate Station & Kiosk** | `RecordScanUseCaseTest`, `KioskAppTest` | Ghi journal trước khi gọi mạng (không lưu token thô), retry cùng `attempt_id`, xử lý `IN_PROGRESS`/`UNCONFIRMED`, mở cổng offline qua Local Gate Link và `flush` theo `journal_seq`. |
| **Kịch bản demo (tài liệu runbook stack `local`)** | `scripts/demo/offline.sh` (`make demo-offline`), `scripts/demo/timeout.sh` (`make demo-timeout`) | Theo `docs/runbook/demo-scenarios.md` (stack `local` qua HTTP thật): `demo-offline` kiểm chứng P6 (`ALLOWED mode=OFFLINE`, `handedOver=2`, lần `flush` thứ hai `handedOver=0`); `demo-timeout` kiểm chứng P3 (chèn trễ `DELAY_OPENED 1500 ms`, kiosk nhận `HTTP 202 PENDING`, `pass_count` chỉ tăng `1`, đối soát sau đó chốt `COMMITTED` mà không gửi lệnh mở lần hai). |

