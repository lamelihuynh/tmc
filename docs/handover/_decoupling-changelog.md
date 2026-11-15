# Nhật ký gỡ phụ thuộc giữa các team (nhánh `chore/x/decouple-teams`)

> Tệp này được cập nhật sau mỗi phần việc, để nếu phiên làm việc bị ngắt thì vẫn biết đã xong gì.
> Bước sau sẽ dùng nội dung ở đây để viết lại `docs/handover/team-*.md` và lịch bàn giao.
> Quy ước: **Đã kiểm chứng** = đã chạy thật và xanh trên máy có Docker (ghi số test); **Chưa kiểm chứng** = chưa chạy.

## 1. Các commit trên nhánh

| Commit | Nội dung |
| --- | --- |
| `d7884c6` | Hợp đồng dùng chung trong `docs/api` (JWT, JWKS, AsyncAPI `tmc.events`, AttendanceCorrected, `employees` trong gói đồng bộ, fixture) |
| `cdb7efb` | Attendance trả lượt tính công cuối (`GET /internal/v1/employees/{uuid}/last-direction`), `AttemptView` có `employeeUuid`, `direction`; gate-station có `IAttendanceGateway.lastDirection` |
| `e433728` | AttendanceCorrected: bảng `attendance_corrections` (V2), `AttendanceCorrection`, `IAttendanceCorrectionRepository` (in-memory + JDBC), `AttendanceEventFactory.attendanceCorrected`, reporting áp dụng bản hiệu chỉnh |

## 2. Blocker đã gỡ

### 2.1 AttendanceCorrected (B-06 ↔ D-04/D-06)

- Hợp đồng: `docs/api/attendance-events.asyncapi.yaml`, kênh `attendance.AttendanceCorrected`, schema `CorrectionPayload`
  (`correctionId`, `originalAttemptId`, `revision`, `employeeUuid`, `direction`, `scannedAt`, `previousDirection`,
  `previousScannedAt`, `reason` là mã UPPER_SNAKE_CASE, `correctedBy`, `source`).
- `eventId` = `attemptId` gốc; `aggregateVersion` = phiên bản lượt gốc + `revision`
  (`AttendanceEventFactory.correctedVersion`). Consumer áp bản có `aggregateVersion` lớn nhất.
- Mã: `services/attendance/.../domain/AttendanceCorrection.java`, `.../port/out/IAttendanceCorrectionRepository.java`,
  `.../adapter/out/inmem/InMemoryAttendanceCorrectionRepository.java`,
  `.../adapter/out/persistence/JdbcAttendanceCorrectionRepository.java`, `db/migration/V2__attendance_corrections.sql`.
- Reporting: `ProjectAttendanceEventUseCase` chiếu cả `AttendanceCorrected`; test
  `ProjectionTest.should_apply_a_correction_event_on_top_of_the_original` đã bật.
- Team B (B-06) chỉ còn viết use case hiệu chỉnh + endpoint HR, dùng các lớp trên.
- Team D: hr-integration chưa xử lý `AttendanceCorrected` (thuộc D-06, độc lập).

### 2.2 JWT (C-04 ↔ mọi team dùng xác thực)

- Hợp đồng: `docs/api/identity.openapi.yaml` (ES256, 15 phút, claim `iss`/`aud`/`sub`/`roles`/`fac`/`gates`/`iat`/`exp`/`jti`,
  JWKS, lỗi). `central.openapi.yaml` trỏ sang.
- identity (cổng 8087): `POST /auth/v1/login` trả `accessToken`, `tokenType`, `expiresIn` (900), `expiresAt`, vẫn giữ
  `subject`, `roles` cho thanh danh tính dev. `GET /.well-known/jwks.json`. Khóa: `IDENTITY_JWT_PRIVATE_JWK`
  (trống = tự sinh khi khởi động, chỉ dev). Mã: `LoginUseCase`, `NimbusAccessTokenIssuer`, `JwksController`, `DevSeed`.
- `platform/security`: `jwt/AccessTokenContract`, `jwt/AccessTokenSigner`, `jwt/JwtPrincipalVerifier`, `PrincipalResolver`
  (Bearer trước; header dev chỉ khi service cho phép và không có `Authorization`).
- Fixture test cho mọi team: `testImplementation(testFixtures(project(":platform:security")))` →
  `TestJwtKeys.TRUSTED/UNTRUSTED`, `TestJwtKeys.verifier()`, `TestTokens.valid/expired/signedByUntrustedKey/unsigned/withIssuer/withAudience/withRoles/bearer`.
- Đã gắn vào **user-qr** (`UserQrConfig.principalResolver`): `tmc.security.jwks-url` (env `IDENTITY_JWKS_URL`),
  `tmc.security.allow-dev-headers` = `true` chỉ ở `local`, `inmem`. Test: `BearerTokenApiTest`.
- Chưa gắn (opt-in, mỗi team tự làm theo mẫu user-qr, việc nhỏ): gate-station `SecurityController`, reporting,
  hr-integration, audit-admin. Web portal và employee app vẫn gửi header dev (profile `local`).
- Còn lại cho Team C (độc lập): TOTP (C-05), hết hạn phiên khi không hoạt động, quản lý tài khoản (FR-31), xoay khóa.

## 3. Những gì đã chạy (cập nhật dần)

| Lệnh | Kết quả |
| --- | --- |
| `./gradlew :services:attendance:test` | 34 test, 0 lỗi |
| `./gradlew :services:attendance:integrationTest` (Testcontainers, Docker thật) | 8 test, 0 lỗi (gồm 5 test mới cho hiệu chỉnh, trạng thái, lượt cuối, đếm lỗi outbox) |
| `./gradlew :services:reporting:test` | 9 test, 2 bỏ qua (TODO Team D), 0 lỗi |
| `./gradlew :platform:security:test` | 13 test, 0 lỗi (7 test JWT mới) |
| `./gradlew :services:identity:test` | 16 test, 3 bỏ qua (TODO Team C khác), 0 lỗi; gồm `LoginApiTest`, `EmployeeSeedContractTest` |
| `./gradlew :services:user-qr:test` | 21 test, 0 lỗi; gồm `BearerTokenApiTest` |
