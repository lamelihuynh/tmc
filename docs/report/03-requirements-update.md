# Cập nhật Yêu cầu, Use Case và Đặc tính Kiến trúc (Task 1.1)

> Phần này trả lời yêu cầu **Task 1.1** của đề bài M2: cập nhật các yêu cầu chức năng, phi chức năng, use case và fitness function bị ảnh hưởng bởi các quyết định kiến trúc mới (ADR-11, ADR-12, ADR-13). Tệp gồm phần **Team C** (`FR-19`, `NFR-05`, `UC-07`, `AC-04`, kèm liên đới `NFR-03`) và phần **Team B** (`NFR-09`, `UC-08`); `AC-03` do **Team D** bổ sung khi hợp nhất (D-12), dùng cả bằng chứng của Team B.

---

## 1. Phần Team C: FR-19, NFR-05 (NFR-03), UC-07, AC-04

Căn cứ đối chiếu: `docs/document.md` (M1 mục 4.2.3, 4.3, 5.7, 6.1) và `docs/adr/README.md` mục 3.

### 1.1. Cập nhật Yêu cầu chức năng & phi chức năng

| ID | Mục M1 | Bản M1 hiện tại | Bản cập nhật cho M2 (ADR-12, ADR-08, ADR-09) |
| --- | --- | --- | --- |
| **FR-19** | 4.2.3 | Hệ thống tự động sinh mã QR định danh cho nhân viên trên mobile app/web portal. Mã QR là chuỗi token ngẫu nhiên (30–60 giây) và chỉ có giá trị sử dụng một lần. | **Loại bỏ hoàn toàn QR tĩnh.** Ứng dụng nhân viên (`employee-app` / `device-simulator`) đăng ký cặp khóa bất đối xứng ECDSA P-256 cho thiết bị (mỗi nhân viên tối đa **1 thiết bị `ACTIVE`**, khóa riêng không rời thiết bị) và **tự sinh, tự ký mã QR động** (compact JWS `ES256`, `kid = device_id`, hiệu lực mặc định 30 giây, tối đa 60 giây, chỉ dùng một lần) ngay cả khi điện thoại mất mạng. Email không còn được dùng để gửi QR mở cổng. |
| **NFR-05** | 4.3 | Mã QR không chứa thông tin cá nhân đọc được, là token ngẫu nhiên không đoán được; 100% QR hết hạn, đã dùng hoặc bị thu hồi bị từ chối trong ngân sách 2 giây. | Bổ sung 3 tiêu chí bắt buộc từ ADR-12: **(1) Ràng buộc thiết bị:** token phải do thiết bị `ACTIVE` đã đăng ký ký và `sub` phải khớp chủ sở hữu thiết bị (`VerifyQrTokenUseCase`); **(2) Cấm claim PII & giới hạn tuổi thọ:** từ chối ngay (`MALFORMED_TOKEN`) mọi token có claim ngoài `{sub, jti, iat, exp, fac, nonce}` hoặc `exp - iat > 60s` (`TokenVerificationPolicy`, `NimbusTokenCodec`); **(3) Chống replay kể cả khi kiosk offline:** 100% token dùng lại bị từ chối khi online (Redis `SET NX`, R1) và tại cùng kiosk offline (replay cache cục bộ, R2), và bị gắn cờ `TOKEN_REUSED_OFFLINE` khi đồng bộ từ nhiều kiosk (R3). |
| **NFR-03** *(bổ sung trạng thái M2)* | 4.3 | Bắt buộc MFA cho System Administrator và HR Staff; khóa tạm 15 phút sau 5 lần sai; hết hạn phiên sau 15 phút không hoạt động. | Bổ sung cơ chế thật trong `identity`: cấp JWT `ES256` sống 15 phút kèm JWKS hỗ trợ xoay khóa (`SigningKeys`, C-12); xác thực MFA bằng **TOTP RFC 6238** có chống dùng lại mã cho `HR_STAFF` và `SYSTEM_ADMIN` (`TotpMfaVerifier`, C-05); audit mọi lần đăng nhập/khóa qua outbox (C-08, C-11). **Ghi rõ hạn chế:** hết hạn phiên theo không hoạt động và thu hồi phiên khi đổi vai trò chưa hiện thực (`AuthenticationTest` còn 2 test `@Disabled`). |

### 1.2. Cập nhật Use Case UC-07: Đăng ký thiết bị và sinh QR động ký bằng khóa thiết bị

Thay thế bảng mục **5.7 UC-07** trong `docs/document.md`:

| Trường | Nội dung cập nhật M2 |
| --- | --- |
| **Use-case ID** | UC-07 |
| **Use-case name** | Đăng ký thiết bị và sinh QR động ký bằng khóa thiết bị |
| **Actor** | Employee (phụ: HR Staff khi hỗ trợ thu hồi / đổi trạng thái theo UC-02) |
| **Description** | Nhân viên đăng ký khóa công khai của điện thoại vào Device Key Registry (mỗi người tối đa 1 thiết bị `ACTIVE`). Sau khi đăng ký, ứng dụng tự sinh và tự ký mã QR động ngắn hạn (30 giây) bằng khóa riêng lưu trên máy để chống chụp màn hình, chuyển tiếp và vẫn dùng được khi điện thoại không có mạng. |
| **Trigger** | Nhân viên mở `employee-app` để lấy QR hoặc đăng ký thiết bị mới. |
| **Precondition** | Nhân viên có tài khoản hợp lệ trong `identity` (JWT Bearer hoặc dev profile), trạng thái nhân sự `ACTIVE`. |
| **Postcondition** | Thiết bị mới được lưu `ACTIVE` trong `user-qr` (thiết bị cũ nếu có tự động chuyển `REVOKED`), kèm sự kiện `AuditRecorded`; ứng dụng hiển thị mã QR tự xoay vòng mỗi 20 giây (hiệu lực mỗi token 30 giây). |
| **Normal flow** | 1. Nhân viên đăng nhập qua `identity` (`POST /auth/v1/login`) và nhận access token JWT `ES256`.<br/>2. Nếu chưa có khóa trên máy: app sinh cặp khóa EC P-256 trong vùng lưu không thể xuất khóa riêng (`extractable = false` / Keystore), gửi khóa công khai tới `POST /self-service/v1/devices`.<br/>3. `user-qr` kiểm tra JWT & vai trò `EMPLOYEE`, kiểm tra `caller.subject == employeeUuid`, thu hồi thiết bị `ACTIVE` cũ (nếu có) và lưu thiết bị mới + audit trong một transaction (`DeviceManagementService`), trả `201 { deviceId }`.<br/>4. Khi hiển thị QR (kể cả khi điện thoại mất mạng), app tự đóng gói JWS `ES256` (`kid = deviceId`, claims `{sub, jti, iat, exp = iat + 30s, fac, nonce}`) và ký bằng khóa riêng trên máy.<br/>5. App tự tạo token mới mỗi 20 giây (nhỏ hơn thời hạn 30 giây); khi quét tại kiosk, `gate-station` và `user-qr` xác thực chữ ký, thiết bị, thời hạn và giữ chỗ `jti` dùng một lần. |
| **Alternative flows** | - **Đã xóa luồng 1.1 cũ (gửi QR tĩnh qua email có hạn theo ngày).**<br/>- **3.1 Đăng ký thay thế thiết bị (đổi điện thoại):** bước 3 tự động thu hồi thiết bị `ACTIVE` trước đó (`DEVICE_REVOKED`).<br/>- **3.2 Thu hồi chủ động:** nhân viên gọi `DELETE /self-service/v1/devices/{deviceId}`, hoặc HR gọi `POST /hr/v1/employees/{employeeUuid}/devices/revoke`, hoặc HR chuyển nhân viên sang `INACTIVE` qua `PUT /hr/v1/employees/{employeeUuid}/eligibility` (tự động thu hồi thiết bị).<br/>- **1.2 Không có điện thoại dùng được tại cổng:** đi theo **UC-06** (bảo vệ xác minh và mở cổng thủ công có audit). |
| **Exception flows** | - **3a. Hai yêu cầu đăng ký thiết bị chạy song song cho cùng nhân viên:** một yêu cầu thắng `201`, yêu cầu còn lại bị partial unique index chặn, rollback và trả `409 Conflict` (`CONCURRENT_DEVICE_REGISTRATION`) kèm audit `CONFLICT`.<br/>- **3b. Khóa công khai không hợp lệ:** trả `400 Bad Request`.<br/>- **4.1 (Sửa từ 5.1 cũ) Điện thoại mất mạng khi mở app lấy QR:** app **vẫn tự sinh và ký được QR bình thường** bằng khóa riêng đã lưu cục bộ (chỉ bước đăng ký thiết bị lần đầu mới cần kết nối `user-qr`).<br/>- **5.1 Token bị từ chối khi quét tại cổng:** trả mã lý do tường minh không lộ PII (`QR_EXPIRED`, `QR_ALREADY_USED`, `DEVICE_REVOKED`, `DEVICE_UNKNOWN`, `INVALID_SIGNATURE`, `CLOCK_SKEW_EXCEEDED`, `MALFORMED_TOKEN`). |

### 1.3. Cập nhật Đặc tính Kiến trúc AC-04 (mục 6.1 M1)

| Mã | Bổ sung Fitness Function trong M2 | Bằng chứng tự động trong mã |
| --- | --- | --- |
| **AC-04 (Security — AuthN/AuthZ & Chống giả mạo QR)** | - 100% API tài nguyên kiểm tra chữ ký JWT `ES256` qua JWKS (profile mặc định tắt header dev) và áp dụng `Rbac.requireRole` / `DataScope` ở use case.<br/>- 100% tài khoản `HR_STAFF`, `SYSTEM_ADMIN` bắt buộc qua TOTP RFC 6238 chống dùng lại mã.<br/>- 100% token quá hạn vượt skew (`> exp + 60s`), `exp - iat > 60s`, chứa PII, sai chữ ký, khác chủ thiết bị, hoặc thiết bị đã thu hồi bị từ chối.<br/>- Tuân thủ chính sách hợp đồng chung `qr-token-policy.json` trên cả producer và verifier. | `BearerTokenApiTest`, `JwtPrincipalVerifierTest`, `TotpMfaVerifierTest`, `TotpLoginApiTest`, `SigningKeyRotationTest`, `Adr12TokenVerificationTest`, `QrTokenPolicyContractTest`, `EmployeeEligibilityApiTest`, `ConcurrentDeviceRegistrationIT`. |

---

## 2. Cập nhật Yêu cầu phi chức năng — NFR-09 (Team B)

So với M1 ([`docs/document.md`](../document.md), mục 4.3), **NFR-09** được bổ sung tiêu chí đo lường cụ thể cho mất kết nối giữa chừng (ADR-11):

| ID | Phân loại | Mô tả chi tiết (M2) | Tiêu chí đo lường / Ràng buộc (M2) |
| --- | --- | --- | --- |
| **NFR-09** | Tính sẵn sàng & Dự phòng mất mạng | Hệ thống phải duy trì hoạt động liên tục và bảo toàn toàn vẹn giao dịch khi mất kết nối mạng nội bộ/WAN hoặc tiến trình gặp sự cố **ở bất kỳ bước nào** của lượt quét (từ lúc quét tại kiosk tới khi phát sự kiện về trung tâm). | **0 bản ghi chấm công bị mất hoặc tính trùng** và **0 lần mở cổng lặp** khi đứt kết nối tại bất kỳ điểm nào (P1–P7). Kiosk phải ghi **Kiosk Journal** (append-only SQLite, lưu hash token) **trước** khi gọi mạng; mọi lệnh mở cổng ghi trước trạng thái `GATE_COMMANDED` bền vững; khi timeout chuyển `GATE_UNKNOWN` để đối soát thay vì gửi lại lệnh mở; Outbox chỉ chuyển `PUBLISHED` sau khi broker xác nhận. |

---

## 3. Cập nhật Ca sử dụng — UC-08: Quét QR vào/ra (Team B)

So với M1 ([`docs/document.md`](../document.md), mục 5.8), **UC-08** được điều chỉnh theo ADR-11, ADR-12 và ADR-13 như sau:

### 3.1. Điều chỉnh Luồng chính và Luồng thay thế
- **Xóa luồng thay thế `1.2` ("Nhân viên dùng QR nhận qua email"):** Loại bỏ hoàn toàn QR tĩnh qua email theo ADR-12; mọi QR sử dụng tại cổng phải là token động ES256 ký bằng khóa riêng gắn thiết bị đã đăng ký (`kid`), thời hạn tối đa 60 giây.
- **Chuẩn hóa bước 2–6 của Luồng chính theo ADR-13 & ADR-11:**
  - Bước 2: Kiosk sinh `attempt_id` (UUID v7), ghi vào **Kiosk Journal** (`RECEIVED`, `journal_seq`) **trước** khi gọi `POST /kiosk/v1/scans` kèm header `Idempotency-Key = attempt_id`.
  - Bước 3–4: `gate-station` kiểm tra idempotency với `attendance`, gọi `user-qr` xác thực chữ ký/thiết bị/thời hạn, áp chính sách và giữ chỗ nguyên tử `jti` + anti-passback.
  - Bước 5: `attendance` ghi `RESERVED`, sau đó ghi **`GATE_COMMANDED` (write-ahead)** trước khi `gate-station` gọi Turnstile (`cmd-ts-<attempt_id>`).
  - Bước 6: Khi Turnstile trả `OPENED`, `attendance` cập nhật `GATE_OPENED → COMMITTED`, tạo `AttendanceTransaction` (`saveIfAbsent`) và ghi sự kiện `AttendanceConfirmed` vào `outbox_events` **trong cùng một transaction PostgreSQL**.

### 3.2. Viết lại các Luồng ngoại lệ `2.x`, `5.1` và `6.1` theo máy trạng thái ADR-11

| Mã ngoại lệ | Tình huống | Xử lý chuẩn hóa theo máy trạng thái (M2) |
| --- | --- | --- |
| **2.1 (Kiosk mất mạng tới Edge)** | Không kết nối được `gate-station` trước khi gửi hoặc sau số lần thử lại | Kiosk tự xác thực token động bằng khóa thiết bị và danh bạ/danh sách thu hồi từ lần đồng bộ gần nhất, kiểm tra replay cache cục bộ (2 phút), ghi journal (`OFFLINE_OPENED` hoặc `OFFLINE_DENIED`) trước khi mở cổng qua Local Gate Link. |
| **2.2 (Khôi phục mạng sau offline)** | Kiosk đẩy các dòng chưa bàn giao (`handed_over = 0`) | Kiosk `flush` theo thứ tự `journal_seq` tới `POST /kiosk/v1/scans/offline-batch` (Edge Gateway chuyển sang `POST /internal/v1/offline-batch` của `attendance` — `IngestOfflineBatchUseCase`). `attendance` bỏ qua `attempt_id` đã có (`DUPLICATE`); gắn cờ bất thường `TOKEN_REUSED_OFFLINE` hoặc `ANTI_PASSBACK_OFFLINE` (không tính công, đẩy vào hàng đợi HR `GET /hr/v1/anomalies` cho UC-09); hợp lệ thì commit `OFFLINE` + outbox. Tuyệt đối không phát lại lệnh mở cổng. |
| **2.3 (Kiosk chưa có sync hoặc journal lỗi)** | Kiosk offline nhưng chưa có khóa thiết bị hoặc không ghi được journal | Không mở cổng, hiển thị lỗi để Security Guard xử lý mở thủ công (UC-06). |
| **2.4 (Mất phản hồi giữa chừng – P1/P5)** | Kiosk gửi online nhưng rớt phản hồi hoặc Edge trả `IN_PROGRESS` | Kiosk gửi lại với **cùng `attempt_id`**: nếu Edge đã chốt kết quả cuối thì trả lại (`duplicate = true`); nếu đang xử lý (`IN_PROGRESS`) thì kiosk hỏi lại tối đa 6 lần × 300 ms, quá giới hạn ghi `PENDING` (không tự mở cổng cục bộ). |
| **5.1 (Turnstile timeout hoặc lỗi – P3/P4)** | Turnstile quá hạn chờ (0,8 giây) hoặc trả `504`/`ERROR` | Nếu Turnstile báo lỗi phần cứng (`ERROR`), chuyển `GATE_FAILED` (`GATE_HARDWARE_ERROR`) và nhả `jti` để nhân viên quét lại. Nếu `TIMEOUT`, chuyển **`GATE_UNKNOWN`**, trả kiosk `HTTP 202 PENDING` (`TURNSTILE_TIMEOUT_RETRY_SCAN`), **không gửi lại lệnh mở** và giữ khóa `jti`. Job nền `RecoverStaleAttemptsUseCase` chuyển `GATE_COMMANDED` bị treo quá 10 giây sang `GATE_UNKNOWN`, và `ReconcileUnknownUseCase` hỏi trạng thái cổng để chốt `COMMITTED` hoặc `GATE_FAILED`. |
| **6.1 (Lỗi lưu kết quả sau khi cổng đã mở – P5 / Crash)** | Cổng trả `OPENED` nhưng không lưu được vào `attendance` | `gate-station` trả `UNCONFIRMED`; bản ghi `GATE_COMMANDED` trong PostgreSQL được phục hồi sang `GATE_UNKNOWN` rồi đối soát chốt `COMMITTED` + công, bảo đảm không mất công của nhân viên. |

---

## 4. Cập nhật Đặc tính Kiến trúc AC-03 (Team D, kèm bằng chứng Team B)

Mục này trả lời phần AC-03 của Task 1.1 (`docs/adr/README.md` mục 3, dòng "6.1 AC-03, AC-04"). M1 định nghĩa **AC-03 Reliability & Data Integrity**: RPO = 0 cho mọi sự kiện chấm công đã xác nhận mở cổng, không mất và không trùng bản ghi (M1 mục 14.1). M2 biến tiêu chí đó thành các fitness function chạy tự động, ở cả biên (ADR-11, ADR-15) lẫn các consumer trung tâm (D-04, D-06, D-08, ADR-16). Tiêu chí về sẵn sàng khi mất mạng (AC-02) dùng chung bằng chứng ở hàng 1.

| Fitness function | Phạm vi | Bằng chứng tự động |
| --- | --- | --- |
| **1. Không mất, không trùng tại biên:** gửi lại cùng `attempt_id` tạo đúng một lượt và một sự kiện; timeout cổng không sinh lệnh mở thứ hai; lượt treo được phục hồi rồi đối soát; kiosk offline ghi journal trước và giao lại theo thứ tự, không giao hai lần | `attendance`, `gate-station`, `kiosk-simulator` (Team B, A, D) | `Adr11ResilienceTest`, `RecordScanUseCaseTest`, `KioskAppTest.should_hand_over_offline_rows_in_journal_order_when_the_edge_returns`, `KioskAppTest.should_not_double_hand_over_when_flush_runs_twice`; `make demo-timeout`, `make demo-offline` |
| **2. Ghi trạng thái và sự kiện nguyên tử, phát có xác nhận:** use case ném lỗi thì cả lượt lẫn outbox rollback; broker dừng thì outbox giữ `PENDING` và phát đúng thứ tự khi bật lại | `attendance`, `platform/messaging` (Team B) | `PersistenceAndBrokerIT.should_roll_back_attempt_and_outbox_when_use_case_throws_after_writes`, `OutboxBrokerRestartIT.should_keep_outbox_pending_while_broker_is_stopped_and_deliver_in_order_once_restarted`, `MessagingInfraIT.should_round_trip_outbox_to_rabbit_to_an_idempotent_consumer` |
| **3. Consumer lũy đẳng, ghi cùng transaction với dấu chống trùng:** thông điệp giao lại là no-op; handler lỗi thì cả dữ liệu lẫn dấu `processed_messages` đều không được ghi | `reporting`, `audit-admin`, `platform/messaging` (Team D) | `ProjectionTest.should_process_a_redelivered_message_only_once`, `AuditTest.should_keep_one_entry_when_the_same_message_is_delivered_twice`, `PresencePersistenceIT.should_roll_back_the_projection_and_the_marker_when_the_handler_fails`, `MessagingInfraIT.should_roll_the_dedup_marker_back_when_the_handler_fails_in_the_same_transaction` |
| **4. Read model nhất quán khi sự kiện đến sai thứ tự:** bản cũ hơn của cùng lượt bị bỏ qua (so `aggregateVersion`); hiệu chỉnh lượt cũ không đè lượt mới hơn; dữ liệu còn sau khi khởi động lại | `reporting` (Team D) | `ProjectionTest.should_ignore_an_older_version_of_the_same_scan_that_arrives_late`, `should_not_let_a_correction_of_an_earlier_scan_override_a_later_scan`, `should_apply_a_correction_event_on_top_of_the_original`, `PresencePersistenceIT.should_keep_presence_across_a_restart_of_reporting` |
| **5. Không gửi trùng và không bỏ sót sang HR/Payroll:** kết quả không rõ thì tra cứu trước, chỉ gửi lại khi HR xác nhận chưa nhận, cùng `Idempotency-Key`; thử lại theo lịch có backoff, không chờ thông điệp được giao lại; dòng kẹt `IN_PROGRESS` được tra cứu | `hr-integration` (Team D) | `SyncAttendanceEventTest.should_look_up_the_result_before_resending_an_unknown_event`, `should_resend_with_the_same_event_id_when_hr_confirms_not_found`, `should_keep_unknown_without_resending_when_lookup_is_inconclusive`; `HrRetryTest` (5 test); `HttpHrPayrollAdapterTest.should_map_each_http_status_to_what_it_says_about_the_batch`; `SyncLogPersistenceIT` |
| **6. Bản hiệu chỉnh tới được mọi consumer đúng một lần** | `reporting`, `hr-integration` (Team D) | `ProjectionTest.should_apply_a_correction_event_on_top_of_the_original`, `SyncAttendanceEventTest.should_send_a_correction_once_after_the_original_event_was_synced`, `HrRetryTest.should_send_a_correction_even_while_the_original_is_unknown` |
| **7. Thông điệp lỗi không biến mất:** sau 3 lần thử, thông điệp nằm trong DLQ và được đếm | `hr-integration` (Team D) | `HrAttendanceListenerIT.should_park_a_message_that_keeps_failing_in_the_dead_letter_queue` (gauge `tmc.rabbitmq.dlq.depth`) |
| **8. Toàn vẹn nhật ký kiểm toán:** login của service không sửa, xóa, TRUNCATE được; ghi song song giữ một chuỗi băm hợp lệ; sửa trái phép bị chỉ ra đúng dòng | `audit-admin` (Team D, ADR-16) | `AuditStoreIT` (4 test), `AuditTest.should_detect_a_modified_entry_through_the_hash_chain` |
| **9. Hạn đồng bộ NFR-08:** bản ghi chưa `SYNCED` của kỳ bị cảnh báo lúc 08:00 và 12:00 ngày 1 | `hr-integration` (Team D) | `SyncAttendanceEventTest.should_raise_a_deadline_alert_before_noon_on_the_first_day_of_the_month`, `SyncLogPersistenceIT.should_let_the_deadline_alert_count_rows_from_the_database` |

**Chưa chứng minh** (xem [08-limitations.md](08-limitations.md)): RPO = 0 ở mức phần cứng (replication đồng bộ sang Edge Node B là thiết kế, không có trong hạ tầng dự án); 3 kịch bản demo trên stack Docker đầy đủ; đối soát kỳ với Attendance và lô chốt (test `should_send_a_correction_batch_for_missing_or_failed_records` còn `@Disabled`).
