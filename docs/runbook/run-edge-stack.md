# Vận hành stack Edge (một nhà máy)

Dành cho người chạy hệ thống để demo, kiểm thử hoặc nghiệm thu. Sơ đồ: `docs/architecture/c4-edge.md`.

## Thứ tự khởi động

1. **Hạ tầng** (chế độ đầy đủ): PostgreSQL edge, Redis, RabbitMQ (`make up`). Chế độ `local` bỏ qua bước này.
2. `turnstile-simulator` (hoặc Turnstile thật): `:8090`.
3. `user-qr`: `:8083` (cần có trước gate-station vì xác thực QR).
4. `attendance`: `:8082`.
5. `gate-station`: `:8081`. Khởi động **sau cùng**, vì nó là cổng vào của kiosk.
6. Kiosk chỉ nên bật sau khi `GET :8081/kiosk/v1/sync-bundle` trả 200.

`make local-up` làm đủ các bước trên. Dừng theo thứ tự ngược lại: kiosk, gate-station, attendance, user-qr, turnstile.

## Kiểm tra nhanh "đang khỏe"

| Kiểm tra | Lệnh | Mong đợi |
| --- | --- | --- |
| Gate Station | `curl -s localhost:8081/kiosk/v1/sync-bundle` | JSON có danh sách `gates` |
| User/QR | `curl -s localhost:8083/internal/v1/sync-bundle` | JSON có `deviceKeys` |
| Turnstile | `curl -s localhost:8090/gates/BD-HW-01/status` | `"state":"ONLINE"` |
| Một lượt quét | `make demo-online` | `online demo passed` |

## Dấu hiệu cần can thiệp

| Dấu hiệu | Ý nghĩa | Việc làm |
| --- | --- | --- |
| Kiosk liên tục trả `PENDING` | Turnstile chậm hoặc lỗi | Xem `turnstile-sim` hoặc thiết bị thật; đối soát tự chạy mỗi vài giây |
| Lượt kẹt ở `RESERVED` hoặc `GATE_COMMANDED` lâu | Một tiến trình đã chết giữa chừng | Job phục hồi của attendance tự xử lý sau 30 giây (`RESERVED`) và 10 giây (`GATE_COMMANDED`); xem nhật ký `recovery:` |
| Lượt ở `GATE_UNKNOWN` mãi | Turnstile không trả lời trạng thái | Bảo vệ xác minh thủ công theo UC-06; đừng gửi lại lệnh mở |
| Outbox tồn đọng | Broker không khả dụng | Không mất dữ liệu; publisher phát lại khi broker trở lại. Cảnh báo `outbox backlog` trong nhật ký |
| Kiosk có hàng chờ offline không giảm | Edge không nhận batch | Chạy `kiosk-sim flush` (hoặc cơ chế của kiosk thật) sau khi Edge lên lại |

## Thay đổi cấu hình khi vận hành

| Muốn | Đổi | Ghi chú |
| --- | --- | --- |
| Đổi turnstile thật ⇄ giả lập | `TURNSTILE_MODE=http|simulated` | Không sửa mã (OCP) |
| Đổi thời hạn giữ chỗ, chờ phục hồi | `tmc.gate.scan.*`, `tmc.attendance.recovery.*` | Khởi động lại service |
| Đổi sai lệch đồng hồ cho phép, tuổi token tối đa | `tmc.qr.clock-skew-seconds`, `tmc.qr.max-lifetime-seconds` | user-qr. Giảm tuổi tối đa làm QR an toàn hơn nhưng nhạy với đồng hồ điện thoại |
| Thêm cổng, nhân viên mẫu | `tmc.gate.gates`, `tmc.gate.employees` trong `application.yml` | Chỉ cho scaffold; story A-04 và C-02 thay bằng CSDL |

## Sao lưu và khôi phục (chế độ đầy đủ)

Chưa có công cụ riêng (FR-35 chưa làm). Tối thiểu: `pg_dump` hằng ngày cho `attendance_edge` và `user_qr`; bật AOF cho Redis (đã bật trong compose). Sau khôi phục, chạy `make demo-online` rồi kiểm tra hàng đợi outbox đã được phát hết.
