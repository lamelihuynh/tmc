# Bộ tài liệu bàn giao M2

Bộ tài liệu này đủ để một team làm việc độc lập: biết mình làm gì, ở đâu, khi nào xong, chạy và kiểm thử ra sao, cần gì từ team khác và nợ team khác cái gì.

**Thời gian:** Thứ Năm 12/11/2026 đến Thứ Tư 25/11/2026 (10 ngày làm việc). **Mọi hạn là 23:00 của ngày được ghi.**

## Đọc theo thứ tự này

| Bước | Tài liệu | Ai đọc | Thời gian đọc |
| --- | --- | --- | --- |
| 1 | [00-he-thong.md](00-he-thong.md): hệ thống làm gì, gồm những phần nào, một lượt quét đi qua đâu | Mọi người | 15 phút |
| 2 | [02-chay-local.md](02-chay-local.md): cài máy, dựng server local, chạy test, chạy demo, dùng app nhân viên | Mọi người, **làm theo ngay ngày 12/11** | 30 phút, kể cả thời gian chạy |
| 3 | [01-ke-hoach.md](01-ke-hoach.md): lịch 10 ngày, thứ tự bàn giao giữa các team, phạm vi bị cắt | Mọi người, lead đọc kỹ | 15 phút |
| 3b | [03-gantt.md](03-gantt.md): biểu đồ Gantt của lịch, kèm bản tương tác [lich-ban-giao.html](lich-ban-giao.html) | Mọi người | 5 phút |
| 4 | Tài liệu của team mình: [team-a.md](team-a.md), [team-b.md](team-b.md), [team-c.md](team-c.md), [team-d.md](team-d.md) | Team tương ứng | 20 phút |

## Bốn team

| Team | Phụ trách | Mã nguồn sở hữu |
| --- | --- | --- |
| **A. Gate Station** | Điều phối một lượt quét tại cổng, điều khiển cổng xoay | `services/gate-station`, `simulators/turnstile-simulator` |
| **B. Attendance** | Sổ ghi lượt quét, máy trạng thái, outbox phát sự kiện | `services/attendance` |
| **C. User/QR và Identity** | Khóa thiết bị, xác thực QR, đăng nhập | `services/user-qr`, `services/identity`, `simulators/device-simulator`, `platform/security` |
| **D. Kiosk, Portal và Central** | Kiosk, hai ứng dụng web, báo cáo, tích hợp HR, kiểm toán, CI, hạ tầng | `simulators/kiosk-simulator`, `apps/*`, `services/reporting`, `services/hr-integration`, `services/audit-admin`, `docker`, `scripts`, `.github` |

## Quan hệ với tài liệu có sẵn

- Bộ này **thay thế** phần lịch (mục 3, 6, 7) của [`docs/team/WORK-BREAKDOWN.md`](../team/WORK-BREAKDOWN.md), vốn viết cho 4 tuần. Mã story (A-01, B-02, ...) được giữ nguyên để tra cứu chéo.
- Quy ước viết mã, commit, review, kiểm thử vẫn ở [`docs/conventions/`](../conventions/) và [`CONTRIBUTING.md`](../../CONTRIBUTING.md). Bộ này không lặp lại, chỉ trỏ tới.
- Bản trực quan của lịch và các lần bàn giao: <https://claude.ai/artifact/HJQqZixiUDbWPr9JQcR3Kp> (trang riêng tư, người tạo cần chia sẻ thì người khác mới mở được).
- Quyết định kiến trúc ở [`docs/adr/`](../adr/); hợp đồng API ở [`docs/api/`](../api/).

## Hiện trạng tại thời điểm bàn giao (11/11/2026)

Đã kiểm chứng trên Windows 11, JDK 21:

| Hạng mục | Kết quả |
| --- | --- |
| Build toàn bộ 7 service và 4 thư viện `platform` | Qua |
| Test đơn vị và kiến trúc của `services/*`, `platform/*` | 145 chạy, 0 lỗi, 12 bỏ qua có chủ đích (`@Disabled("TODO Team X")`) |
| Ba kịch bản demo trên 4 tiến trình thật (profile `local`) | `online`, `offline`, `timeout` đều qua |
| Ứng dụng nhân viên (`apps/employee-app`) với server thật | Đăng nhập, đăng ký khóa, quét, dùng lại QR, QR 24 giờ, QR chứa tên, sai nhà máy, thu hồi thiết bị: đều cho kết quả đúng |

| Chạy cả 8 service trên PostgreSQL, Redis, RabbitMQ thật (Docker, profile mặc định) | Chạy thử một lượt: Flyway tạo bảng, lượt quét vào PostgreSQL, token vào Redis, 11 sự kiện outbox đều `PUBLISHED`, hr-integration và audit-admin nhận được, reporting hiện đúng người đang ở trong nhà máy; log không có lỗi |
| Web portal cho bảo vệ, quản đốc, HR (`apps/web-portal`) trên stack đó | Mở cổng thủ công, xem hiện diện, xem đồng bộ HR đều có dữ liệu thật (ảnh trong `docs/screenshots/`) |

Lần chạy thử trên hạ tầng thật ở trên chỉ là **một lượt đi qua đường thành công**. Nó cho thấy các adapter Redis, JDBC, AMQP không sai rõ ràng, nhưng chưa chứng minh các tính chất khó (hai luồng tranh nhau, rollback, broker chết giữa chừng). Các story dưới đây vẫn phải làm.

Chưa có test tự động, và là việc đầu tiên của các team:

| Hạng mục | Còn thiếu gì | Story |
| --- | --- | --- |
| `RedisTokenReservation`, `RedisAntiPassbackStore` | Mới chạy thử một lượt, chưa có test tranh chấp và khởi động lại | A-01 |
| `Jdbc*Repository`, `SpringTransactionRunner`, migration Flyway của attendance | `PersistenceAndBrokerIT` (3 test) đã xanh với PostgreSQL thật; còn thiếu test rollback và chạy 3 demo trên PostgreSQL | B-01 |
| `AmqpEventPublisher` và các listener của consumer | Mới chạy thử một lượt, chưa thử broker chết, gửi trùng, DLQ | B-02, D-03 |
| `JdbcDeviceKeyRegistry` | Mới chạy thử một lượt, chưa thử hai lệnh đăng ký song song | C-01 |
| 20 test của `kiosk-simulator` trên Windows | Lỗi khóa file SQLite khi dọn thư mục tạm (macOS và Linux không bị) | D-14 |
| Quét QR bằng camera ở trang `/kiosk` | Máy kiểm chứng không có webcam; đường "dán token" và "gửi tới cổng" đã chạy | D-13 |
