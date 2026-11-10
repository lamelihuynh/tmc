# Ghi chú bảo vệ dữ liệu cá nhân (ADR-09, NFR-04, Nghị định 13/2023/NĐ-CP)

Tài liệu ngắn cho Team C và báo cáo. Phần "cần làm" là việc thật, không phải đã xong.

## Dữ liệu và nơi lưu

| Dữ liệu | Có trong hệ thống? | Nơi | Cách bảo vệ hiện tại |
| --- | --- | --- | --- |
| Họ tên, CCCD, số điện thoại | **Không** nằm trong token QR, sự kiện, journal kiosk, nhật ký | Hồ sơ nhân viên thuộc HR/Payroll (A-04 của M1) | Không đi vào các luồng của TMC |
| Mã nhân viên nội bộ (UUID) | Có | Token (`sub`), sự kiện, bản ghi công | Chỉ là định danh gián tiếp, tham chiếu bằng UUID (DP-06) |
| `device_id`, khóa công khai thiết bị | Có | `devices` (user-qr), cache kiosk | Là định danh gián tiếp; **không** đưa vào sự kiện. Mã hóa lưu trữ ở mức đĩa/CSDL |
| Token QR thô | **Không lưu** | Chỉ trong bộ nhớ khi xử lý | Journal kiosk chỉ lưu SHA-256 |
| Id token (`jti`) | Có | `access_attempts`, Redis | Không phải PII; **không** đưa vào sự kiện (`AttendanceEventFactory`) |
| Thời điểm ra vào, nhà máy, cổng | Có | `attendance_transactions` | RBAC theo vai trò và phạm vi |
| Sinh trắc học, vị trí GPS | **Không** thu thập | không | Ngoài phạm vi (M1 mục 1.4) |

## Biện pháp đã có trong mã

1. `NimbusTokenCodec` từ chối token chứa claim ngoài `sub, jti, iat, exp, fac, nonce`; test `Adr12TokenVerificationTest.should_reject_a_token_that_carries_a_pii_claim`.
2. `AttendanceEventFactory` chỉ đưa UUID nội bộ vào payload; test kiểm không có `name`, `jti`, `token`.
3. `RecordAuditUseCase` và `AuditEntry.details` chỉ chứa định danh.
4. Khi từ chối, thông báo gửi về kiosk chỉ là `reason_code`, không có thông tin người.
5. `DeviceManagementService` trả cùng một lỗi cho "không tồn tại" và "không phải của bạn".

## Việc cần làm (chưa xong)

| Việc | Story | Ghi chú |
| --- | --- | --- |
| Mã hóa dữ liệu lưu (AES-256) | D-11 | Mức đĩa hoặc mã hóa cột; chọn và ghi vào báo cáo |
| TLS 1.2+ giữa mọi thành phần và mTLS cho kiosk | D-11 | Hiện chạy HTTP thuần ở môi trường phát triển |
| Chính sách lưu giữ và xóa dữ liệu hết hạn | C-10 | Job xóa, thời hạn do công ty quyết định |
| Ghi log mọi truy cập PII | C-08, D-08 | Qua audit-admin |
| Quyền của chủ thể dữ liệu (xem, sửa, xóa) | C-10 | Quy trình, không nhất thiết là mã |
| Che mờ (masking) mã nhân viên khi hiển thị cho bảo vệ | C-03, D-10 | FR-09: bảo vệ chỉ xem thông tin tối thiểu |
