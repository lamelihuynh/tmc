# ADR-15: Một exchange sự kiện dùng chung `tmc.events` và outbox dùng chung trong `platform/messaging`

| Trạng thái | Accepted (16/11/2026) |
| --- | --- |
| Ngày | 16/11/2026 |
| Người đề xuất | Team D (tích hợp), theo yêu cầu gỡ phụ thuộc giữa các team |
| Người duyệt | Lead cả bốn team (thay đổi `platform/messaging` và `docs/api`) |
| Vấn đề giải quyết | C-08: user-qr, identity, gate-station phải gửi audit tới audit-admin nhưng chỉ attendance có outbox và publisher; mỗi consumer khai báo exchange `tmc.attendance.events` theo kiểu riêng |
| Thay thế / mở rộng | Mở rộng ADR-02 (event-driven), ADR-04 (outbox), ADR-05 (idempotency) |

## Bối cảnh (Context)

Ở scaffold, outbox, publisher AMQP và quy tắc định tuyến nằm trong `services/attendance`. Exchange tên `tmc.attendance.events`
gắn tên một service vào hạ tầng chung. Audit từ ba service khác (C-08) hoặc phải sao chép mã đó ba lần, hoặc phải chờ một
quyết định chung. Phía nhận, reporting, hr-integration, audit-admin đều có listener nhưng chưa từng chạy với RabbitMQ và dùng
bộ khử trùng trong bộ nhớ.

## Quyết định (Decision)

1. **Một topic exchange bền `tmc.events`** cho mọi service. Khóa định tuyến `<nguồn>.<EventType>`, nguồn là tên ngắn:
   `attendance`, `user-qr`, `identity`, `gate-station` (`platform/messaging/TmcExchange`). Khóa của attendance giữ nguyên
   (`attendance.AttendanceConfirmed`...), chỉ đổi tên exchange.
2. **Mỗi consumer một hàng đợi bền và một DLQ** `<hàng đợi>.dlq`, thử lại 3 lần rồi chuyển DLQ:
   reporting `attendance.*`; hr-integration `attendance.AttendanceConfirmed`, `attendance.AttendanceCorrected`;
   audit-admin `*.AuditRecorded`.
3. **Outbox dùng chung** trong `platform/messaging`: `OutboxStore` (`InMemoryOutboxStore`, `JdbcOutboxStore` cùng khuôn bảng
   `outbox_events` với attendance), `OutboxRelay` (dừng ở lỗi đầu tiên để giữ thứ tự), `AmqpEnvelopePublisher` (publisher
   confirm), `AuditEvents` và `AuditOutbox` để dựng `AuditRecorded`. Mỗi service tự tạo bảng trong migration của mình.
4. **Khử trùng phía nhận** bằng `JdbcProcessedMessageStore` (bảng `processed_messages`, khóa `(consumer, message_id)`),
   chạy chung transaction với handler qua `IdempotentConsumer.TransactionRunner`: handler lỗi thì dấu "đã xử lý" bị hoàn tác.
5. Thông điệp **không có hàng đợi nào bind** vẫn được broker xác nhận và bỏ đi: consumer chưa triển khai không được làm tắc
   outbox của bên phát.
6. Thư viện Spring JDBC, AMQP, Jackson là `compileOnly` trong `platform/messaging`: service chạy không Docker không kéo theo.

## Phương án đã cân nhắc

| Phương án | Vì sao không chọn |
| --- | --- |
| Mỗi service một exchange (`tmc.user-qr.events`...) | audit-admin phải bind N exchange, thêm nguồn mới phải sửa consumer |
| Giữ `tmc.attendance.events` cho mọi nguồn | Tên sai nghĩa, dễ nhầm sự kiện audit với công |
| Mỗi service tự viết outbox | Ba bản sao của cùng logic dễ lệch về thứ tự và xác nhận |
| **Một exchange `tmc.events`, khóa `<nguồn>.<EventType>`, outbox trong platform** | **Chọn:** consumer bind theo mẫu, thêm nguồn không sửa consumer, một bản mã được kiểm chứng |

## Hệ quả (Consequences)

(+) C-08 ở user-qr và identity chỉ còn trỏ bean `IAuditLog` sang `AuditOutbox` (đã có sẵn bean, relay và publisher).
(+) Outbox → relay → RabbitMQ → consumer idempotent đã chạy thật (`MessagingInfraIT`), cùng IT của ba consumer.
(–) Đổi tên exchange: hàng đợi cũ bind với `tmc.attendance.events` trên broker dev không còn nhận gì; xóa chúng hoặc `make nuke`.
(–) identity chưa có cơ sở dữ liệu nên outbox của nó ở bộ nhớ, **không bền**: audit đăng nhập có thể mất khi khởi động lại
cho tới khi identity có database (Team C).
(–) Read model của reporting và nhật ký HR vẫn ở bộ nhớ; dấu khử trùng ở PostgreSQL bền hơn trạng thái chiếu. Khởi động lại
mất trạng thái chiếu nhưng không làm sai dữ liệu đã có (D-04, D-06).
(–) DLQ chưa có cảnh báo (Team D).

## Kiểm chứng tuân thủ (Compliance)

- `MessagingInfraIT` (`platform/messaging`, PostgreSQL + RabbitMQ thật): định tuyến `user-qr.AuditRecorded` tới `*.AuditRecorded`
  và không tới `attendance.*`, khử trùng, hoàn tác dấu khi handler lỗi, publisher báo lỗi khi broker không tới được.
- `AttendanceEventListenerIT`, `HrAttendanceListenerIT` (gồm DLQ), `AuditEventListenerIT`.
- `OutboxRelayTest` (thứ tự và dừng ở lỗi đầu), `LocalProfileContextTest` (profile `local` không cần hạ tầng).

## Vị trí trong kiến trúc

| View | Thay đổi |
| --- | --- |
| Module view | `platform/messaging/{TmcExchange, AuditEvents, outbox/*, jdbc/*, amqp/*}` |
| Component and connector view | Mọi bên phát nối tới `tmc.events`; mỗi consumer một hàng đợi + DLQ |
| Deployment view | RabbitMQ ở Central có một exchange `tmc.events` |
| API / sự kiện | `docs/api/attendance-events.asyncapi.yaml` (kênh `<nguồn>.<EventType>`) |

## Liên kết

ADR-02, ADR-04, ADR-05, ADR-10, ADR-14; NFR-02; story B-02, C-08, D-03, D-04, D-06, D-08.
