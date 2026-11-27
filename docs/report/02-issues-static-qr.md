# Issues Encountered: QR tĩnh bị lộ và giải pháp QR động gắn thiết bị (ADR-12)

> Phần này trả lời yêu cầu của đề bài M2 mục **"Issues Encountered"** đối với vấn đề **I-2 (QR tĩnh bị lộ dẫn tới vào trái phép hoặc gian lận chấm công)**: *"Propose architectural enhancements ... explain how it mitigates ... how it should be incorporated"*.

---

## 1. Vấn đề

Hệ thống TMC phục vụ khoảng 1.400 nhân viên tại 2 nhà máy (Bình Dương `BD` và Long An `LA`), sử dụng mã QR hiển thị trên điện thoại để quét qua cổng xoay tại các trạm kiosk. Nếu hệ thống chấp nhận **QR tĩnh** hoặc cơ chế xác thực không gắn chặt với thiết bị và cửa sổ thời gian ngắn, kẻ xấu có thể khai thác theo bốn kịch bản phổ biến:

1. **Chụp màn hình / chuyển tiếp ảnh QR:** Nhân viên hoặc kẻ gian chụp ảnh mã QR rồi gửi qua tin nhắn/email cho người khác dùng để chấm công hộ hoặc xâm nhập nhà máy.
2. **Đánh cắp phiên đăng nhập:** Nếu token QR chỉ gắn với danh tính nhân viên mà server là bên ký, bất kỳ ai lấy được phiên đăng nhập trên trình duyệt/thiết bị khác đều xin được mã QR hợp lệ thay cho chủ tài khoản.
3. **Quét lặp khi mất mạng (offline replay):** Khi đường truyền từ kiosk về Edge bị đứt, bộ kiểm tra dùng một lần đặt tại Redis của Edge không thể chặn ngay việc quét lại cùng một ảnh QR tại cùng kiosk hoặc giữa hai kiosk khác nhau.
4. **Gian lận liên nhà máy hoặc dùng thiết bị đã mất:** Một nhân viên bị mất điện thoại hoặc xuất hiện tại hai nhà máy cách nhau không hợp lý về mặt vật lý.

---

## 2. Vì sao thiết kế M1 chưa đủ

Trong báo cáo M1 (`docs/document.md` mục 4.2.3, 4.3, 5.7, 16.2), nhóm đã thiết kế mã QR là token JWS ký `ES256` hiệu lực 30 giây, mang `jti` dùng một lần và giữ chỗ bằng Redis `SET NX`. Tuy nhiên, rà soát lại đặc tả M1 phát hiện **bốn lỗ hổng thực sự (L1–L4)**:

| Mã | Lỗ hổng còn hở trong M1 | Cách bị khai thác |
| --- | --- | --- |
| **L1** | **UC-07 (luồng thay thế 1.1) và UC-08 (luồng thay thế 1.2)** vẫn cho phép gửi **QR tĩnh qua email có hạn theo ngày** thay cho QR động | Chụp màn hình hoặc chuyển tiếp email là dùng được suốt cả ngày — đây chính là lỗ hổng trực tiếp dẫn tới bài toán I-2 của đề bài |
| **L2** | Token do **server sinh và ký**; UC-07 ngoại lệ 5.1 nêu rõ điện thoại mất mạng thì không làm mới được QR | Nhân viên vào vùng sóng yếu tại cổng phải nhờ bảo vệ mở tay (UC-06), tạo áp lực nghiệp vụ phải quay lại dùng QR tĩnh qua email |
| **L3** | Token chỉ gắn với `sub` (nhân viên), **không ràng buộc với khóa của thiết bị vật lý** | Chiếm được phiên đăng nhập là tạo được QR hợp lệ từ máy lạ |
| **L4** | Khi kiosk offline, kiểm tra `jti` một lần trên Redis Edge **không hoạt động**; đồng thời M1 chưa có quy tắc phát hiện gian lận hành vi liên nhà máy | Quét lặp một ảnh QR chụp màn hình ở kiosk mất mạng hoặc hai kiosk khác nhau |

---

## 3. Giải pháp kiến trúc (ADR-12)

Để đóng đồng thời cả bốn lỗ hổng L1–L4 mà vẫn giữ ngân sách thời gian qua cổng `<= 2 giây` (NFR-06, AC-01), hệ thống áp dụng [ADR-12](../adr/ADR-12-dynamic-qr-device-binding.md) kết hợp với ADR-08 và ADR-09 trong [`docs/document.md`](../document.md) (chi tiết mô hình đe dọa ở [`docs/architecture/privacy-notes.md`](../architecture/privacy-notes.md)) theo bốn trụ cột cơ chế:

### 3.1. Loại bỏ hoàn toàn QR tĩnh khỏi luồng xác thực tại cổng
- Xóa bỏ luồng thay thế 1.1 của UC-07 và 1.2 của UC-08: **không bao giờ phát hành QR tĩnh qua email** làm credential mở cổng.
- Trường hợp nhân viên không có điện thoại dùng được tại cổng đi theo đường lùi có kiểm soát **UC-06** (bảo vệ xác minh nhân thân và mở cổng thủ công, bắt buộc nhập lý do và ghi sự kiện kiểm toán `AuditRecorded`).

### 3.2. Ràng buộc thiết bị (Device Binding) và Danh bạ đủ điều kiện từ HR
- Khi đăng ký thiết bị lần đầu (`POST /self-service/v1/devices`, `DeviceManagementService`), điện thoại tự sinh cặp khóa bất đối xứng **ECDSA P-256** ngay trên thiết bị (`deviceKey.ts` dùng WebCrypto không thể xuất khóa riêng `extractable = false` trên web app; tương đương Keystore/Secure Enclave trên app gốc) và chỉ gửi **khóa công khai** (X.509 SubjectPublicKeyInfo Base64) lên `user-qr`.
- `user-qr` lưu ánh xạ `(device_id, employee_uuid, public_key, status)` vào bảng `devices` (`V1__device_registry.sql`, `JdbcDeviceKeyRegistry`). Chỉ mục duy nhất bán phần `uq_one_active_device_per_employee` bảo đảm **mỗi nhân viên có tối đa 1 thiết bị `ACTIVE`**. Đăng ký thiết bị mới tự động thu hồi thiết bị cũ trong một transaction; nếu hai luồng đăng ký chạy **song song**, transaction thua bị từ chối với `ActiveDeviceConflictException` → HTTP **`409 Conflict`**, rollback sạch và ghi audit `CONFLICT` (C-01).
- Nhân viên có thể tự thu hồi (`DELETE /self-service/v1/devices/{deviceId}`); HR Staff hoặc System Admin có thể thu hồi khi mất máy (`POST /hr/v1/employees/{employeeUuid}/devices/revoke`). Ngoài ra, khi HR cập nhật trạng thái nhân viên thành `INACTIVE` qua `PUT /hr/v1/employees/{employeeUuid}/eligibility` (C-02, bảng `employees` trong `V3__employees.sql`), `EmployeeEligibilityService` **tự động thu hồi thiết bị `ACTIVE`** của nhân viên đó và ghi `AuditRecorded` trong cùng transaction.

### 3.3. Token QR do thiết bị tự ký, hoạt động cả khi điện thoại mất mạng
- Thay vì gọi server xin mã mỗi 30 giây, điện thoại tự ký compact JWS ngay trên máy theo chuẩn chung `docs/api/fixtures/qr-token-policy.json`:
  - Header: `alg: ES256`, `typ: JWT`, `kid: <device_id>`.
  - Claims bắt buộc: `sub` (UUID nội bộ của nhân viên), `jti` (UUID dùng một lần), `iat`, `exp` (mặc định `iat + 30s`); claims tùy chọn được phép: `fac`, `nonce`.
  - Bảo vệ ADR-09 & chặn giả QR tĩnh: `NimbusTokenCodec` từ chối ngay (`MALFORMED_TOKEN`) bất kỳ claim lạ nào ngoài 6 claim trên (ví dụ `name`); `TokenVerificationPolicy` từ chối mọi token có `exp - iat > 60s` (`maxLifetime = 60s`), kiểm `iat > now + 60s` (`CLOCK_SKEW_EXCEEDED`) và `now > exp + 60s` (`QR_EXPIRED`).
- Trình tự xác thực trong `VerifyQrTokenUseCase` (fail-secure, rẻ và an toàn trước): (1) phân tích cấu trúc & kiểm tra PII → (2) tra cứu thiết bị theo `kid` (`DEVICE_UNKNOWN`) → (3) kiểm trạng thái thiết bị (`DEVICE_REVOKED`) → (4) kiểm tra `device.employeeUuid == claims.sub` tránh thiết bị này ký thay cho người khác (`INVALID_SIGNATURE`) → (5) xác thực chữ ký ES256 bằng khóa công khai đã đăng ký → (6) kiểm tra quy tắc thời gian (`TokenVerificationPolicy`).

### 3.4. Bốn lớp chống dùng lại (R1–R4) và hiện trạng trong mã

| Lớp | Nơi thực hiện | Cơ chế | Tình trạng hiện tại trong mã |
| --- | --- | --- | --- |
| **R1** | `gate-station` (online) | Giữ chỗ `jti` nguyên tử bằng `ITokenReservation` (`RedisTokenReservation` dùng `SET NX` với TTL `exp + 60s`). `VerifyQrTokenUseCase` của `user-qr` **chỉ đọc**, không đánh dấu đã dùng, để `gate-station` nhả được `jti` nếu cổng phần cứng báo lỗi (`JAM`/`OFFLINE`). | **Đã có & đã kiểm chứng** (`RecordScanUseCaseTest`, `RedisAdaptersIT`). |
| **R2** | `kiosk-simulator` (offline) | Bộ nhớ đệm replay cục bộ lưu trong SQLite journal (`KioskApp`, `Journal`): từ chối ngay lập tức nếu cùng `jti` được đưa vào lại cùng kiosk trong lúc LAN đứt. | **Đã có & đã kiểm chứng** (`KioskAppTest`). |
| **R3** | `attendance` (nhập lô offline) | `IngestOfflineBatchUseCase` phát hiện cùng `jti` xuất hiện trong lô đồng bộ từ hai kiosk khác nhau, gắn cờ bất thường `TOKEN_REUSED_OFFLINE` và **không tính công**. | **Đã có & đã kiểm chứng** (`Adr11ResilienceTest`). |
| **R4** | `reporting` (trung tâm) | Quy tắc hành vi phát hiện gian lận liên nhà máy (ví dụ cùng `sub` quét ở `BD` và `LA` cách nhau 10 phút) và lặp từ chối bất thường. | **Mới có interface** `FraudRule` (`services/reporting/.../domain/FraudRule.java`); hiện thực rule cụ thể thuộc story D-05 của Team D (chưa khẳng định là đã chạy trong bản hiện tại). |

---

## 4. Cách giảm thiểu từng kịch bản & Mô hình đe dọa STRIDE (C-10)

Bảng đầy đủ được duy trì tại [`docs/architecture/privacy-notes.md`](../architecture/privacy-notes.md#L30-L40). Tóm tắt 7 kịch bản cốt lõi của luồng QR và đăng nhập:

| Kịch bản tấn công / rủi ro | STRIDE | Quy tắc giảm thiểu chính | Bằng chứng kiểm chứng |
| --- | --- | --- | --- |
| **Chụp màn hình QR** | Spoofing, Tampering | Hiệu lực 30s (`maxLifetime <= 60s`); giữ chỗ `jti` nguyên tử (R1), replay cache offline (R2), phát hiện trùng giữa các kiosk (R3). | `Adr12TokenVerificationTest`, `QrTokenPolicyContractTest`, `RecordScanUseCaseTest`, `RedisAdaptersIT`, `KioskAppTest.should_refuse_the_same_token_twice_offline_using_the_local_replay_cache`; `scripts/demo/online.sh` bước 4; chế độ *"Ảnh chụp màn hình"* của `employee-app`. |
| **Chuyển tiếp ảnh QR / QR 24h / QR chứa PII** | Spoofing, Info Disclosure, Elevation | Bỏ QR tĩnh qua email; từ chối `exp - iat > 60s` và mọi claim ngoài tập cho phép (`MALFORMED_TOKEN`). | `Adr12TokenVerificationTest` (`should_reject_a_long_lived_token_that_behaves_like_a_static_qr`, `should_reject_a_token_that_carries_a_pii_claim`), `token.test.ts`; `scripts/demo/online.sh` bước 5–6; chế độ *"QR sống 24 giờ"*, *"QR chứa họ tên"*. |
| **Mất điện thoại** | Spoofing, Elevation | Khóa riêng không xuất khỏi thiết bị; thu hồi tự phục vụ, thu hồi bởi HR, hoặc tự động thu hồi khi nhân viên thành `INACTIVE` (C-02) → `DEVICE_REVOKED` qua `sync-bundle`. | `Adr12TokenVerificationTest` (`should_reject_tokens_of_a_revoked_device`, `should_revoke_the_previous_device_when_a_new_one_is_registered`), `DeviceAndVerifyApiTest`, `SyncBundleTest`, `EmployeeDirectoryIT`, `KioskAppTest.should_not_open_offline_for_a_device_revoked_in_the_last_sync`; nút *"Thu hồi thiết bị này"*. |
| **Giả mạo thiết bị / ký thay / đăng ký song song** | Spoofing, Tampering, Elevation | Kiểm tra `kid` ACTIVE, `device.employeeUuid == sub` và chữ ký ES256; chỉ chủ thể tự đăng ký; chỉ mục duy nhất `uq_one_active_device_per_employee` chặn đăng ký đồng thời (`201`/`409`). | `Adr12TokenVerificationTest` (`should_reject_a_token_signed_by_a_different_key_with_the_same_kid`, `should_reject_a_device_that_vouches_for_another_employee`, `should_not_let_an_employee_register_a_device_for_someone_else`), `DeviceRegistrationConflictTest`, `DeviceConflictApiTest`, `ConcurrentDeviceRegistrationIT`. |
| **Lệch đồng hồ điện thoại** | Tampering, DoS | Dung sai `clockSkew = 60s`; `iat > now + 60s` → `CLOCK_SKEW_EXCEEDED`, `now > exp + 60s` → `QR_EXPIRED`. | `Adr12TokenVerificationTest` (nhóm test thời gian), `QrTokenPolicyContractTest`. |
| **Kiosk bị can thiệp** | Tampering, Repudiation, Info Disclosure | Kiosk chỉ giữ khóa công khai từ `sync-bundle`; journal chỉ lưu SHA-256 của token; nhập lô phát hiện `TOKEN_REUSED_OFFLINE`. | `KioskAppTest` (`should_never_store_the_raw_token_in_the_journal`, `should_retry_an_unfinished_scan_with_the_hash_never_the_raw_token`), `Adr11ResilienceTest`, `simulators/kiosk-simulator/.../QrTokenPolicyContractTest`. |
| **Token JWT bị lộ / tấn công tài khoản quản trị** | Spoofing, Elevation, Repudiation | ES256 JWT 15 phút (`iss`, `aud`, `kid` qua JWKS), tắt header dev ở profile mặc định (`allow-dev-headers = false`), hỗ trợ xoay khóa ký (C-12), khóa 15 phút sau 5 lần sai + TOTP RFC 6238 chống replay cho `HR_STAFF`/`SYSTEM_ADMIN` (C-05), audit outbox bền (C-08, C-11). | `BearerTokenApiTest`, `JwtPrincipalVerifierTest`, `LoginApiTest`, `AuthenticationTest`, `TotpMfaVerifierTest`, `TotpLoginApiTest`, `SigningKeyRotationTest`, `JwksRotationApiTest`, `LoginAuditApiTest`, `IdentityOutboxIT`. |

---

## 5. Đưa vào kiến trúc

Các thay đổi được tích hợp trực tiếp vào bốn góc nhìn kiến trúc của M1 (cập nhật chi tiết sơ đồ trong [`04-views-update.md`](04-views-update.md) và phản ánh trung thực theo [`implementation-notes.md`](../architecture/implementation-notes.md) mục 1):

| View | Thành phần / Thay đổi chính |
| --- | --- |
| **Module view** | `services/user-qr`: thêm domain `Device`, `QrClaims`, `TokenVerificationPolicy`; ports `IDeviceKeyRegistry`, `IEmployeeDirectory`, `IEmployeeEligibility`, `ITokenCodec`, `IAuditLog`, `ITransactionRunner`; adapters `JdbcDeviceKeyRegistry`, `JdbcEmployeeDirectory`, `NimbusTokenCodec`, `OutboxAuditLog`. `services/identity`: thêm `TotpMfaVerifier`, `SigningKeys`, `OutboxAuditLog`, `JdbcOutboxStore`. `platform/security`: `JwtPrincipalVerifier`, `PrincipalResolver`. *(Lưu ý: `ITokenReservation` đặt ở `gate-station` thay vì `user-qr` để hỗ trợ nhả token khi cổng lỗi — `implementation-notes.md` mục 1 điểm 3).* |
| **Component & Connector view** | Bổ sung API đăng ký/thu hồi thiết bị (`/self-service/v1/devices`, `/hr/v1/employees/{id}/devices/revoke`), API cập nhật nhân viên (`/hr/v1/employees/{id}/eligibility`), gói đồng bộ `/internal/v1/sync-bundle` trả cả `deviceKeys[]` và `employees[]`, endpoint JWKS `/.well-known/jwks.json`, và luồng `AuditRecorded` từ `user-qr`/`identity` qua RabbitMQ `tmc.events` tới `audit-admin`. |
| **Deployment view** | Khóa riêng thiết bị nằm trong trình duyệt/keystore điện thoại; CSDL `user_qr` và `identity` đặt trên PostgreSQL central (`V1`–`V3`); bản sao khóa công khai và danh bạ đủ điều kiện được `gate-station` và `kiosk-simulator` kéo định kỳ qua `sync-bundle`; `user-qr` hiện chạy endpoint `/internal/v1/qr/verify` cùng service trung tâm/biên theo mô hình logic Edge (`implementation-notes.md` mục 1 điểm 6). |
| **API & Hợp đồng dùng chung** | `docs/api/user-qr.openapi.yaml` (bổ sung `409` cho đăng ký song song, `PUT /hr/v1/employees/{id}/eligibility`, `employees[]` trong `sync-bundle`), `docs/api/identity.openapi.yaml` (JWT ES256, JWKS, xoay khóa C-12), `docs/api/fixtures/qr-token-policy.json` (được kiểm tra đồng bộ tự động ở 3 nơi: `user-qr`, `kiosk-simulator`, `apps/employee-app`). |

---

## 6. Đánh đổi và rủi ro còn lại

1. **Phụ thuộc đồng hồ điện thoại & dung sai `clockSkew = 60s`:** Chấp nhận lệch 60 giây giúp tránh từ chối oan nhưng kéo dài cửa sổ hiệu lực tối đa của token từ 30s lên 90s; bù lại Redis giữ chỗ `jti` kéo dài tương ứng tới `exp + 60s`.
2. **Đánh đổi của kiểm tra R1 tại Redis Edge:** `RedisTokenReservation` bảo đảm đúng một lượt thắng trong race condition (`RedisAdaptersIT`), nhưng nếu Redis Edge mất trắng dữ liệu (dù compose bật AOF), token chưa hết hạn có thể bị quét lại trong tối đa `exp + 60s`.
3. **Độ trễ đồng bộ thu hồi & kiosk offline:** `gate-station` đồng bộ `sync-bundle` mỗi 30 giây (`EmployeeDirectorySyncJob`); kiosk offline chưa nhận bản đồng bộ mới hoặc hiện chưa lọc trường `employees[]` trong cache cục bộ (phạm vi story D-15).
4. **Kênh truyền, lưu trữ và quản lý phiên chưa hoàn thiện ở M2:**
   - Kết nối nội bộ và lưu trữ phát triển chưa cấu hình TLS 1.2+/mTLS và mã hóa đĩa/cột AES-256 (thuộc D-11).
   - `identity` hiện lưu `Account` và bí mật TOTP trong bộ nhớ (bí mật mẫu chỉ bật ở profile `local`, `inmem`; mặc định fail-closed), chưa có DB tài khoản (C-03/C-07 cắt), chưa có hết hạn phiên theo không hoạt động hoặc thu hồi JWT trước hạn 15 phút (`should_expire_an_idle_session` và `should_end_open_sessions_when_a_role_changes` trong `AuthenticationTest` đang `@Disabled`).
   - Bảo mật trình duyệt thử nghiệm (`apps/employee-app`) dùng IndexedDB + WebCrypto non-extractable thay cho hardware keystore của ứng dụng di động gốc.

---

## 7. Bằng chứng kiểm chứng

Toàn bộ cơ chế thuộc phạm vi Team C đã được kiểm chứng tự động sau lần chỉnh sửa cuối cùng (trên JDK 21 và Docker Desktop):

1. **Unit & Architecture tests (`spotlessCheck` xanh):**
   - `:services:user-qr:test`: **30/30 passed** (`Adr12TokenVerificationTest` 14 ca, `QrTokenPolicyContractTest`, `EmployeeSeedContractTest`, `SyncBundleTest` 3 ca, `DeviceRegistrationConflictTest` 2 ca, `DeviceConflictApiTest`, `EmployeeEligibilityApiTest` 2 ca, `BearerTokenApiTest` 2 ca, `DeviceAndVerifyApiTest` 3 ca, `ArchitectureTest`).
   - `:services:identity:test`: **29 passed, 2 skipped có chủ đích** (`TotpMfaVerifierTest` 6 ca gồm vector RFC 6238 Appendix B, `TotpLoginApiTest`, `LoginApiTest` 2 ca, `LoginAuditApiTest`, `SigningKeyRotationTest` 4 ca, `JwksRotationApiTest`, `AuthenticationTest` 12 passed / 2 `@Disabled`, `EmployeeSeedContractTest`, `ArchitectureTest`).
   - `:simulators:device-simulator:test`, `:platform:security:test`, `:simulators:kiosk-simulator:test --tests "*QrTokenPolicyContractTest"`: **xanh toàn bộ**.
2. **Integration tests trên PostgreSQL thật (`Testcontainers` + Flyway V1–V3):**
   - `:services:user-qr:integrationTest`: **13/13 passed** (`ConcurrentDeviceRegistrationIT` 2 ca — gồm thay thế song song thiết bị đã tồn tại trả `[201, 409]`, `EmployeeDirectoryIT` 2 ca — gồm seed, `INACTIVE` tự động thu hồi thiết bị, audit `DENIED`, `UserQrPersistenceIT` 3 ca, `DeviceAuditOutboxIT` 5 ca, `DefaultProfileAuditIT` 1 ca).
   - `:services:identity:integrationTest`: **1/1 passed** (`IdentityOutboxIT` — outbox bền qua khởi động lại context).
3. **Kịch bản demo & app nhân viên:**
   - `scripts/demo/online.sh` kiểm chứng luồng: đăng ký khóa thiết bị → kiosk đồng bộ → quét token 30s hợp lệ (`ALLOWED`) → quét lại cùng ảnh chụp màn hình (`QR_ALREADY_USED`) → token 24h (`MALFORMED_TOKEN`) → token chứa họ tên (`MALFORMED_TOKEN`) → quét vào hai lần liên tiếp (`ANTI_PASSBACK_VIOLATION`). *(Lưu ý: trên stack Docker đầy đủ do I-6 điều phối; trong CI hiện chạy trên stack profile `local`).*
   - `apps/employee-app` (`docs/handover/02-chay-local.md` mục 5.3) cung cấp 4 chế độ trực quan: *Bình thường*, *Ảnh chụp màn hình*, *QR sống 24 giờ*, *QR chứa họ tên* và nút *Thu hồi thiết bị này*.
