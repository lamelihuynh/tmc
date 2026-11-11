# Team B: Attendance

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này.

## 1. Nhiệm vụ của team

Team B giữ **sổ cái của hệ thống**: mọi lượt quét, trạng thái của nó, bản ghi công, và việc phát sự kiện ra ngoài mà không mất và không trùng.

| Sở hữu | Nội dung |
| --- | --- |
| `services/attendance` (cổng 8082) | Máy trạng thái lượt quét, bản ghi công, outbox, nhập lô offline, phục hồi, đối soát |
| Hợp đồng | `docs/api/internal-attendance.openapi.yaml`, `docs/api/attendance-events.asyncapi.yaml` |
| Tài liệu | ADR-11, `docs/architecture/state-machine.md`, phần "mất kết nối" của báo cáo |

Team B nằm trên **đường găng**: B-01 rồi B-02 phải xong ngày 18/11, vì phần trung tâm của Team D chờ nó.

## 2. Bản đồ mã của attendance

Gói gốc: `services/attendance/src/main/java/vn/tmc/attendance/`.

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | **`AccessAttempt`**, `AttemptStatus` | Thực thể lượt quét và các chuyển trạng thái hợp lệ |
| | `AttendanceTransaction` | Bản ghi công, mỗi lượt quét tối đa một bản |
| | `Anomaly`, `AttemptSource` | Cờ bất thường; nguồn `ONLINE`, `OFFLINE`, `MANUAL` |
| `application.usecase` | `AttendanceCommandService` | Ghi lượt quét, đánh dấu đã ra lệnh, ghi kết quả cổng |
| | `GateResultApplier` | Nơi duy nhất biến kết quả cổng thành trạng thái, bản ghi công và sự kiện |
| | `IngestOfflineBatchUseCase` | Nhập journal của kiosk, phát hiện token dùng lại và anti-passback |
| | `RecoverStaleAttemptsUseCase` | Nhả giữ chỗ quá hạn; chuyển lượt treo sang `GATE_UNKNOWN` |
| | `ReconcileUnknownUseCase` | Hỏi gate-station rồi chốt lượt `GATE_UNKNOWN` |
| | `PublishOutboxUseCase` | Đẩy outbox lên broker, chỉ đánh dấu khi broker xác nhận |
| | `AttendanceEventFactory` | Dựng sự kiện, không chứa dữ liệu cá nhân |
| `application.port.out` | `IAttemptRepository`, `IAttendanceTransactionRepository`, `IOutboxRepository`, `ITransactionRunner`, `IGateStatusQuery` | Mọi thứ use case cần ở bên ngoài |
| `adapter.in.rest` | `InternalAttendanceController` | API nội bộ cho gate-station |
| `adapter.in.scheduler` | `MaintenanceScheduler` | Chạy outbox (1 giây), phục hồi (5 giây), đối soát (10 giây) |
| `adapter.out.inmem` | `InMemoryAttemptRepository`, `InMemoryOutboxRepository`, `InMemoryStore`, ... | Bản dùng cho test và profile `local` |
| `adapter.out.persistence` | `JdbcAttemptRepository`, `JdbcAttendanceTransactionRepository`, `JdbcOutboxRepository`, `SpringTransactionRunner` | Bản PostgreSQL, **chưa kiểm chứng** |
| `adapter.out.messaging` | `AmqpEventPublisher` | Bản RabbitMQ, **chưa kiểm chứng** |
| `adapter.out.http` | `HttpGateStatusQuery` | Hỏi gate-station khi đối soát |
| `bootstrap` | `UseCaseConfig`, `InMemoryProfileConfig`, `InfrastructureProfileConfig` | Chọn adapter theo profile |

Lược đồ cơ sở dữ liệu: `src/main/resources/db/migration/V1__attendance_schema.sql`.

## 3. Chạy và kiểm thử phần của team

```bash
./gradlew :services:attendance:test
./gradlew :services:attendance:test --tests '*Adr11ResilienceTest'
./gradlew :services:attendance:integrationTest      # cần Docker
```

| Lớp test | Kiểm gì |
| --- | --- |
| `AccessAttemptTest` | Chuyển trạng thái hợp lệ và không hợp lệ |
| `Adr11ResilienceTest` | Sáu test bắt buộc của ADR-11: gửi trùng, hết giờ chờ, máy chủ chết, broker chết, lô offline, giữ chỗ quá hạn |
| `ManualOpenTest` | Mở thủ công không tự tính công |
| `InternalApiFlowTest` | API nội bộ qua MockMvc với profile `inmem` |
| `PersistenceAndBrokerIT` | Các repository JDBC với PostgreSQL thật (thẻ `integration`) |
| `ArchitectureTest` | Chiều phụ thuộc giữa các gói |

`AttendanceFixture` (trong `src/test/.../support`) dựng cả ứng dụng trên adapter trong bộ nhớ, kèm `MutableClock` để tua thời gian.

Chạy riêng attendance: `SPRING_PROFILES_ACTIVE=inmem ./gradlew :services:attendance:bootRun`. Ở profile này bộ đối soát dùng bản giả, luôn trả "chưa xác định".

Thử tay API:

```bash
curl -s http://localhost:8082/internal/v1/attempts/<attempt_id>
```

## 4. Việc phải làm

**Mọi hạn là 23:00 của ngày được ghi**, tính theo thời điểm PR hợp nhất vào `main`. Khi một story ghi khoảng ngày (ví dụ "16 đến 17/11"), hạn là 23:00 của ngày cuối. Toàn cảnh lịch: [03-gantt.md](03-gantt.md).

### B-05. Chiều của lượt công gần nhất (bắt buộc, S, 12 đến 13/11, B1). **Team A đang chờ.**

Gate-station cần hỏi "lượt công gần nhất của nhân viên này là vào hay ra" để dựng lại anti-passback khi Redis mất dữ liệu (A-03).

- **Sửa ở:** thêm `lastDirection(UUID employeeUuid)` vào `IAttendanceQuery` và `AttendanceQueryService` (đã có sẵn `IAttendanceTransactionRepository.lastDirectionBefore`); thêm `GET /internal/v1/employees/{uuid}/last-direction` vào `InternalAttendanceController`; mô tả trong `internal-attendance.openapi.yaml`.
- **Xong khi:** test trả đúng chiều và thời điểm của bản ghi công mới nhất; nhân viên chưa có bản ghi thì trả 404 hoặc thân rỗng (chốt một cách và ghi vào OpenAPI).
- **Hạn hợp nhất: 23:00 ngày 13/11.**

### B-04. Thêm `direction` và `employeeUuid` vào `AttemptView` (nên có, S, 12 đến 13/11, B2)

- **Sửa ở:** `application/port/in/AttemptView.java` và OpenAPI.
- **Xong khi:** `GET /internal/v1/attempts/{id}` trả thêm hai trường; thay đổi là thêm trường nên không phá client cũ.

### Lược đồ `AttendanceCorrected` (13/11, B1). **Team D đang chờ.**

Thêm lược đồ sự kiện vào `attendance-events.asyncapi.yaml` trước khi hiện thực B-06, để Team D viết D-04 song song. Tối thiểu: `eventId` của bản ghi gốc, `employeeUuid`, `direction`, `scannedAt` mới, lý do hiệu chỉnh, `aggregateVersion` tăng.

### B-01. Kiểm chứng tầng JDBC (bắt buộc, M, 16 đến 17/11, B1)

Bốn lớp trong `adapter/out/persistence` đã viết nhưng chưa từng chạy với PostgreSQL.

- **Việc:** chạy `./gradlew :services:attendance:integrationTest`, sửa cho tới khi `PersistenceAndBrokerIT` xanh. Sau đó chạy service với profile mặc định và chạy 3 demo.
- **Bổ sung test cho:** `existsOtherWithJti`, `findByStatusUpdatedBefore`, `lastDirectionBefore`, và **rollback**: một use case ném lỗi giữa chừng thì cả lượt quét lẫn dòng outbox đều không được lưu.
- **Xong khi:** test tích hợp xanh; 3 demo qua với PostgreSQL thật.
- **Điểm dễ sai:** kiểu `TIMESTAMPTZ` với `Instant`; cột `JSONB` của outbox; `EventEnvelope` đọc lại từ JSON phải bằng bản gốc.

### B-02. Outbox với RabbitMQ thật (bắt buộc, M, 16 đến 18/11, B2). **Team D và C đang chờ.**

- **Việc:** kiểm chứng `AmqpEventPublisher`: phát kèm xác nhận của broker, tên exchange `tmc.attendance.events`, khóa định tuyến `attendance.<eventType>`.
- **Sửa ở:** thêm test tích hợp dùng Testcontainers RabbitMQ.
- **Xong khi** có test chứng minh:
  1. Sự kiện tới đúng hàng đợi, đúng thứ tự ghi.
  2. Dừng broker: `PublishOutboxUseCase.run()` không đánh dấu `PUBLISHED`, dòng outbox còn `PENDING`.
  3. Bật lại broker: mọi sự kiện được phát, không mất.
  4. Dòng đã `PUBLISHED` không bị phát lại ở lần chạy sau.
- **Bắt đầu sớm:** B2 có thể làm với Testcontainers RabbitMQ từ 16/11 mà không cần chờ B-01, bằng cách dùng `InMemoryOutboxRepository` cho phần outbox.
- **Hạn: 18/11, mục tiêu xong trong ngày 17/11** để còn thời gian cho B-10. Báo tình trạng mỗi sáng.

### B-10. `JdbcProcessedMessageStore` (nên có, M, 18/11, B2). **Team D đang chờ.**

Bên nghe sự kiện cần ghi nhớ thông điệp đã xử lý vào cơ sở dữ liệu để không xử lý trùng sau khi khởi động lại. Team B đã quen JDBC và Testcontainers sau B-01 nên nhận việc này thay Team D. B2 làm ngay sau khi xong B-02; nếu B-02 kéo tới hết ngày 18/11 thì B-10 lùi sang sáng 19/11 và báo Team D (Team D có phương án thay thế, xem H9).

- **Sửa ở:** `platform/messaging`: lớp `JdbcProcessedMessageStore` hiện thực `ProcessedMessageStore`, kèm câu lệnh tạo bảng `processed_messages (consumer, message_id, processed_at)` với khóa chính `(consumer, message_id)`.
- **Xong khi:** test chèn cùng `message_id` hai lần thì lần hai báo "đã xử lý"; hai consumer khác tên không chặn nhau.
- **Lưu ý:** `platform/*` là mã dùng chung, PR cần cả bốn lead duyệt. Giữ lớp này không phụ thuộc Spring Boot, chỉ cần `JdbcTemplate`.

### B-03. Hàng đợi bất thường cho HR (bắt buộc, M, 19 đến 20/11, B2)

Lượt quét offline có cờ `TOKEN_REUSED_OFFLINE` hoặc `ANTI_PASSBACK_OFFLINE` hiện được lưu nhưng không ai liệt kê được.

- **Sửa ở:** thêm truy vấn `anomalies(...)` vào `IAttendanceQuery` và `IAttemptRepository` (cả hai adapter), thêm endpoint và mô tả OpenAPI.
- **Xong khi:** HR (vai trò `HR_STAFF`) liệt kê được các lượt có cờ bất thường chưa xử lý, lọc theo nhà máy và khoảng thời gian; vai trò khác nhận 403.

### B-06. Hiệu chỉnh chấm công (bắt buộc, L, 18 đến 20/11, B1). **Team D đang chờ.**

HR sửa một bản ghi công sai một cách có kiểm soát (FR-26).

- **Sửa ở:** use case mới trong `application/usecase`, port vào mới, endpoint cho HR, sự kiện `attendanceCorrected` trong `AttendanceEventFactory`.
- **Xong khi:** hiệu chỉnh tạo bản ghi mới liên kết với bản gốc (không sửa đè), phát `AttendanceCorrected` với `aggregateVersion` tăng, ghi `AuditRecorded` có người sửa và lý do; chỉ `HR_STAFF` được làm; hiệu chỉnh hai lần cùng khóa lũy đẳng không tạo hai bản.
- **Thứ tự:** lược đồ sự kiện đã chốt từ 13/11. Hợp nhất trước 23:00 ngày 20/11.

### B-08. Chỉ số vận hành (nên có, S, 23/11, B2)

Số dòng outbox tồn quá 5 phút và số lượt `GATE_UNKNOWN`, xuất qua Micrometer ở `/actuator`. Cắt nếu trễ.

## 5. Team B cần gì và nợ gì

| Chiều | Hạn (23:00) | Nội dung |
| --- | --- | --- |
| Giao cho A | 13/11 | B-05, B-04 |
| Giao cho D | 13/11 | Lược đồ `AttendanceCorrected` |
| Giao cho D, C | 18/11 | B-02: outbox phát lên RabbitMQ thật |
| Giao cho D | 18/11 | B-10: `JdbcProcessedMessageStore` |
| Giao cho D | 20/11 | B-06: sự kiện `AttendanceCorrected` thật; bản nháp báo cáo |
| Nhận từ A | luôn luôn | `GET /internal/v1/attempts/{id}/gate-observation` để đối soát |
| Nhận từ D | 12/11, 16/11 | Compose chạy được; CI |

**Đổi API nội bộ hoặc lược đồ sự kiện thì phải báo trước.** Gate-station gọi mọi endpoint của `InternalAttendanceController`; ba consumer của Team D đọc mọi sự kiện. Thêm trường thì được; đổi tên hoặc bỏ trường cần ADR.

## 6. Bẫy cần tránh

- **Mọi thay đổi trạng thái và dòng outbox của nó phải cùng một transaction.** Dùng `tx.inTransaction(...)`. Ghi outbox bên ngoài transaction là mất bảo đảm của ADR-04.
- **Use case không tự đặt trạng thái.** Chỉ gọi các phương thức của `AccessAttempt` (`reserve`, `commandGate`, `gateOpened`, ...); thực thể tự kiểm chuyển trạng thái hợp lệ.
- **Mọi lệnh ghi đều lũy đẳng theo `attemptId`.** Gọi lại thì trả kết quả đã lưu với `duplicate = true`, không tạo gì mới.
- **Sự kiện không được chứa `jti`, token, hay tên người.** Có test kiểm điều này; đừng tắt nó.
- **Dòng outbox chỉ thành `PUBLISHED` sau khi broker xác nhận.** Hết giờ chờ xác nhận nghĩa là chưa phát.
- **Bản bộ nhớ không có rollback.** Test trên `InMemoryStore` không chứng minh được tính nguyên tử; điều đó phải chứng minh bằng test tích hợp (B-01).

## 7. Phần báo cáo của Team B

Team B viết phần lớn nhất. Nộp bản nháp ngày 20/11 vào `docs/report/`:

- **Issues Encountered: mất kết nối.** Vấn đề, 7 điểm đứt, cơ chế (ghi trước làm sau, khóa lũy đẳng, không gửi lại lệnh mở, đối soát, outbox), đánh đổi, và bằng chứng: tên test trong `Adr11ResilienceTest` cho từng điểm đứt, cùng `demo-offline` và `demo-timeout`.
- Cập nhật NFR-09 và UC-08 theo `docs/adr/README.md` mục 3.
- Sơ đồ máy trạng thái và sơ đồ outbox, khớp với mã.
- Áp dụng SOLID trên mã của team, ví dụ: `GateResultApplier` (một lý do để thay đổi), các port `IAttendanceCommand`, `IAttendanceQuery`, `IOfflineAttendanceIngest` tách riêng (interface nhỏ), `ITransactionRunner` và `EventPublisher` (phụ thuộc vào trừu tượng).
- Một đoạn phản ánh.
