# Team C: User/QR và Identity

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này. Lịch chung: [01-ke-hoach.md](01-ke-hoach.md).

> **Bạn có thể bắt đầu ngay (16/11)**
>
> **Đã có sẵn cho Team C** (bước gỡ phụ thuộc, nhánh `chore/x/decouple-teams`):
>
> - C-04 xong: identity cấp JWT ES256 15 phút (`LoginUseCase`, `NimbusAccessTokenIssuer`), `GET /.well-known/jwks.json` (`JwksController`); test `should_issue_a_signed_token_on_successful_login` đã bật.
> - C-06 (bản thu nhỏ) xong: `platform/security` có `JwtPrincipalVerifier`, `PrincipalResolver`; user-qr nhận Bearer (`UserQrConfig.principalResolver`, `BearerTokenApiTest`); header dev chỉ còn ở `local`, `inmem`.
> - `employees` trong gói đồng bộ: `IEmployeeDirectory` + `InMemoryEmployeeDirectory` (nạp từ `tmc.qr.employees`); C-02 chỉ còn thay adapter.
> - Outbox sẵn cho audit (C-08): user-qr có bảng `outbox_events` (Flyway `V2__outbox_events.sql`) và bean `AuditOutbox`; identity có `AuditOutbox` trên outbox **trong bộ nhớ** (`OutboxConfig`); relay 2 giây, publisher RabbitMQ.
> - `UserQrPersistenceIT` (3 test) xanh; contract test `QrTokenPolicyContractTest`, `EmployeeSeedContractTest`.
>
> **Lệnh:**
>
> ```bash
> ./gradlew :services:user-qr:test :services:identity:test :simulators:device-simulator:test :platform:security:test
> ./gradlew :services:user-qr:integrationTest      # cần Docker đang chạy (Testcontainers)
> make up                                          # hạ tầng thật (PostgreSQL central đã có database user_qr và identity)
> ```
>
> **Code theo:** `docs/api/user-qr.openapi.yaml`, `docs/api/identity.openapi.yaml`, `docs/api/attendance-events.asyncapi.yaml` (`AuditRecorded`, khóa `user-qr.AuditRecorded`, `identity.AuditRecorded`), `docs/api/fixtures/employee-seed.json`, `docs/api/fixtures/qr-token-policy.json`; trong test: `TestJwtKeys`, `TestTokens`, `PhoneSimulator`, `TmcContainers`.
>
> **Không chờ team nào.** Phần cần mã thật của team khác là kiểm tích hợp **tùy chọn** ở mục 5.

## 1. Nhiệm vụ của team

Team C giữ **lòng tin của hệ thống**: ai là ai (đăng nhập, JWT), điện thoại nào thuộc về ai (khóa thiết bị), một mã QR có thật hay không, và ai còn được vào nhà máy (danh bạ nhân viên trong gói đồng bộ).

| Sở hữu | Nội dung |
| --- | --- |
| `services/user-qr` (cổng 8083) | Đăng ký và thu hồi thiết bị, xác thực QR, gói đồng bộ (khóa, thu hồi, nhân viên) |
| `services/identity` (cổng 8087) | Đăng nhập, khóa tài khoản, MFA, cấp JWT, JWKS |
| `simulators/device-simulator` | Điện thoại giả lập dòng lệnh |
| `platform/security` | `Role`, `Principal`, `Rbac`, `PrincipalHeaders`, `PrincipalResolver`, `jwt/*` (cả bốn lead duyệt khi đổi) |
| Hợp đồng | `docs/api/user-qr.openapi.yaml`, `docs/api/identity.openapi.yaml` |
| Tài liệu | ADR-08, ADR-09, ADR-12, `docs/architecture/privacy-notes.md`, phần "QR tĩnh" của báo cáo |

Hai người: **C1** (lead) giữ identity và audit; **C2** giữ user-qr, MFA và mô hình đe dọa.

## 2. Bản đồ mã

### user-qr: `services/user-qr/src/main/java/vn/tmc/userqr/`

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | `Device`, `QrClaims`, **`TokenVerificationPolicy`** | Thiết bị; sáu claim được phép; sống tối đa 60 giây, lệch đồng hồ tối đa 60 giây |
| `application.usecase` | **`VerifyQrTokenUseCase`**, `DeviceManagementService`, `BuildSyncBundleUseCase` | Xác thực; đăng ký và thu hồi; gói đồng bộ |
| `application.port.out` | `ITokenCodec`, `IDeviceKeyRegistry`, `IEmployeeDirectory`, `IAuditLog` | |
| `adapter.out.inmem` | `InMemoryDeviceKeyRegistry`, `InMemoryEmployeeDirectory`, `LoggingAuditLog` | Test và profile `local` |
| `adapter.out.persistence` | `JdbcDeviceKeyRegistry` | PostgreSQL, có `UserQrPersistenceIT` |
| `bootstrap` | `UserQrConfig`, `OutboxConfig` | Nối adapter, resolver JWT, outbox |

### identity: `services/identity/src/main/java/vn/tmc/identity/`

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | `Account` | Khóa 15 phút sau 5 lần sai; `HR_STAFF`, `SYSTEM_ADMIN` bắt buộc MFA |
| `application.usecase` | `AuthenticateUseCase`, `LoginUseCase` | Đăng nhập; cấp token |
| `application.port.out` | `IAccountRepository`, `IPasswordVerifier`, `IMfaVerifier`, `IAccessTokenIssuer` | |
| `adapter.in.rest` | `AuthController`, `JwksController` | `POST /auth/v1/login`, `GET /.well-known/jwks.json` |
| `adapter.out` | `InMemoryAccountRepository`, `Pbkdf2PasswordVerifier`, `NimbusAccessTokenIssuer` | |
| `bootstrap` | `IdentityConfig`, `DevSeed`, `OutboxConfig` | Tài khoản mẫu `supervisor1`, `admin1`, `nv001` đến `nv005`; khóa ký `IDENTITY_JWT_PRIVATE_JWK` (trống thì tự sinh, chỉ dev) |

## 3. Việc phải làm (16/11 đến 24/11)

**Mọi hạn là 23:00 của ngày cuối**, tính theo lúc PR hợp nhất vào `main`. Mức: **B** bắt buộc, **N** nên có.

| Story | Mức | Người | Bắt đầu | Hạn |
| --- | --- | --- | --- | --- |
| C-08 Audit của user-qr và identity qua `AuditOutbox` | B | C1 | 16/11 | 17/11 |
| C-11 Database cho identity, outbox bền | N | C1 | 18/11 | 19/11 |
| C-12 Xoay khóa ký JWT | N | C1 | 20/11 | 20/11 |
| C-01 (phần còn lại) Hai đăng ký song song | B | C2 | 16/11 sáng | 16/11 |
| C-02 Nguồn HR cho danh bạ nhân viên | B | C2 | 16/11 chiều | 18/11 |
| C-05 TOTP cho MFA | B | C2 | 19/11 | 19/11 |
| C-10 Mô hình đe dọa và ghi chú riêng tư | B | C2 | 20/11 | 20/11 |
| R-C Phần báo cáo "QR tĩnh" | B | C2 (nháp cùng C-10), C1 và C2 hoàn thiện 23/11 | 20/11 (nháp) | 24/11 (bản cuối) |

Ngày 23/11: hoàn thiện báo cáo, tham gia kiểm tích hợp. Ngày 24/11: đóng băng mã lúc 23:00, chỉ sửa lỗi; tổng duyệt demo.

### C-08. Audit của user-qr và identity qua `AuditOutbox` (B, M, C1, 16 đến 17/11)

| | |
| --- | --- |
| Mục tiêu | Đăng ký, thu hồi thiết bị và đăng nhập (thành công, thất bại, khóa tài khoản) thành sự kiện `AuditRecorded` trên `tmc.events`, không chỉ ghi log |
| Sửa ở | user-qr: adapter `IAuditLog` bọc `AuditOutbox` (thay `LoggingAuditLog` ở profile mặc định), bọc thay đổi thiết bị và dòng outbox trong cùng transaction. identity: port ra mới cho audit (identity chưa có `IAuditLog`), gọi từ `AuthenticateUseCase`, adapter bọc `AuditOutbox` |
| Code theo | `platform/messaging` (`AuditOutbox`, `AuditEvents`); `UserQrPersistenceIT.should_keep_audit_events_in_the_outbox_table_of_v2`; không chứa họ tên, token, mật khẩu (ADR-09) |
| Xong khi | Test user-qr (IT): đăng ký thiết bị ghi đúng một dòng outbox; lỗi giữa chừng thì không có thiết bị lẫn dòng outbox. Test identity: đăng nhập sai 5 lần sinh sự kiện khóa tài khoản, không chứa mật khẩu. `./gradlew :services:user-qr:integrationTest :services:identity:test` xanh |

### C-11. Database cho identity, outbox bền (N, M, C1, 18 đến 19/11)

| | |
| --- | --- |
| Mục tiêu | Sự kiện audit của identity không mất khi identity khởi động lại |
| Sửa ở | identity: datasource cho database `identity` (đã tạo trong `docker/init/central/01-databases.sql`), Flyway `V1__outbox_events.sql` (lấy từ `platform/messaging/src/main/resources/tmc-messaging/outbox_events.sql`), `JdbcOutboxStore` ở profile mặc định; giữ outbox bộ nhớ ở `local`, `inmem` |
| Code theo | Mẫu `services/user-qr/.../bootstrap/OutboxConfig.java`; `TmcContainers` |
| Xong khi | Test IT mới của identity: đăng nhập ghi dòng outbox vào PostgreSQL; dừng relay, tạo lại context, dòng vẫn `PENDING` và được phát sau đó. `./gradlew :services:identity:integrationTest` xanh |

### C-12. Xoay khóa ký JWT (N, S đến M, C1, 20/11)

| | |
| --- | --- |
| Mục tiêu | Đổi khóa ký mà token cũ còn hạn vẫn kiểm được (NFR-03, `identity.openapi.yaml` mục JWKS) |
| Sửa ở | identity: cấu hình khóa hiện tại và khóa trước (ví dụ `IDENTITY_JWT_PREVIOUS_PUBLIC_JWK`), JWKS trả cả hai `kid`; `platform/security` không cần đổi nếu verifier chọn khóa theo `kid` (kiểm trước) |
| Code theo | `TestJwtKeys`, `JwtPrincipalVerifier` |
| Xong khi | Test: token ký bằng khóa cũ còn hạn được chấp nhận sau khi đổi khóa; JWKS có hai khóa; bỏ khóa cũ thì token đó bị từ chối |

### C-01 (phần còn lại). Hai đăng ký song song (B, S, C2, 16/11 sáng)

| | |
| --- | --- |
| Mục tiêu | `UserQrPersistenceIT.should_refuse_a_second_active_device_for_the_same_employee` đã chứng minh chỉ mục duy nhất; còn hai luồng đăng ký **đồng thời** và câu trả lời có nghĩa |
| Sửa ở | `DeviceManagementService.register` (thu hồi cũ và lưu mới trong một transaction), ánh xạ lỗi vi phạm chỉ mục thành 409 |
| Code theo | `user-qr.openapi.yaml` (thêm mã 409 nếu chưa có) |
| Xong khi | Test IT: hai luồng cùng đăng ký cho một nhân viên, đúng một bên thắng, bên kia nhận 409, không còn hai thiết bị `ACTIVE` |

### C-02. Nguồn HR cho danh bạ nhân viên (B, L, C2, 16/11 chiều đến 18/11)

| | |
| --- | --- |
| Mục tiêu | `employees` trong gói đồng bộ lấy từ dữ liệu HR bền, không từ cấu hình `tmc.qr.employees` |
| Sửa ở | `IEmployeeDirectory` thêm thao tác ghi; adapter JDBC mới với migration `V3__employees.sql` trong `user_qr`; endpoint HR đổi `status` và `factories` (kiểm vai `HR_STAFF` bằng `PrincipalResolver`, ghi audit qua C-08); vô hiệu hóa nhân viên thì thu hồi thiết bị; nạp dữ liệu mẫu từ `docs/api/fixtures/employee-seed.json` |
| Code theo | `employees[]` trong `GET /internal/v1/sync-bundle` của `user-qr.openapi.yaml` (đã chốt: `employeeUuid`, `status`, `factories`, `updatedAt`); endpoint HR mới thêm vào cùng tệp trong cùng PR |
| Xong khi | `SyncBundleTest` mở rộng: đổi trạng thái thì lần đồng bộ sau (`since`) trả `status: INACTIVE`; thiết bị bị thu hồi; `EmployeeSeedContractTest` vẫn xanh; IT JDBC xanh. Test `@Disabled` `should_publish_employee_eligibility_for_the_edge` của identity: chuyển sang user-qr hoặc xóa kèm ghi chú trong PR (yêu cầu đã nằm ở user-qr) |
| Phạm vi | Không làm vòng đời nhân viên đầy đủ (C-03 bị cắt) |

### C-05. TOTP cho MFA (B, M, C2, 19/11)

| | |
| --- | --- |
| Mục tiêu | `HR_STAFF`, `SYSTEM_ADMIN` đăng nhập được bằng mã TOTP thật (RFC 6238) |
| Sửa ở | Adapter mới hiện thực `IMfaVerifier`, thay bản luôn từ chối trong `IdentityConfig`; bí mật TOTP của tài khoản mẫu chỉ cho dev |
| Code theo | `AuthenticationTest.should_require_mfa_for_hr_staff_and_system_admin` |
| Xong khi | Test: mã đúng, mã sai, mã của bước liền trước và liền sau, mã đã dùng không dùng lại được |

### C-10. Mô hình đe dọa và ghi chú riêng tư (B, M, C2, 20/11)

| | |
| --- | --- |
| Mục tiêu | Bảng STRIDE ngắn cho luồng QR và đăng nhập: chụp màn hình, chuyển tiếp ảnh, mất điện thoại, giả mạo thiết bị, lệch đồng hồ, kiosk bị can thiệp, token JWT bị lộ |
| Sửa ở | `docs/architecture/privacy-notes.md`, phần "QR tĩnh" trong `docs/report/` |
| Xong khi | Mỗi dòng trỏ tới một test có thật (ví dụ `Adr12TokenVerificationTest`, `QrTokenPolicyContractTest`, `BearerTokenApiTest`) hoặc một chế độ kiểm thử của app nhân viên ([02-chay-local.md](02-chay-local.md) mục 5.3) |

### R-C. Phần báo cáo "QR tĩnh" (nháp 20/11, bản cuối 24/11)

- **Issues Encountered: QR tĩnh.** Mối đe dọa, QR động ký bằng khóa thiết bị (ADR-12), bốn lớp chống dùng lại và lớp nào đã có, đánh đổi, bằng chứng (`Adr12TokenVerificationTest`, `demo-online`, chế độ kiểm thử của app nhân viên).
- Bảng mô hình đe dọa của C-10; cập nhật NFR-05 và UC-07 theo `docs/adr/README.md` mục 3; sơ đồ bảo mật (đăng ký thiết bị, ký token, xác thực, JWT).
- SOLID: `ITokenCodec`, `IMfaVerifier` (thay bản giả bằng TOTP không sửa `AuthenticateUseCase`), `TokenVerificationPolicy`. Một đoạn phản ánh.

## 4. Đã xong hoặc đã cắt

| Story | Trạng thái |
| --- | --- |
| C-04 Cấp JWT, JWKS | Xong (xem hộp đầu tài liệu) |
| C-06 Xác thực JWT ở service | Xong cho user-qr. Gate-station (A-11), attendance (B-06) và ba service trung tâm (D-13) tự gắn theo mẫu |
| Test `should_expire_an_idle_session` | Còn `@Disabled`: token sống 15 phút, chưa có làm mới theo hoạt động. Ghi vào phần hạn chế; làm nếu còn thời gian |
| C-03, C-07, C-09 | Cắt khỏi đợt này (`should_end_open_sessions_when_a_role_changes` vẫn `@Disabled`) |

## 5. Kiểm tích hợp tùy chọn

| Mã | Ngày | Cùng team | Kiểm gì |
| --- | --- | --- | --- |
| I-2 | 19/11 | A | Đổi trạng thái nhân viên ở user-qr, cổng từ chối sau tối đa 30 giây |
| I-4 | 23/11 | A, D | Đăng ký và thu hồi thiết bị xuất hiện trong `GET /admin/v1/audit` |
| I-5 | 23/11 | A, D | App nhân viên và portal dùng Bearer trên profile mặc định; JWKS được gate-station và service trung tâm đọc |

Không story nào ở trên chờ những mục này.

## 6. Hợp đồng Team C giữ ổn định

| Cho ai | Hợp đồng |
| --- | --- |
| Team A | `POST /internal/v1/qr/verify` giữ tên trường (`valid`, `reason`, `employeeUuid`, `jti`, `expiresAt`, `factories`); `employees` trong sync bundle giữ định dạng |
| Team D | `POST /self-service/v1/devices`, định dạng token QR (`qr-token-policy.json`), câu trả lời đăng nhập (`accessToken`, `subject`, `roles` vẫn giữ) |
| Mọi team | Claim JWT, `iss`, `aud`, đường dẫn JWKS trong `identity.openapi.yaml` |

**Đổi quy tắc xác thực token QR thì phải đổi ở ba nơi cùng lúc:** `VerifyQrTokenUseCase`, `OfflineVerifier` của `kiosk-simulator` (Team D), và phía ký trong `device-simulator` lẫn `apps/employee-app`. `QrTokenPolicyContractTest` đỏ là dấu hiệu lệch.

## 7. Bẫy cần tránh

- **`user-qr` chỉ đọc, không đánh dấu token đã dùng.** Việc dùng một lần thuộc về `ITokenReservation` của gate-station.
- **Từ chối theo mặc định.** Mọi lỗi bất ngờ khi xác thực phải thành "từ chối".
- **Không dữ liệu cá nhân trong token, sự kiện, log.** Chỉ dùng UUID; sự kiện audit chỉ có mã lý do.
- **Đăng nhập không tiết lộ tài khoản có tồn tại hay không.**
- **Khóa riêng không vào repo.** Khóa ký JWT lấy từ biến môi trường; khóa thiết bị không rời điện thoại.
- **Header `X-Subject` không phải bảo mật.** Chỉ được tin ở profile `local`, `inmem`.
