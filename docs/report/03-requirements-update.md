# Cập nhật Yêu cầu, Use Case và Đặc tính Kiến trúc (Task 1.1)

> Phần này trả lời yêu cầu **Task 1.1** của đề bài M2: cập nhật các yêu cầu chức năng, phi chức năng, use case và fitness function bị ảnh hưởng bởi các quyết định kiến trúc mới (ADR-11, ADR-12, ADR-13). Tài liệu này hiện chứa đầy đủ phần **Team C** (`FR-19`, `NFR-05`, `UC-07`, `AC-04`, kèm liên đới `NFR-03`); phần **Team B** (`NFR-09`, `UC-08`, `AC-03`) để giữ chỗ để Team D hợp nhất tại D-12.

---

## 1. Phần Team C: FR-19, NFR-05 (NFR-03), UC-07, AC-04

Căn cứ đối chiếu: `docs/document.md` (M1 mục 4.2.3, 4.3, 5.7, 6.1) và `docs/adr/README.md` mục 3.

### 1.1. Cập nhật Yêu cầu chức năng & phi chức năng

| ID | Mục M1 | Bản M1 hiện tại | Bản cập nhật cho M2 (ADR-12, ADR-08, ADR-09) |
| --- | --- | --- | --- |
| **FR-19** | 4.2.3 | Hệ thống tự động sinh mã QR định danh cho nhân viên trên mobile app/web portal. Mã QR là chuỗi token ngẫu nhiên (30–60 giây) và chỉ có giá trị sử dụng một lần. | **Loại bỏ hoàn toàn QR tĩnh.** Ứng dụng nhân viên (`employee-app` / `device-simulator`) đăng ký cặp khóa bất đối xứng ECDSA P-256 cho thiết bị (mỗi nhân viên tối đa **1 thiết bị `ACTIVE`**, khóa riêng không rời thiết bị) và **tự sinh, tự ký mã QR động** (compact JWS `ES256`, `kid = device_id`, hiệu lực mặc định 30 giây, tối đa 60 giây, chỉ dùng một lần) ngay cả khi điện thoại mất mạng. Email không còn được dùng để gửi QR mở cổng. |
| **NFR-05** | 4.3 | Mã QR không chứa thông tin cá nhân đọc được, là token ngẫu nhiên không đoán được; 100% QR hết hạn, đã dùng hoặc bị thu hồi bị từ chối trong ngân sách 2 giây. | Bổ sung 3 tiêu chí bắt buộc từ ADR-12: **(1) Ràng buộc thiết bị:** token phải do thiết bị `ACTIVE` đã đăng ký ký và `sub` phải khớp chủ sở hữu thiết bị (`VerifyQrTokenUseCase`); **(2) Cấm claim PII & giới hạn tuổi thọ:** từ chối ngay (`MALFORMED_TOKEN`) mọi token có claim ngoài `{sub, jti, iat, exp, fac, nonce}` hoặc `exp - iat > 60s` (`TokenVerificationPolicy`, `NimbusTokenCodec`); **(3) Chống replay kể cả khi kiosk offline:** 100% token dùng lại bị từ chối khi online (Redis `SET NX`, R1) và tại cùng kiosk offline (replay cache cục bộ, R2), và bị gắn cờ `TOKEN_REUSED_OFFLINE` khi đồng bộ từ nhiều kiosk (R3). |
| **NFR-03** *(bổ sung trạng thái M2)* | 4.3 | Bắt buộc MFA cho System Administrator và HR Staff; khóa tạm 15 phút sau 5 lần sai; hết hạn phiên sau 15 phút không hoạt động. | Bổ sung cơ chế thật trong `identity`: cấp JWT `ES256` sống 15 phút kèm JWKS hỗ trợ xoay khóa (`SigningKeys`, C-12); xác thực MFA bằng **TOTP RFC 6238** có chống dùng lại mã cho `HR_STAFF` và `SYSTEM_ADMIN` (`TotpMfaVerifier`, C-05); audit mọi lần đăng nhập/khóa qua outbox (C-08, C-11). **Ghi rõ hạn chế:** hết hạn phiên theo không hoạt động và thu hồi phiên khi đổi vai trò chưa hiện thực (`AuthenticationTest` còn 2 test `@Disabled`). |

### 1.2. Cập nhật Use Case UC-07: Đăng ký thiết bị và sinh QR động ký bằng khóa thiết bị

Thay thế bảng mục **5.7 UC-07** trong `docs/document.md`:

| Trường | Nội dung cập nhật M2 |
| --- | --- |
| **Use-case ID** | UC-07 |
| **Use-case name** | Đăng ký thiết bị và sinh QR động ký bằng khóa thiết bị |
| **Actor** | Employee (phụ: HR Staff khi hỗ trợ thu hồi / đổi trạng thái theo UC-02) |
| **Description** | Nhân viên đăng ký khóa công khai của điện thoại vào Device Key Registry (mỗi người tối đa 1 thiết bị `ACTIVE`). Sau khi đăng ký, ứng dụng tự sinh và tự ký mã QR động ngắn hạn (30 giây) bằng khóa riêng lưu trên máy để chống chụp màn hình, chuyển tiếp và vẫn dùng được khi điện thoại không có mạng. |
| **Trigger** | Nhân viên mở `employee-app` để lấy QR hoặc đăng ký thiết bị mới. |
| **Precondition** | Nhân viên có tài khoản hợp lệ trong `identity` (JWT Bearer hoặc dev profile), trạng thái nhân sự `ACTIVE`. |
| **Postcondition** | Thiết bị mới được lưu `ACTIVE` trong `user-qr` (thiết bị cũ nếu có tự động chuyển `REVOKED`), kèm sự kiện `AuditRecorded`; ứng dụng hiển thị mã QR tự xoay vòng mỗi 20 giây (hiệu lực mỗi token 30 giây). |
| **Normal flow** | 1. Nhân viên đăng nhập qua `identity` (`POST /auth/v1/login`) và nhận access token JWT `ES256`.<br/>2. Nếu chưa có khóa trên máy: app sinh cặp khóa EC P-256 trong vùng lưu không thể xuất khóa riêng (`extractable = false` / Keystore), gửi khóa công khai tới `POST /self-service/v1/devices`.<br/>3. `user-qr` kiểm tra JWT & vai trò `EMPLOYEE`, kiểm tra `caller.subject == employeeUuid`, thu hồi thiết bị `ACTIVE` cũ (nếu có) và lưu thiết bị mới + audit trong một transaction (`DeviceManagementService`), trả `201 { deviceId }`.<br/>4. Khi hiển thị QR (kể cả khi điện thoại mất mạng), app tự đóng gói JWS `ES256` (`kid = deviceId`, claims `{sub, jti, iat, exp = iat + 30s, fac, nonce}`) và ký bằng khóa riêng trên máy.<br/>5. App tự tạo token mới mỗi 20 giây (nhỏ hơn thời hạn 30 giây); khi quét tại kiosk, `gate-station` và `user-qr` xác thực chữ ký, thiết bị, thời hạn và giữ chỗ `jti` dùng một lần. |
| **Alternative flows** | - **Đã xóa luồng 1.1 cũ (gửi QR tĩnh qua email có hạn theo ngày).**<br/>- **3.1 Đăng ký thay thế thiết bị (đổi điện thoại):** bước 3 tự động thu hồi thiết bị `ACTIVE` trước đó (`DEVICE_REVOKED`).<br/>- **3.2 Thu hồi chủ động:** nhân viên gọi `DELETE /self-service/v1/devices/{deviceId}`, hoặc HR gọi `POST /hr/v1/employees/{employeeUuid}/devices/revoke`, hoặc HR chuyển nhân viên sang `INACTIVE` qua `PUT /hr/v1/employees/{employeeUuid}/eligibility` (tự động thu hồi thiết bị).<br/>- **1.2 Không có điện thoại dùng được tại cổng:** đi theo **UC-06** (bảo vệ xác minh và mở cổng thủ công có audit). |
| **Exception flows** | - **3a. Hai yêu cầu đăng ký thiết bị chạy song song cho cùng nhân viên:** một yêu cầu thắng `201`, yêu cầu còn lại bị partial unique index chặn, rollback và trả `409 Conflict` (`CONCURRENT_DEVICE_REGISTRATION`) kèm audit `CONFLICT`.<br/>- **3b. Khóa công khai không hợp lệ:** trả `400 Bad Request`.<br/>- **4.1 (Sửa từ 5.1 cũ) Điện thoại mất mạng khi mở app lấy QR:** app **vẫn tự sinh và ký được QR bình thường** bằng khóa riêng đã lưu cục bộ (chỉ bước đăng ký thiết bị lần đầu mới cần kết nối `user-qr`).<br/>- **5.1 Token bị từ chối khi quét tại cổng:** trả mã lý do tường minh không lộ PII (`QR_EXPIRED`, `QR_ALREADY_USED`, `DEVICE_REVOKED`, `DEVICE_UNKNOWN`, `INVALID_SIGNATURE`, `CLOCK_SKEW_EXCEEDED`, `MALFORMED_TOKEN`). |

### 1.3. Cập nhật Đặc tính Kiến trúc AC-04 (mục 6.1 M1)

| Mã | Bổ sung Fitness Function trong M2 | Bằng chứng tự động trong mã |
| --- | --- | --- |
| **AC-04 (Security — AuthN/AuthZ & Chống giả mạo QR)** | - 100% API tài nguyên kiểm tra chữ ký JWT `ES256` qua JWKS (profile mặc định tắt header dev) và áp dụng `Rbac.requireRole` / `DataScope` ở use case.<br/>- 100% tài khoản `HR_STAFF`, `SYSTEM_ADMIN` bắt buộc qua TOTP RFC 6238 chống dùng lại mã.<br/>- 100% token quá hạn vượt skew (`> exp + 60s`), `exp - iat > 60s`, chứa PII, sai chữ ký, khác chủ thiết bị, hoặc thiết bị đã thu hồi bị từ chối.<br/>- Tuân thủ chính sách hợp đồng chung `qr-token-policy.json` trên cả producer và verifier. | `BearerTokenApiTest`, `JwtPrincipalVerifierTest`, `TotpMfaVerifierTest`, `TotpLoginApiTest`, `SigningKeyRotationTest`, `Adr12TokenVerificationTest`, `QrTokenPolicyContractTest`, `EmployeeEligibilityApiTest`, `ConcurrentDeviceRegistrationIT`. |

---

## 2. Phần Team B *(chờ ghép khi hợp nhất D-12)*

- `NFR-09`, `UC-08`, `AC-03` được giữ phần trong tệp `01-issues-connectivity.md` / đóng góp của Team B và sẽ hợp nhất vào mục này ở bước **D-12**.
