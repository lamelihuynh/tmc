# Thay đổi kiến trúc cho Milestone 2 (M2)

Tài liệu này tóm tắt các thay đổi kiến trúc so với báo cáo M1 (`docs/document.md`) để cả nhóm nắm được trước khi sửa báo cáo và bắt đầu code. Ngày cập nhật: 07/11/2026.

## 1. Vì sao phải thay đổi

Đề bài M2 bổ sung mục **"Issues Encountered"** với hai vấn đề và yêu cầu đề xuất giải pháp kiến trúc, giải thích rõ cách giảm thiểu và cách đưa vào kiến trúc:

| # | Vấn đề đề bài nêu | M1 đã có gì | M1 còn thiếu gì |
| --- | --- | --- | --- |
| I-1 | Hệ thống mất kết nối **trong lúc đang xử lý** giao dịch chấm công | NFR-09, UC-08 ngoại lệ 2.x, mục 14.5 (3 lớp chịu lỗi offline), offline queue SQLite, `GET /kiosk/v1/sync-bundle`, interface `IOfflineAttendanceIngest`, trạng thái `PENDING_GATE`/`UNKNOWN` | Chưa có máy trạng thái rõ ràng cho một lượt quét; chưa chỉ ra mất kết nối tại **từng điểm** (giữa xác thực, giữ chỗ token, mở cổng, commit DB, trả kết quả) thì phục hồi thế nào; chưa chứng minh không mất và không trùng bản ghi tại mỗi điểm; kiosk chưa ghi journal **trước** khi gọi mạng |
| I-2 | QR tĩnh bị lộ, dẫn tới vào trái phép hoặc gian lận chấm công | FR-19, UC-07, NFR-05, token JWS ES256 hiệu lực 30 giây, `jti` dùng một lần, Redis `SET NX` | UC-07 (luồng thay thế 1.1) và UC-08 (luồng thay thế 1.2) **vẫn cho phép QR tĩnh qua email có hạn theo ngày**, đây chính là lỗ hổng đề bài nêu; chưa ràng buộc thiết bị; điện thoại mất mạng thì không sinh được QR mới; chưa phát hiện replay khi kiosk offline; chưa có quy tắc phát hiện gian lận |

Ngoài ra, khi rà lại M1 nhóm phát hiện một điểm chưa nhất quán cần chốt trước khi code: mục 13 nói **Gate Station Service** điều phối luồng quét, còn mục 15 nói **Attendance Service (Edge)** điều phối và gọi QR Verifier. ADR-13 chốt lại điểm này.

## 2. Danh sách ADR mới và ADR bị sửa

| Mã | Tên | Loại | Giải quyết | File |
| --- | --- | --- | --- | --- |
| ADR-11 | Offline-first store-and-forward với máy trạng thái giao dịch tại cổng | Mới | I-1 | `ADR-11-offline-first-store-and-forward.md` |
| ADR-12 | QR động có chữ ký, ràng buộc thiết bị và chống replay; loại bỏ QR tĩnh | Mới | I-2 | `ADR-12-dynamic-qr-device-binding.md` |
| ADR-13 | Chốt thành phần điều phối luồng quét tại biên (Edge Access Orchestrator) | Mới | Nhất quán M1, chuẩn bị code | `ADR-13-edge-access-orchestrator.md` |
| ADR-05 | Idempotency cho attendance event | Sửa | Mở rộng: một `Idempotency-Key` duy nhất đi xuyên suốt kiosk, edge, turnstile, outbox (xem ADR-11) | Sửa trực tiếp trong báo cáo |
| ADR-09 | Privacy by Design | Sửa | Bổ sung: khóa thiết bị và `device_id` là dữ liệu định danh, phải mã hóa khi lưu và không đưa vào event (xem ADR-12) | Sửa trực tiếp trong báo cáo |

## 3. Các mục trong báo cáo M1 phải cập nhật theo

| Mục M1 | Việc cần làm | ADR liên quan |
| --- | --- | --- |
| 4.2.3 FR-19 | Bỏ cụm "QR tĩnh"; ghi rõ QR chỉ là token động ký bằng khóa thiết bị | ADR-12 |
| 4.3 NFR-05 | Thêm tiêu chí: 100% token bị replay bị từ chối kể cả khi kiosk offline; token phải ràng buộc thiết bị đã đăng ký | ADR-12 |
| 4.3 NFR-09 | Thêm tiêu chí: 0 bản ghi mất hoặc trùng khi ngắt kết nối tại bất kỳ bước nào của giao dịch; kiosk ghi journal trước khi gọi mạng | ADR-11 |
| 5.7 UC-07 | Xóa luồng thay thế 1.1 (QR tĩnh qua email); thêm bước đăng ký thiết bị; sửa ngoại lệ 5.1 (điện thoại mất mạng vẫn sinh được QR) | ADR-12 |
| 5.8 UC-08 | Xóa luồng thay thế 1.2; viết lại ngoại lệ 2.x theo máy trạng thái của ADR-11 | ADR-11, ADR-12 |
| 6.1 AC-03, AC-04 | Bổ sung fitness function tương ứng | ADR-11, ADR-12 |
| 7.1 bảng ADR | Thêm ADR-11, 12, 13 | tất cả |
| 9 ma trận truy vết | Thêm hai dòng cho I-1, I-2 | tất cả |
| 12.1, 12.4 component view | Thêm Kiosk Journal, Replay Cache, Device Key Registry; đổi tên thành phần điều phối theo ADR-13 | ADR-11, 12, 13 |
| 13.1 sequence quét QR | Vẽ lại theo máy trạng thái và thành phần điều phối mới | ADR-11, ADR-13 |
| 15.2 deployment nhà máy | Thêm Kiosk Journal (thay cho "Offline Queue"), Replay Cache trên Redis, mô tả Edge Agent kéo thêm danh sách khóa thiết bị | ADR-11, ADR-12 |
| 16.2 API kiosk | Thêm trường `journal_seq`, `device_id` vào `POST /kiosk/v1/scans`; sửa mô tả dòng 5 (manual-open) đang bị copy nhầm từ heartbeat | ADR-11, ADR-12 |
| 16.4.3 API HR | Sửa header: đang dùng nhầm `TURNSTILE_EDGE_API_TOKEN` và ghi chú idempotency của turnstile | lỗi M1 |
| Mục mới "Issues Encountered và giải pháp" | Viết một mục riêng, mỗi vấn đề một tiểu mục: mô tả vấn đề, kịch bản lỗi, cơ chế, cách giảm thiểu, vị trí trong từng view, đánh đổi | ADR-11, ADR-12 |

## 4. Ảnh hưởng tới Task 2 (code)

Module được chọn để hiện thực là **Attendance Event Recording tại biên**. Ba ADR này quy định trực tiếp cấu trúc code:

- ADR-13 quyết định tên use case điều phối và các port phải có.
- ADR-11 quyết định máy trạng thái `AccessAttempt`, Kiosk Journal, và bộ test ngắt kết nối tại từng bước.
- ADR-12 quyết định `QrToken`, `ITokenReplayStore`, `IDeviceKeyRegistry`, và bộ test replay/giả mạo.

Chi tiết xem từng file ADR.
