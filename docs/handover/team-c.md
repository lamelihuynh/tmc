# Team C: User/QR và Identity

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này.

## 1. Nhiệm vụ của team

Team C giữ **lòng tin của hệ thống**: ai là ai (đăng nhập), điện thoại nào thuộc về ai (khóa thiết bị), và một mã QR có thật hay không (xác thực token).

| Sở hữu | Nội dung |
| --- | --- |
| `services/user-qr` (cổng 8083) | Đăng ký và thu hồi thiết bị, xác thực QR, gói đồng bộ khóa cho kiosk |
| `services/identity` (cổng 8087) | Đăng nhập, khóa tài khoản, MFA |
| `simulators/device-simulator` | Điện thoại giả lập dòng lệnh |
| `platform/security` | `Role`, `Principal`, `Rbac`, `PrincipalHeaders` (cả bốn lead duyệt khi đổi) |
| Hợp đồng | `docs/api/user-qr.openapi.yaml`; header danh tính trong `docs/conventions/api-design-guidelines.md` |
| Tài liệu | ADR-08, ADR-09, ADR-12, `docs/architecture/privacy-notes.md`, phần "QR tĩnh" của báo cáo |

Phối hợp với Team D trên `apps/employee-app`: app này hiện thực phía điện thoại của ADR-12 bằng WebCrypto, và phải khớp từng byte với những gì `user-qr` chấp nhận.

## 2. Bản đồ mã

### user-qr: `services/user-qr/src/main/java/vn/tmc/userqr/`

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | `Device`, `DeviceStatus` | Thiết bị và khóa công khai; mỗi nhân viên tối đa một thiết bị `ACTIVE` |
| | `QrClaims` | Sáu claim được phép: `sub`, `jti`, `iat`, `exp`, `fac`, `nonce` |
| | **`TokenVerificationPolicy`** | Quy tắc thời gian: sống tối đa 60 giây, lệch đồng hồ tối đa 60 giây |
| `application.usecase` | **`VerifyQrTokenUseCase`** | Thứ tự kiểm: định dạng, thiết bị, trạng thái, chủ sở hữu, chữ ký, thời gian |
| | `DeviceManagementService` | Đăng ký, tự thu hồi, HR thu hồi; có kiểm quyền và audit |
| | `BuildSyncBundleUseCase` | Khóa và thu hồi thay đổi sau một mốc thời gian |
| `application.port.out` | `ITokenCodec`, `IDeviceKeyRegistry`, `IAuditLog` | |
| `adapter.in.rest` | `DeviceController`, `QrVerifyController` | `/self-service/v1/devices`, `/hr/v1/...`, `/internal/v1/qr/verify`, `/internal/v1/sync-bundle` |
| `adapter.out.crypto` | `NimbusTokenCodec` | Đọc JWS, chỉ nhận ES256, từ chối claim lạ |
| `adapter.out.inmem` | `InMemoryDeviceKeyRegistry`, `LoggingAuditLog` | Test và profile `local` |
| `adapter.out.persistence` | `JdbcDeviceKeyRegistry` | PostgreSQL, **chưa kiểm chứng** |

### identity: `services/identity/src/main/java/vn/tmc/identity/`

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | `Account` | Khóa 15 phút sau 5 lần sai; `HR_STAFF` và `SYSTEM_ADMIN` bắt buộc MFA |
| `application.usecase` | `AuthenticateUseCase` | Luồng đăng nhập; không tiết lộ tài khoản có tồn tại hay không |
| `application.port.out` | `IAccountRepository`, `IPasswordVerifier`, `IMfaVerifier` | |
| `adapter.in.rest` | `AuthController` | `POST /auth/v1/login` |
| `adapter.out.inmem` | `InMemoryAccountRepository`, `Pbkdf2PasswordVerifier` | Chỉ lưu băm PBKDF2 có muối |
| `bootstrap` | `IdentityConfig` | Tài khoản mẫu: `supervisor1`, `admin1`, `nv001` đến `nv005` |

**Hiện trạng đăng nhập:** `AuthController` trả `status`, và khi thành công trả thêm `subject` cùng `roles` ở dạng JSON không ký. Đây là giải pháp tạm để app nhân viên chạy được; C-04 thay nó bằng JWT.

## 3. Chạy và kiểm thử phần của team

```bash
./gradlew :services:user-qr:test :services:identity:test :simulators:device-simulator:test :platform:security:test
./gradlew :services:user-qr:test --tests '*Adr12TokenVerificationTest'
./gradlew :services:user-qr:integrationTest      # cần Docker, sau khi C-01 thêm vào
```

| Lớp test | Kiểm gì |
| --- | --- |
| `Adr12TokenVerificationTest` | Chữ ký, thiết bị lạ, thiết bị bị thu hồi, hết hạn, lệch đồng hồ, token 24 giờ, token chứa tên |
| `SyncBundleTest` | Gói đồng bộ chứa khóa mới và thu hồi sau mốc |
| `DeviceAndVerifyApiTest` | API qua MockMvc |
| `AuthenticationTest` | Khóa tài khoản, MFA, băm mật khẩu; **4 test đang `@Disabled` chờ team bật** |
| `TokenSignerTest` | Token của `device-simulator` |
| `PrincipalHeadersTest`, `RbacTest` | Thư viện phân quyền |

`PhoneSimulator` (trong `services/user-qr/src/test/.../support`) là điện thoại giả cho test: có cặp khóa và ký token như app thật.

Thử tay:

```bash
# đăng nhập
curl -s -X POST http://localhost:8087/auth/v1/login -H 'Content-Type: application/json' \
  -d '{"username":"nv001","password":"dev-only-change-me"}'

# điện thoại giả lập
D=simulators/device-simulator/build/install/device-sim/bin/device-sim
$D keygen --name p1 --employee 0192a1c0-0000-7000-8000-000000000001 --home /tmp/phones
$D register --name p1 --home /tmp/phones --user-qr http://localhost:8083
$D token --name p1 --home /tmp/phones                 # thêm --long-lived hoặc --pii để thử từ chối
```

Kiểm tra phía trình duyệt: `cd apps/employee-app && npm test` chạy `token.test.ts`, kiểm token do WebCrypto ký có đúng định dạng JWS ES256.

## 4. Việc phải làm

**Mọi hạn là 23:00 của ngày được ghi**, tính theo thời điểm PR hợp nhất vào `main`. Khi một story ghi khoảng ngày (ví dụ "16 đến 17/11"), hạn là 23:00 của ngày cuối. Toàn cảnh lịch: [03-gantt.md](03-gantt.md).

### Hợp đồng JWT (12 đến 13/11, C1). **Cả ba team còn lại đang chờ.**

Trước khi viết mã C-04, chốt và công bố hợp đồng để các team khác chuẩn bị:

- Thuật toán ES256; claim: `sub` (UUID), `roles`, `scope` (nhà máy, cổng), `iat`, `exp`, `jti`.
- Thời hạn token và cách hết hạn sau 15 phút không hoạt động.
- Đường dẫn `/.well-known/jwks.json`; cách xoay khóa.
- Dạng mới của câu trả lời `POST /auth/v1/login`.
- Một cặp khóa và một token mẫu dùng cho test của team khác.

**Nộp ở:** một tệp OpenAPI cho identity trong `docs/api/` và cập nhật `docs/conventions/api-design-guidelines.md`. **Hạn: 23:00 ngày 13/11.**

### Hợp đồng `employees` trong gói đồng bộ (13/11, C2). **Team A đang chờ.**

Thêm vào `user-qr.openapi.yaml` trường `employees` của `GET /internal/v1/sync-bundle`: mỗi phần tử có `employeeUuid`, `active`, `factories`, `updatedAt`. Chỉ có định danh và quyền, không có họ tên (ADR-09). **Hạn: 23:00 ngày 13/11.**

### C-10. Mô hình đe dọa và ghi chú riêng tư (bắt buộc, M, 12 đến 20/11, C2 làm xen kẽ)

- **Việc:** bảng STRIDE ngắn cho luồng QR: chụp màn hình, chuyển tiếp ảnh, mất điện thoại, giả mạo thiết bị, lệch đồng hồ, kiosk bị can thiệp. Mỗi mối đe dọa ghi biện pháp và **tên test** chứng minh.
- **Sửa ở:** `docs/architecture/privacy-notes.md` và phần "QR tĩnh" của báo cáo.
- **Xong khi:** mỗi dòng trỏ được tới một test hoặc một chế độ kiểm thử trong app nhân viên (runbook mục 5.3).

### C-04. Cấp JWT (bắt buộc, L, 13 đến 18/11, C1). **Team D đang chờ.**

- **Sửa ở:** `AuthenticateUseCase` và `AuthController` trả token khi `GRANTED`; port ra mới để ký token (ví dụ `ITokenIssuer`) với adapter dùng Nimbus; controller mới cho `/.well-known/jwks.json`.
- **Xong khi:** bật được hai test `@Disabled`: `should_issue_a_signed_token_on_successful_login` và `should_expire_an_idle_session`; token hết hạn bị từ chối; JWKS trả khóa công khai và đổi được khóa mà token cũ vẫn kiểm được trong thời gian chuyển tiếp.
- **Giữ tương thích:** trong đợt này giữ nguyên các trường `subject` và `roles` trong câu trả lời đăng nhập, thêm trường `token`. App nhân viên và portal chuyển sang JWT ở D-13; bỏ trường cũ là việc sau.
- **Khóa ký:** lấy từ cấu hình hoặc sinh lúc khởi động cho môi trường phát triển. Không đưa khóa riêng vào repo.

### C-01. Kiểm chứng `JdbcDeviceKeyRegistry` (bắt buộc, M, 16 đến 17/11, C2)

- **Sửa ở:** thêm `src/test/.../adapter/DeviceRegistryIT.java` gắn thẻ `@Tag("integration")`, dùng Testcontainers PostgreSQL và chạy migration `V1__device_registry.sql`.
- **Xong khi** có test chứng minh: lưu rồi đọc lại khóa công khai giống bản gốc; `changedSince` đúng thứ tự; **hai lệnh đăng ký song song cho một nhân viên thì chỉ mục duy nhất `uq_one_active_device_per_employee` từ chối một bên**.
- **Điểm cần xử lý:** `DeviceManagementService.register` thu hồi thiết bị cũ rồi lưu thiết bị mới bằng hai lệnh riêng. Với PostgreSQL, hai lệnh này cần nằm trong một transaction, và lỗi vi phạm chỉ mục phải thành câu trả lời có nghĩa (409), không phải 500.

### C-02. Nhân viên đủ quyền trong gói đồng bộ (bắt buộc, L, 17 đến 19/11, C2). **Team A đang chờ.**

Gate-station hiện đọc danh sách nhân viên từ `application.yml`. Nguồn đúng phải là user-qr.

- **Sửa ở:** thực thể mới trong `domain` (ví dụ `EmployeeEligibility`: UUID, `active`, `factories`, `updatedAt`), port ra và hai adapter (bộ nhớ, JDBC kèm migration `V2`), mở rộng `ISyncBundle.Bundle` và `BuildSyncBundleUseCase`. Dữ liệu mẫu trùng với 5 nhân viên trong `application.yml` của gate-station.
- **Xong khi:** `GET /internal/v1/sync-bundle?since=...` trả `employees` thay đổi sau mốc; vô hiệu hóa một nhân viên thì thiết bị của người đó bị thu hồi và lần đồng bộ sau trả `active: false`.
- **Phạm vi:** không làm đầy đủ vòng đời nhân viên (C-03 đã cắt). Chỉ cần một endpoint cho HR đổi `active` và `factories`, có kiểm quyền và audit.

### C-06. Xác thực JWT ở service (bắt buộc, M, 19 đến 20/11, C1). Bản thu nhỏ.

- **Sửa ở:** `platform/security`: bộ xác thực nhận token và JWKS, trả `Principal`. Áp dụng cho `user-qr` qua `HeaderPrincipalResolver`: có `Authorization: Bearer` thì xác thực token; không có thì, **chỉ ở profile phát triển**, vẫn đọc header `X-Subject`.
- **Xong khi:** gọi `/self-service/v1/devices` với token hợp lệ thì được; token sai chữ ký hoặc hết hạn thì 401; ở profile mặc định, header `X-Subject` giả bị bỏ qua.
- **Phạm vi:** chỉ `user-qr`. Các service khác giữ header, ghi vào phần hạn chế của báo cáo.
- **Lưu ý:** `platform/security` là mã dùng chung, PR cần cả bốn lead duyệt. Không kéo Spring vào thư viện này.

### C-05. TOTP cho MFA (bắt buộc, M, 20/11, C2). Cắt thứ hai nếu trễ.

- **Sửa ở:** adapter mới hiện thực `IMfaVerifier` theo RFC 6238, thay cho bản luôn từ chối trong `IdentityConfig`.
- **Xong khi:** test mã đúng, mã sai, mã của bước thời gian liền trước và liền sau; mã đã dùng không dùng lại được.

### C-08. Audit qua outbox (bắt buộc, M, 23/11, C1). Cắt đầu tiên nếu trễ.

- **Phụ thuộc:** H8 (B-02, 18/11).
- **Việc:** thay `LoggingAuditLog` của user-qr và identity bằng adapter ghi sự kiện `AuditRecorded` vào outbox rồi phát lên broker, để `audit-admin` nhận được.
- **Xong khi:** đăng ký và thu hồi thiết bị xuất hiện trong `GET /admin/v1/audit`; broker tắt thì không mất.

### D-13 (phối hợp với Team D, 23/11, C2)

Chuyển `apps/employee-app` sang JWT: lưu token sau khi đăng nhập, gửi `Authorization: Bearer` khi đăng ký thiết bị. Team D sửa app, Team C kiểm phía server. Chỉ làm khi C-04 và C-06 đã xong.

## 5. Team C cần gì và nợ gì

| Chiều | Hạn (23:00) | Nội dung |
| --- | --- | --- |
| Giao cho A, B, D | 13/11 | Hợp đồng JWT |
| Giao cho A | 13/11, 19/11 | Hợp đồng rồi dữ liệu `employees` trong sync bundle |
| Giao cho D | 18/11 | `identity` cấp JWT và có JWKS |
| Giao cho D | 20/11 | Bản nháp báo cáo |
| Giao cho A | luôn luôn | `POST /internal/v1/qr/verify` giữ nguyên tên trường (`valid`, `reason`, `employeeUuid`, `jti`, `expiresAt`, `factories`) |
| Giao cho D | luôn luôn | `POST /self-service/v1/devices` và định dạng token giữ nguyên; `kiosk-simulator`, `device-simulator` và app nhân viên phụ thuộc vào chúng |
| Nhận từ B | 18/11 | Outbox phát được lên RabbitMQ (cho C-08) |
| Nhận từ D | 12/11, 16/11 | Compose chạy được; CI |

**Đổi quy tắc xác thực token thì phải đổi ở ba nơi cùng lúc:** `VerifyQrTokenUseCase` (online), bộ xác thực offline của `kiosk-simulator` (Team D), và phía ký trong `device-simulator` lẫn `apps/employee-app`. Ba nơi lệch nhau thì token hợp lệ khi có mạng nhưng bị từ chối khi mất mạng, hoặc ngược lại.

## 6. Bẫy cần tránh

- **`user-qr` chỉ đọc, không đánh dấu token đã dùng.** Việc dùng một lần thuộc về `ITokenReservation` của gate-station, vì khi cổng lỗi thì token phải nhả được. Đừng thêm kho chống dùng lại vào user-qr.
- **Từ chối theo mặc định.** Mọi lỗi bất ngờ khi xác thực phải thành "từ chối", không bao giờ thành "hợp lệ".
- **Không dữ liệu cá nhân trong token, sự kiện, hay log.** Chỉ dùng UUID. `NimbusTokenCodec` từ chối mọi claim ngoài sáu claim cho phép; giữ nguyên điều đó.
- **Đăng nhập không tiết lộ tài khoản có tồn tại hay không.** Sai tên và sai mật khẩu trả cùng một câu trả lời; khi từ chối, câu trả lời không chứa `subject` hay `roles`.
- **Khóa riêng không rời điện thoại và không vào repo.** Server chỉ giữ khóa công khai.
- **Header `X-Subject` không phải bảo mật.** Bất kỳ ai cũng tự đặt được. Trong báo cáo, mô tả nó là cơ chế phát triển tạm thời.

## 7. Phần báo cáo của Team C

Nộp bản nháp ngày 20/11 vào `docs/report/`:

- **Issues Encountered: QR tĩnh.** Mối đe dọa của QR tĩnh, lời giải QR động ký bằng khóa thiết bị (ADR-12), bốn lớp chống dùng lại và lớp nào đã có, đánh đổi (cần điện thoại, cần đồng bộ giờ, một thiết bị mỗi người), bằng chứng: tên test trong `Adr12TokenVerificationTest`, `demo-online`, và các chế độ kiểm thử của app nhân viên.
- Bảng mô hình đe dọa của C-10.
- Cập nhật NFR-05 và UC-07 theo `docs/adr/README.md` mục 3.
- Sơ đồ bảo mật: đăng ký thiết bị, ký token, xác thực.
- Áp dụng SOLID trên mã của team, ví dụ: `ITokenCodec` tách thư viện JOSE khỏi use case, `IMfaVerifier` thay được bản giả bằng TOTP mà không sửa `AuthenticateUseCase`, `TokenVerificationPolicy` là quy tắc thuần không phụ thuộc hạ tầng.
- Một đoạn phản ánh.
