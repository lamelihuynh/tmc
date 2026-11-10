# ADR-12: QR động có chữ ký, ràng buộc thiết bị và chống replay; loại bỏ QR tĩnh

| Trạng thái | Proposed (đề xuất cho M2, chờ nhóm duyệt) |
| --- | --- |
| Ngày | 06/11/2026 |
| Vấn đề giải quyết | I-2: QR tĩnh bị lộ, kẻ xấu vào trái phép hoặc gian lận chấm công |
| Thay thế / mở rộng | Mở rộng ADR-08 (Identity), ADR-09 (Privacy); thay thế luồng QR tĩnh qua email trong UC-07 và UC-08 |

## Bối cảnh (Context)

M1 đã thiết kế QR động: token JWS (ES256) hiệu lực 30 giây, `jti` dùng một lần, giữ chỗ bằng Redis `SET NX`. Tuy nhiên vẫn còn bốn lỗ hổng:

| # | Lỗ hổng trong M1 | Cách bị khai thác |
| --- | --- | --- |
| L1 | UC-07 luồng thay thế 1.1 và UC-08 luồng thay thế 1.2 cho phép **QR tĩnh qua email, hạn theo ngày** | Chụp màn hình, chuyển tiếp email → người khác dùng cả ngày; đây chính là tình huống đề bài nêu |
| L2 | Token được **server** sinh và ký; app phải có mạng mới làm mới được (UC-07 ngoại lệ 5.1) | Khi điện thoại mất mạng, nhân viên phải nhờ bảo vệ (UC-06), tạo áp lực đưa QR tĩnh trở lại |
| L3 | Token chỉ gắn với nhân viên (`sub`), không gắn với **thiết bị** | Ai lấy được phiên đăng nhập hoặc token đều dùng được trên điện thoại khác |
| L4 | Khi kiosk offline, kiểm tra `jti` một lần **không hoạt động** (Redis ở Edge) | Một QR chụp màn hình trong 30 giây có thể quét ở hai kiosk offline, hoặc quét lặp ở cùng kiosk |

Ngoài ra M1 chưa có quy tắc phát hiện gian lận ở mức hành vi (ví dụ cùng một nhân viên xuất hiện ở Bình Dương và Long An cách nhau 10 phút).

## Quyết định (Decision)

### 1. Loại bỏ QR tĩnh làm thông tin xác thực tại cổng

- QR tĩnh **không còn là credential** để mở cổng trong bất kỳ trường hợp nào.
- Email chỉ dùng để gửi **liên kết đăng ký thiết bị** (enrollment link, dùng một lần, hết hạn sau 24 giờ), không gửi QR mở cổng.
- Nhân viên không có điện thoại dùng được tại thời điểm đó đi theo UC-06 (bảo vệ xác minh và mở thủ công, có lý do, có audit). Đây là đường lùi có kiểm soát, không phải credential thay thế.

### 2. Ràng buộc thiết bị (device binding)

Khi nhân viên đăng ký app lần đầu (UC-07 bước mới):

1. App sinh cặp khóa bất đối xứng (ECDSA P-256) trong **secure enclave / keystore** của điện thoại; khóa riêng không rời thiết bị.
2. App gửi khóa công khai lên User/QR Service kèm phiên đăng nhập hợp lệ.
3. User/QR Service lưu `device_id`, `device_public_key`, `employee_uuid`, `status = ACTIVE` vào **Device Key Registry** và ghi audit.
4. Mỗi nhân viên có tối đa **1 thiết bị ACTIVE**; đăng ký thiết bị mới tự thu hồi thiết bị cũ (tương đương "làm mới QR" trong UC-02).

HR Staff hoặc nhân viên có thể thu hồi thiết bị (mất điện thoại). Thu hồi được đẩy xuống Edge qua `sync-bundle` như revocation list của M1.

### 3. Token QR do thiết bị ký, sinh được khi không có mạng

Thay vì server ký, **app ký token bằng khóa thiết bị**:

```
header:  { alg: ES256, kid: <device_id> }
payload: { sub: <employee_uuid>, jti: <uuid>, iat, exp: iat + 30s, fac: [BD, LA], nonce }
```

- Kiosk/Edge xác thực bằng khóa công khai tra theo `kid` trong Device Key Registry (được cache tại Edge và kiosk qua `sync-bundle`).
- App có thể sinh token mới mỗi 30 giây **mà không cần mạng**, giải quyết L2.
- Token không chứa PII (giữ ADR-09). `device_id` là định danh gián tiếp, phải mã hóa khi lưu và không đưa vào AttendanceEvents.
- Đồng hồ điện thoại lệch: Edge chấp nhận lệch tối đa 60 giây (`clock_skew`); app đồng bộ thời gian với server khi có mạng và cảnh báo nếu lệch quá ngưỡng.

Cách này giữ nguyên định dạng JWS của M1; chỉ đổi **ai ký** và **khóa nào xác thực**.

### 4. Chống replay nhiều lớp

| Lớp | Nơi | Cơ chế | Che phủ |
| --- | --- | --- | --- |
| R1 | Edge (online) | Redis `SET NX EX jti` với TTL = `exp` + `clock_skew`; giữ như M1 | Replay trong cùng nhà máy khi online |
| R2 | Kiosk (offline) | **Replay cache cục bộ**: tập `jti` đã thấy trong 2 phút gần nhất, lưu trong Kiosk Journal | Quét lặp cùng kiosk khi LAN đứt |
| R3 | Edge khi nhập batch offline | Kiểm tra `jti` đã có trong `access_attempts` của kiosk khác → gắn cờ `TOKEN_REUSED_OFFLINE`, không tính công (ADR-11 mục 5) | Replay giữa hai kiosk offline |
| R4 | Central (Reporting/Audit consumer) | Quy tắc hành vi: cùng `sub` có lượt vào ở hai nhà máy cách nhau dưới ngưỡng di chuyển (cấu hình, mặc định 60 phút); nhiều lượt bị từ chối `QR_EXPIRED`/`INVALID_SIGNATURE` liên tiếp từ cùng `sub` hoặc cùng kiosk | Gian lận không thể thấy ở một nhà máy |

Khi R4 phát hiện bất thường: ghi `AuditRecorded`, cảnh báo Security Guard của cổng liên quan và HR Staff; bản ghi được gắn cờ chờ đối soát (UC-09), không tự động xóa.

### 5. Hiển thị tại kiosk để bảo vệ kiểm tra chéo

Khi mở cổng thành công, kiosk hiển thị **mã nhân viên rút gọn và ảnh đại diện nếu công ty đã có chính sách** trong 2 giây để bảo vệ đối chiếu bằng mắt. Ảnh là PII: chỉ bật khi được phê duyệt theo Nghị định 13/2023, ảnh lưu mã hóa, không ghi vào log. Mặc định trong Task 2: **tắt**, chỉ hiển thị mã nhân viên rút gọn.

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Giữ QR tĩnh nhưng thêm PIN tại kiosk | Tăng thời gian qua cổng, vi phạm ngân sách 2 giây ở giờ giao ca; PIN cũng bị lộ |
| TOTP dùng chung bí mật đối xứng giữa server và app | Bí mật nằm ở cả hai đầu; lộ DB là lộ toàn bộ; không có chữ ký để phủ nhận |
| Server ký token, app chỉ hiển thị (M1) | Không sinh được QR khi mất mạng (L2); không ràng buộc thiết bị (L3) |
| Sinh trắc học tại cổng | Ngoài phạm vi (mục 1.4 M1), rủi ro PII cao |
| Token ký bằng khóa thiết bị + replay nhiều lớp + quy tắc hành vi | **Chọn**: giữ định dạng M1, đóng cả bốn lỗ hổng, không thêm bước cho nhân viên |

## Hệ quả (Consequences)

(+) Chụp màn hình QR vô dụng sau 30 giây; chuyển tiếp email vô dụng vì email không chứa QR.
(+) Đánh cắp phiên đăng nhập không đủ để tạo QR, cần có khóa riêng trong thiết bị.
(+) Nhân viên vẫn lấy được QR khi điện thoại mất mạng.
(+) Replay bị chặn cả khi kiosk offline, và bị phát hiện sau khi đồng bộ.
(–) Thêm Device Key Registry và luồng đăng ký thiết bị; HR phải xử lý thêm thao tác thu hồi thiết bị.
(–) Phụ thuộc đồng hồ điện thoại; cần `clock_skew` và đồng bộ thời gian.
(–) Nhân viên đổi điện thoại phải đăng ký lại; cần quy trình hỗ trợ.
(–) Bỏ QR qua email làm giảm tiện lợi cho nhân viên chưa cài app; đường lùi là UC-06.

## Kiểm chứng tuân thủ (Compliance)

Các test bắt buộc trong Task 2:

1. Token ký bằng khóa thiết bị đã đăng ký → chấp nhận; token ký bằng khóa khác → `INVALID_SIGNATURE`.
2. Token của thiết bị đã bị thu hồi → `DEVICE_REVOKED` (`QR_REVOKED` dành cho QR bị thu hồi theo nhân viên, không dùng cho thiết bị).
3. Cùng `jti` gửi hai lần khi online → lần hai `QR_ALREADY_USED`.
4. Cùng `jti` quét hai lần ở kiosk offline → lần hai bị kiosk từ chối nhờ replay cache cục bộ.
5. Cùng `jti` trong batch offline của hai kiosk → bản ghi thứ hai gắn cờ `TOKEN_REUSED_OFFLINE`.
6. `exp` quá hạn hơn `clock_skew` → `QR_EXPIRED`; trong ngưỡng skew → chấp nhận.
7. Payload chứa trường PII (ví dụ `name`) → bị từ chối ở bước kiểm tra schema (bảo vệ ADR-09).
8. Quy tắc R4: hai lượt vào ở BD và LA cách nhau 10 phút → sinh cảnh báo và cờ anomaly.

## Vị trí trong kiến trúc

| View | Thay đổi |
| --- | --- |
| Module view | Domain: `QrToken` (value object, parse và kiểm tra schema), `TokenVerificationPolicy`; port `IDeviceKeyRegistry`, `ITokenReplayStore`, `IClock` |
| C&C view | User/QR Service thêm Device Key Registry và API `POST /self-service/devices`, `DELETE /self-service/devices/{id}`, `POST /hr/employees/{id}/devices/revoke`; `sync-bundle` thêm `device_keys_delta`; Kiosk App thêm Replay Cache; Reporting/Audit consumer thêm Fraud Rule Engine |
| Deployment view | Khóa riêng trong keystore điện thoại; Device Key Registry trong DB của User/QR Service tại Central; bản sao khóa công khai tại Edge Node và kiosk; Redis Edge giữ `jti` |
| API | `POST /kiosk/v1/scans` thêm `device_id` (lấy từ `kid`); thêm `reason_code`: `DEVICE_REVOKED`, `DEVICE_UNKNOWN`, `CLOCK_SKEW_EXCEEDED` |
| Use case | UC-07 viết lại: thêm bước đăng ký thiết bị, bỏ luồng 1.1, sửa ngoại lệ 5.1; UC-02 thêm "thu hồi thiết bị"; UC-08 bỏ luồng 1.2 |

## Liên kết

AC-04, AC-05, AC-06 · ADR-08, ADR-09, ADR-11 · NFR-03, NFR-05 · FR-05, FR-06, FR-07, FR-19 · UC-02, UC-06, UC-07, UC-08
