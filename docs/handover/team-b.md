# Team B: Attendance

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này. Lịch chung: [01-ke-hoach.md](01-ke-hoach.md).

> **Bạn có thể bắt đầu ngay (16/11)**
>
> **Đã có sẵn cho Team B** (bước gỡ phụ thuộc, nhánh `chore/x/decouple-teams`):
>
> - B-04, B-05 xong: `AttemptView` có `employeeUuid`, `direction`; `GET /internal/v1/employees/{employeeUuid}/last-direction`.
> - Nền của B-06: `AttendanceCorrection`, `IAttendanceCorrectionRepository` với `InMemoryAttendanceCorrectionRepository` và `JdbcAttendanceCorrectionRepository`, migration `V2__attendance_corrections.sql`, `AttendanceEventFactory.attendanceCorrected` và `correctedVersion`, test `AttendanceCorrectedEventTest`.
> - Phát sự kiện lên exchange `tmc.events` (ADR-15) bằng `AmqpEnvelopePublisher` của `platform/messaging`; B-10 (`JdbcProcessedMessageStore`) đã nằm trong `platform/messaging`.
> - `PersistenceAndBrokerIT` (8 test) xanh với PostgreSQL thật.
> - Consumer thật của Team D đã nghe `attendance.*`; Team B không phải chờ hay báo ai khi phát thêm sự kiện đúng hợp đồng.
>
> **Lệnh:**
>
> ```bash
> ./gradlew :services:attendance:test                    # không cần Docker
> ./gradlew :services:attendance:integrationTest         # cần Docker đang chạy (Testcontainers)
> make up                                                # hạ tầng thật để chạy service; lỗi database cũ: make nuke && make up
> ./gradlew :services:attendance:bootRun                 # profile mặc định, PostgreSQL edge và RabbitMQ
> ```
>
> **Code theo:** `docs/api/internal-attendance.openapi.yaml`, `docs/api/attendance-events.asyncapi.yaml` (kênh `attendance.AttendanceCorrected`, schema `CorrectionPayload`), `docs/api/identity.openapi.yaml` (vai trò trong JWT); trong test: `AttendanceFixture` với `MutableClock`, `TestTokens` (`platform/security` testFixtures), `TmcContainers`.
>
> **Không chờ team nào.** Phần cần mã thật của team khác là kiểm tích hợp **tùy chọn** ở mục 5.

## 1. Nhiệm vụ của team

Team B giữ **sổ cái của hệ thống**: mọi lượt quét, trạng thái của nó, bản ghi công, và việc phát sự kiện ra ngoài mà không mất và không trùng.

| Sở hữu | Nội dung |
| --- | --- |
| `services/attendance` (cổng 8082) | Máy trạng thái lượt quét, bản ghi công, hiệu chỉnh, outbox, nhập lô offline, phục hồi, đối soát |
| Hợp đồng | `docs/api/internal-attendance.openapi.yaml`, `docs/api/attendance-events.asyncapi.yaml` |
| Tài liệu | ADR-11, `docs/architecture/state-machine.md`, phần "mất kết nối" của báo cáo (phần lớn nhất) |

Hai người: **B1** (lead) giữ hiệu chỉnh và mở thủ công, viết phần báo cáo chính; **B2** giữ kiểm chứng hạ tầng, hàng đợi bất thường, chỉ số.

## 2. Bản đồ mã của attendance

Gói gốc: `services/attendance/src/main/java/vn/tmc/attendance/`.

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | **`AccessAttempt`**, `AttemptStatus` | Thực thể lượt quét và các chuyển trạng thái hợp lệ |
| | `AttendanceTransaction`, `AttendanceCorrection` | Bản ghi công; bản hiệu chỉnh liên kết bản gốc |
| | `Anomaly` (`TOKEN_REUSED_OFFLINE`, `ANTI_PASSBACK_OFFLINE`), `AttemptSource` | Cờ bất thường; nguồn `ONLINE`, `OFFLINE`, `MANUAL` |
| `application.usecase` | `AttendanceCommandService`, `GateResultApplier` | Ghi lượt quét; biến kết quả cổng thành trạng thái, bản ghi công, sự kiện |
| | `IngestOfflineBatchUseCase`, `RecoverStaleAttemptsUseCase`, `ReconcileUnknownUseCase` | Nhập lô offline; phục hồi; đối soát `GATE_UNKNOWN` |
| | `PublishOutboxUseCase`, `AttendanceEventFactory` | Đẩy outbox lên broker; dựng sự kiện không chứa dữ liệu cá nhân |
| `application.port.out` | `IAttemptRepository`, `IAttendanceTransactionRepository`, `IAttendanceCorrectionRepository`, `IOutboxRepository`, `ITransactionRunner`, `IGateStatusQuery` | Mọi thứ use case cần ở bên ngoài |
| `adapter.in.rest` | `InternalAttendanceController` | API nội bộ cho gate-station |
| `adapter.in.scheduler` | `MaintenanceScheduler` | Outbox 1 giây, phục hồi 5 giây, đối soát 10 giây |
| `adapter.out.inmem` | `InMemoryStore`, `InMemoryAttemptRepository`, ... | Test và profile `local`, `inmem` |
| `adapter.out.persistence` | `JdbcAttemptRepository`, `JdbcAttendanceTransactionRepository`, `JdbcAttendanceCorrectionRepository`, `JdbcOutboxRepository`, `SpringTransactionRunner` | PostgreSQL, có `PersistenceAndBrokerIT` |
| `bootstrap` | `UseCaseConfig`, `InMemoryProfileConfig`, `InfrastructureProfileConfig` | Chọn adapter theo profile; publisher RabbitMQ |

| Lớp test | Kiểm gì |
| --- | --- |
| `AccessAttemptTest` | Chuyển trạng thái hợp lệ và không hợp lệ |
| `Adr11ResilienceTest` | Sáu test bắt buộc của ADR-11 |
| `ManualOpenTest` | Mở thủ công không tự tính công |
| `AttendanceCorrectedEventTest` | Sự kiện hiệu chỉnh đúng hợp đồng |
| `InternalApiFlowTest` | API nội bộ qua MockMvc với profile `inmem` |
| `PersistenceAndBrokerIT` | Repository JDBC với PostgreSQL thật (thẻ `integration`) |
| `ArchitectureTest` | Chiều phụ thuộc giữa các gói |

## 3. Việc phải làm (16/11 đến 24/11)

**Mọi hạn là 23:00 của ngày cuối**, tính theo lúc PR hợp nhất vào `main`. Mức: **B** bắt buộc, **N** nên có.

| Story | Mức | Người | Bắt đầu | Hạn |
| --- | --- | --- | --- | --- |
| B-06 Hiệu chỉnh chấm công: use case và endpoint | B | B1 | 16/11 | 18/11 |
| B-07 Xác minh lượt mở thủ công | N | B1 | 19/11 | 20/11 sáng |
| R-B Phần báo cáo "mất kết nối" | B | B1 (B2 bổ sung bằng chứng 23/11) | 20/11 chiều (nháp) | 24/11 (bản cuối) |
| B-01 (phần còn lại) Test rollback | B | B2 | 16/11 sáng | 16/11 |
| B-02 (phần còn lại) Dừng và bật lại RabbitMQ | B | B2 | 16/11 chiều | 17/11 |
| B-03 Hàng đợi bất thường cho HR | B | B2 | 18/11 | 19/11 |
| B-08 Chỉ số vận hành | N | B2 | 20/11 | 20/11 |

Ngày 23/11: B1 hoàn thiện báo cáo; B2 chạy 3 demo với attendance ở profile mặc định trên PostgreSQL (thuộc kiểm tích hợp I-6) và bổ sung bằng chứng vào báo cáo. Ngày 24/11: đóng băng mã lúc 23:00, chỉ sửa lỗi; tổng duyệt demo.

Team B có tải code nhẹ hơn các team khác vì phần báo cáo "Issues Encountered: mất kết nối" là phần dài nhất.

### B-06. Hiệu chỉnh chấm công: use case và endpoint (B, L, B1, 16 đến 18/11)

| | |
| --- | --- |
| Mục tiêu | HR sửa một bản ghi công sai một cách có kiểm soát (FR-26) |
| Sửa ở | Port vào mới (ví dụ `IAttendanceCorrection`) và use case trong `application/usecase/` dùng `AttendanceCorrection`, `IAttendanceCorrectionRepository`, `AttendanceEventFactory.attendanceCorrected`; endpoint cho HR trong `adapter/in/rest/`; bean `PrincipalResolver` (theo mẫu `UserQrConfig.principalResolver`) để kiểm vai `HR_STAFF`; mô tả endpoint trong `internal-attendance.openapi.yaml` cùng PR |
| Code theo | `attendance-events.asyncapi.yaml` (`CorrectionPayload`: `reason` là mã UPPER_SNAKE_CASE, `aggregateVersion` = phiên bản gốc + `revision`); `TestTokens.withRoles(...)` cho test quyền |
| Xong khi | Test use case: hiệu chỉnh tạo bản ghi mới liên kết bản gốc (không sửa đè), ghi dòng outbox `AttendanceCorrected` và `AuditRecorded` (người sửa, mã lý do) **trong cùng transaction**; gọi lại cùng khóa lũy đẳng không tạo bản thứ hai; vai khác `HR_STAFF` nhận 403. Test IT trong `PersistenceAndBrokerIT`: use case ném lỗi giữa chừng thì không còn bản hiệu chỉnh lẫn dòng outbox |

### B-07. Xác minh lượt mở thủ công (N, M, B1, 19 đến 20/11 sáng)

| | |
| --- | --- |
| Mục tiêu | Lượt `MANUAL` được HR xác minh thì thành công hợp lệ (FR-20, UC-06 bước cuối) |
| Sửa ở | Use case mới, endpoint HR; phát `AttendanceConfirmed` cho lượt đã xác minh |
| Code theo | `AttendanceConfirmed` trong `attendance-events.asyncapi.yaml` (không đổi lược đồ); `ManualOpenTest` |
| Xong khi | Test: lượt thủ công chưa xác minh không tính công (giữ test cũ xanh); sau khi `HR_STAFF` xác minh thì có bản ghi công và dòng outbox `AttendanceConfirmed`; xác minh hai lần không tạo hai bản |

### B-01 (phần còn lại). Test rollback (B, S, B2, 16/11 sáng)

| | |
| --- | --- |
| Mục tiêu | Chứng minh tính nguyên tử trên PostgreSQL thật, điều bản bộ nhớ không chứng minh được |
| Sửa ở | `PersistenceAndBrokerIT` |
| Code theo | `SpringTransactionRunner`, `TmcContainers` |
| Xong khi | Test mới: một use case ném lỗi sau khi ghi lượt quét thì cả lượt quét lẫn dòng outbox đều không được lưu. `./gradlew :services:attendance:integrationTest` xanh |

### B-02 (phần còn lại). Dừng và bật lại RabbitMQ (B, M, B2, 16/11 chiều đến 17/11)

| | |
| --- | --- |
| Mục tiêu | ADR-11 điểm P7 trên hạ tầng thật: broker chết giữa lúc phát thì không mất, không trùng |
| Sửa ở | Test IT mới (thẻ `integration`) với RabbitMQ của `TmcContainers` và `PublishOutboxUseCase` |
| Code theo | `MessagingInfraIT.should_report_not_published_when_the_broker_is_unreachable` (đã có ở mức thư viện); exchange `tmc.events`, khóa `attendance.<EventType>` |
| Xong khi | Test: dừng broker thì `PublishOutboxUseCase.run()` không đánh dấu `PUBLISHED`, dòng còn `PENDING`; bật lại thì mọi sự kiện tới hàng đợi đúng thứ tự ghi; dòng đã `PUBLISHED` không bị phát lại |

### B-03. Hàng đợi bất thường cho HR (B, M, B2, 18 đến 19/11)

| | |
| --- | --- |
| Mục tiêu | HR liệt kê được lượt có cờ `TOKEN_REUSED_OFFLINE`, `ANTI_PASSBACK_OFFLINE` chưa xử lý (UC-09) |
| Sửa ở | Truy vấn `anomalies(...)` trong `IAttendanceQuery`, `IAttemptRepository` và cả hai adapter; endpoint HR; mô tả trong OpenAPI cùng PR |
| Code theo | `Anomaly`; `IngestOfflineBatchUseCase` (nơi gắn cờ); `PrincipalResolver` đã nối ở B-06 |
| Xong khi | Test use case và IT JDBC: lọc theo nhà máy và khoảng thời gian; vai khác `HR_STAFF` nhận 403 |

### B-08. Chỉ số vận hành (N, S, B2, 20/11)

Số dòng outbox tồn quá 5 phút và số lượt `GATE_UNKNOWN` qua Micrometer ở `/actuator` (AC-11). **Xong khi:** test đọc được hai chỉ số từ `MeterRegistry` sau khi tạo dữ liệu tương ứng. Cắt nếu trễ.

### R-B. Phần báo cáo "mất kết nối" (B1; nháp 20/11, bản cuối 24/11)

- **Issues Encountered: mất kết nối.** Vấn đề, 7 điểm đứt, cơ chế (ghi trước làm sau, khóa lũy đẳng, không gửi lại lệnh mở, đối soát, outbox), đánh đổi; bằng chứng là tên test trong `Adr11ResilienceTest`, `PersistenceAndBrokerIT`, test B-02, cùng `demo-offline` và `demo-timeout`.
- Cập nhật NFR-09 và UC-08 theo `docs/adr/README.md` mục 3.
- Sơ đồ máy trạng thái và sơ đồ outbox, khớp với mã (exchange `tmc.events`, ADR-15).
- SOLID: `GateResultApplier` (một lý do để thay đổi), các port `IAttendanceCommand`, `IAttendanceQuery`, `IOfflineAttendanceIngest` tách riêng, `ITransactionRunner` và `EventPublisher` (phụ thuộc vào trừu tượng). Một đoạn phản ánh.

## 4. Đã xong, không cần làm lại

| Story | Ở đâu |
| --- | --- |
| B-04 `direction`, `employeeUuid` trong `AttemptView` | `AttemptView`, `internal-attendance.openapi.yaml` |
| B-05 last-direction | `InternalAttendanceController`, `HttpAttendanceGatewayTest` phía gate-station |
| B-10 `JdbcProcessedMessageStore` | `platform/messaging/.../jdbc/JdbcProcessedMessageStore.java`, `MessagingInfraIT` |
| Lược đồ và nền lưu trữ của `AttendanceCorrected` | Xem hộp đầu tài liệu |
| B-01, B-02 (phần lớn) | `PersistenceAndBrokerIT` 8 test, `MessagingInfraIT` |

## 5. Kiểm tích hợp tùy chọn

| Mã | Ngày | Cùng team | Kiểm gì |
| --- | --- | --- | --- |
| I-1 | 19/11 | A | Gate-station gọi last-direction thật khi Redis mất dữ liệu (A-03) |
| I-3 | 19/11 | D | Hiệu chỉnh qua RabbitMQ: reporting áp bản hiệu chỉnh, hr-integration ghi nhận |
| I-6 | 23/11 | cả nhóm | 3 demo trên stack Docker đầy đủ, attendance ở profile mặc định |

Không story nào ở trên chờ những mục này.

## 6. Hợp đồng Team B giữ ổn định

Gate-station gọi mọi endpoint của `InternalAttendanceController`; ba consumer của Team D đọc mọi sự kiện `attendance.*`. Thêm trường hoặc endpoint thì được (sửa OpenAPI, AsyncAPI cùng PR, mời bên dùng duyệt); đổi tên hoặc bỏ trường cần ADR.

## 7. Bẫy cần tránh

- **Mọi thay đổi trạng thái và dòng outbox của nó phải cùng một transaction** (`tx.inTransaction(...)`, ADR-04).
- **Use case không tự đặt trạng thái.** Chỉ gọi phương thức của `AccessAttempt`.
- **Mọi lệnh ghi đều lũy đẳng** theo `attemptId` (hoặc khóa hiệu chỉnh).
- **Sự kiện không chứa `jti`, token, hay tên người.** Có test kiểm điều này.
- **Dòng outbox chỉ thành `PUBLISHED` sau khi broker xác nhận.**
- **Hiệu chỉnh không sửa đè bản gốc.** Consumer áp bản có `aggregateVersion` lớn nhất.
