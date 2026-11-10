# Quy ước thiết kế API và sự kiện

Hợp đồng chính thức nằm trong `docs/api/*.yaml`. Tài liệu này là các quy ước chung để các hợp đồng nhất quán.

## Nhóm API theo tác nhân (ISP, ADR-08)

| Tiền tố | Ai gọi | Ai phục vụ | Xác thực |
| --- | --- | --- | --- |
| `/kiosk/v1/**` | Kiosk | gate-station | mTLS ở Edge Gateway (chưa cấu hình, D-11) |
| `/security/v1/**` | Bảo vệ | gate-station | Header danh tính + `Rbac` |
| `/self-service/v1/**` | Nhân viên | user-qr | Header danh tính |
| `/hr/v1/**` | HR Staff | user-qr, ... | Header danh tính + MFA (C-05) |
| `/supervision/v1/**` | Quản lý sản xuất | reporting | Header danh tính + phạm vi |
| `/admin/v1/**` | Quản trị | audit-admin, hr-integration, ... | Header danh tính, MFA |
| `/internal/v1/**` | Service khác | attendance, gate-station, user-qr | mTLS giữa các service, **không** qua gateway công khai |

Thêm đường dẫn mới: chọn nhóm theo **người gọi**, không theo service.

## Danh tính và phạm vi

Gateway xác thực JWT của Identity rồi chèn header; service **không tin** header nếu request không đến từ gateway (cấu hình mạng bảo đảm, C-06 sẽ thay bằng JWT trực tiếp).

| Header | Ý nghĩa |
| --- | --- |
| `X-Subject` | UUID nội bộ của người gọi |
| `X-Roles` | Danh sách vai trò, phân tách dấu phẩy, đúng `Role` (`EMPLOYEE`, `PRODUCTION_SUPERVISOR`, `HR_STAFF`, `SECURITY_GUARD`, `SYSTEM_ADMIN`, `EXECUTIVE`) |
| `X-Scope-Factories` | `BD`, `LA`, hoặc `*` cho toàn bộ |
| `X-Scope-Gates` | Danh sách cổng cho bảo vệ |

Header thiếu hoặc sai định dạng được coi là **chưa xác thực** (HTTP 403 qua `AccessDeniedException`), không bao giờ là "xác thực một phần". Parser dùng chung: `platform/security/src/main/java/vn/tmc/platform/security/PrincipalHeaders.java`.

## Header chung

| Header | Bắt buộc | Ghi chú |
| --- | --- | --- |
| `Idempotency-Key` | Với mọi lệnh ghi kiosk gửi | **Là `attempt_id`** (UUID v7 do kiosk sinh). Nhận lại cùng khóa trả kết quả cũ |
| `X-Correlation-Id` | Khuyến nghị | Đi xuyên suốt và vào mọi dòng log và sự kiện |
| `X-Kiosk-Id` | Với `/kiosk/**` | Khớp chứng chỉ mTLS |

## Phiên bản và đặt tên

- Phiên bản trong đường dẫn: `/v1`. Thêm trường là tương thích; đổi nghĩa, xóa, đổi kiểu là **phá vỡ**, cần `/v2` hoặc ADR.
- `/kiosk/**`: JSON dùng **snake_case** theo đặc tả M1 mục 16.2 (`attempt_id`, `qr_token`, `reason_code`). Mọi API khác: **camelCase**. Đây là hiện trạng; không đổi giữa chừng.
- Thời gian: ISO-8601 UTC (`2026-11-09T06:45:00Z`). Định danh: UUID chuỗi. Không trả mã nhân viên đọc được.
- Enum là chuỗi `UPPER_SNAKE`; client phải chịu được giá trị lạ (thêm giá trị là tương thích).

## Kết quả nghiệp vụ và mã HTTP của `/kiosk/v1/scans`

| `outcome` | HTTP | Ý nghĩa với kiosk |
| --- | --- | --- |
| `ALLOWED` | 200 | Đã mở và ghi nhận. Hiển thị "Mời qua cổng" |
| `UNCONFIRMED` | 200 | Cổng đã mở, bản ghi sẽ hoàn tất sau. Cho qua; ghi journal |
| `DENIED` | 422 | Từ chối, kèm `reason_code`. Cổng đóng |
| `PENDING` | 202 | Đã gửi lệnh nhưng chưa rõ kết quả. **Không quét lại cùng token.** Đối soát sẽ quyết định; bảo vệ xác minh nếu cần |
| `IN_PROGRESS` | 202 | Request trước của cùng `attempt_id` còn chạy. Hỏi lại cùng khóa |
| (lỗi) | 503 | Phụ thuộc không khả dụng. Gửi lại cùng khóa, hoặc chuyển offline |
| (lỗi) | 400 | Sai định dạng (kể cả `Idempotency-Key` không phải UUID) |

`reason_code` hiện có: `QR_EXPIRED`, `QR_ALREADY_USED`, `QR_REVOKED`, `INVALID_SIGNATURE`, `MALFORMED_TOKEN`, `DEVICE_UNKNOWN`, `DEVICE_REVOKED`, `CLOCK_SKEW_EXCEEDED`, `EMPLOYEE_INACTIVE`, `FACTORY_NOT_PERMITTED`, `GATE_DIRECTION_MISMATCH`, `ANTI_PASSBACK_VIOLATION`, `GATE_IN_MAINTENANCE`, `GATE_NOT_FOUND`, `TURNSTILE_TIMEOUT_RETRY_SCAN`, `GATE_HARDWARE_ERROR`, `SYSTEM_RETRY`, `IN_PROGRESS`. Nguồn: `platform/common-kernel/src/main/java/vn/tmc/platform/kernel/ReasonCode.java`. **Chỉ thêm giá trị, không đổi nghĩa.**

## Lỗi

Phản hồi lỗi không chứa chi tiết nội bộ: `{ "error": "FORBIDDEN" }`, thêm `detail` chỉ khi an toàn cho người gọi (ví dụ `ILLEGAL_TRANSITION`). Không trả stack trace. Dùng:

| HTTP | Khi |
| --- | --- |
| 400 | Sai đầu vào |
| 403 | Không xác thực, sai vai trò hoặc ngoài phạm vi (cùng một lỗi cho "không có" và "không phải của bạn") |
| 404 | Không có tài nguyên (chỉ khi người gọi được phép biết) |
| 409 | Chuyển trạng thái không hợp lệ |
| 423 | Tài khoản bị khóa tạm |
| 503 | Phụ thuộc không khả dụng |

## Sự kiện (AsyncAPI, `docs/api/attendance-events.asyncapi.yaml`)

- Exchange `tmc.attendance.events` (topic), khóa định tuyến `attendance.<EventType>`. Mỗi consumer có **hàng đợi riêng** (`tmc.reporting.attendance`, `tmc.hr.attendance`, `tmc.audit.events`).
- Phong bì `EventEnvelope`: `messageId` (một thông điệp, giữ nguyên khi gửi lại), `eventType`, `schemaVersion`, `eventId` (= `attempt_id`, định danh giao dịch), `aggregateVersion`, `occurredAt`, `correlationId`, `source`, `factoryCode`, `gateId`, `payload`.
- Phát **ít nhất một lần**, không thứ tự tuyệt đối. Consumer **phải** khử trùng theo `(consumer, messageId)` và **không** áp phiên bản cũ lên trạng thái mới (`aggregateVersion`).
- `payload` chỉ chứa dữ liệu tối thiểu, UUID nội bộ, **không** token, không `jti`, không tên (ADR-09).
- Thêm trường là tương thích. Đổi nghĩa trường hoặc kiểu thì tăng `schemaVersion` và cần ADR.
- Chỉ ACK thông điệp sau khi đã lưu bền vững kết quả xử lý.

## Gọi hệ thống ngoài

- Mọi lời gọi có timeout; retry có giới hạn và backoff; **chỉ retry khi chắc chắn yêu cầu chưa đến đích** (lỗi kết nối), không retry khi chậm trả lời.
- Gửi khóa idempotency; nếu hệ thống đích không hỗ trợ, tra cứu kết quả trước khi gửi lại.
- Kết quả không rõ là một trạng thái thật (`UNKNOWN`), không gộp vào thất bại hay thành công.
- Ngân sách thời gian của một lượt quét: tổng ≤ 2000 ms; gọi Turnstile ≤ 800 ms (kết nối 200 ms, đọc 800 ms).
