# Áp dụng các nguyên tắc SOLID (Task 1.3)

Phần này trả lời yêu cầu **Task 1.3** về việc chứng minh cách áp dụng năm nguyên tắc SOLID trực tiếp trên mã nguồn của hệ thống (căn cứ từ `docs/architecture/solid-map.md`).

> [!NOTE]
> Phần dưới đây trình bày đầy đủ phân tích **SOLID của Team B (`services/attendance`)**. Phần của Team A, Team C và Team D được giữ chỗ để hợp nhất trong D-12.

---

## 1. Phần của Team B (`services/attendance`)

| Nguyên tắc | Lớp / Port trong `attendance` | Lý giải thiết kế | Bằng chứng kiểm thử |
| --- | --- | --- | --- |
| **S — Single Responsibility** | `GateResultApplier`, `AttendanceEventFactory`, `PublishOutboxUseCase` | `GateResultApplier` có một lý do thay đổi duy nhất: quy tắc chuyển đổi kết quả từ cổng (`OPENED`, `ERROR`, `UNKNOWN`) thành chuyển trạng thái `AccessAttempt`, bản ghi công `AttendanceTransaction` và sự kiện outbox. Cả luồng online (`AttendanceCommandService`) và luồng đối soát nền (`ReconcileUnknownUseCase`) đều dùng chung lớp này để không bị trôi lệch quy tắc. `AttendanceEventFactory` tách riêng trách nhiệm dựng payload không chứa dữ liệu cá nhân (`jti`, tên). | `Adr11ResilienceTest`, `ManualOpenTest`, `Adr11ResilienceTest.should_not_leak_the_token_id_or_any_name_into_event_payloads` |
| **O — Open/Closed** | `IAttemptRepository`, `IOutboxRepository`, `ITransactionRunner`, `EventPublisher` | Có thể thay đổi hoặc mở rộng hạ tầng lưu trữ và truyền thông điệp (từ bản in-memory sang JDBC PostgreSQL và RabbitMQ thật, hoặc thêm adapter mới) bằng cấu hình profile (`InMemoryProfileConfig` ↔ `InfrastructureProfileConfig`) mà không sửa mã nghiệp vụ trong `application/usecase`. | `InternalApiFlowTest` (inmem), `PersistenceAndBrokerIT` (PostgreSQL), `OutboxBrokerRestartIT` (RabbitMQ) |
| **L — Liskov Substitution** | `InMemoryAttemptRepository` ↔ `JdbcAttemptRepository`; `InMemoryOutboxRepository` ↔ `JdbcOutboxRepository`; `InMemoryEventBroker` ↔ `AmqpEnvelopePublisher` | Hai bộ cài đặt giữ nguyên hợp đồng: `save` upsert theo `attempt_id`, `saveIfAbsent` chống trùng, `fetchPending` trả về theo thứ tự ghi và ẩn dòng đã `PUBLISHED`, `publish` chỉ thành công khi broker xác nhận và ném `PublishFailedException` khi broker lỗi. | `Adr11ResilienceTest` (inmem), `PersistenceAndBrokerIT` & `OutboxBrokerRestartIT` (JDBC/RabbitMQ) |
| **I — Interface Segregation** | `IAttendanceCommand`, `IAttendanceQuery`, `IOfflineAttendanceIngest` | Chia nhỏ port vào theo vai trò người gọi: `gate-station` gọi ghi qua `IAttendanceCommand` và nhập lô qua `IOfflineAttendanceIngest`; luồng chỉ đọc (`findAttempt`, `lastPass` cho anti-passback, `anomalies` cho HR) chỉ phụ thuộc vào `IAttendanceQuery`, hoàn toàn không thấy và không thể gọi bất kỳ hàm ghi hay sửa trạng thái nào. | `InternalAttendanceController`, `HrAttendanceController`, `AnomalyQueueTest` |
| **D — Dependency Inversion** | `ITransactionRunner`, `EventPublisher`, `IAttemptRepository`, `IOutboxRepository`, `IGateStatusQuery`, `IClock` | Tất cả interface port ra đều nằm trong gói `vn.tmc.attendance.application.port.out` (hoặc `platform/`), do tầng nghiệp vụ định nghĩa. Tầng `domain` và `application/usecase` không phụ thuộc vào Spring, JDBC, RabbitMQ hay HTTP; việc nối `SpringTransactionRunner`, `AmqpEnvelopePublisher`, `Jdbc*Repository` chỉ thực hiện tại `bootstrap/`. Nhờ đó toàn bộ use case chạy unit test độc lập không cần Spring, DB hay broker. | `ArchitectureTest` (`HexagonalRules.verifyAll(...)`), `Adr11ResilienceTest`, `AnomalyQueueTest`, `OperationalMetricsTest` |

---

## 2. Phần chờ các team khác (D-12 hợp nhất)

- **Team A (`services/gate-station`):** *[Chờ Team A]*
- **Team C (`services/user-qr`, `services/identity`):** *[Chờ Team C]*
- **Team D (`services/reporting`, `services/hr-integration`, `services/audit-admin`):** *[Chờ Team D]*

