# Nhật ký gỡ phụ thuộc giữa các team (nhánh `chore/x/decouple-teams`)

> Tệp này được cập nhật sau mỗi phần việc, để nếu phiên làm việc bị ngắt thì vẫn biết đã xong gì.
> Bước sau sẽ dùng nội dung ở đây để viết lại `docs/handover/team-*.md` và lịch bàn giao.
> Quy ước: **Đã kiểm chứng** = đã chạy thật và xanh trên máy có Docker (ghi số test); **Chưa kiểm chứng** = chưa chạy.
> Nhánh chưa push, chưa merge.

## 1. Các commit trên nhánh

| Commit | Nội dung |
| --- | --- |
| `d7884c6` | Hợp đồng dùng chung trong `docs/api` (JWT, JWKS, AsyncAPI `tmc.events`, AttendanceCorrected, `employees` trong gói đồng bộ, fixture) |
| `cdb7efb` | Attendance trả lượt tính công cuối (`GET /internal/v1/employees/{uuid}/last-direction`), `AttemptView` có `employeeUuid`, `direction`; gate-station có `IAttendanceGateway.lastDirection` |
| `e433728` | AttendanceCorrected: bảng `attendance_corrections` (V2), lưu trữ in-memory + JDBC, `AttendanceEventFactory.attendanceCorrected`, reporting áp dụng bản hiệu chỉnh |
| `113b65c` | `platform/security`: kiểm tra JWT, `PrincipalResolver`, fixture test `TestJwtKeys`, `TestTokens` |
| `3854523` | identity cấp JWT ES256 và JWKS; user-qr nhận Bearer |
| `ce50584` | `employees` trong gói đồng bộ (user-qr → gate-station → kiosk), danh bạ cổng tự làm mới, test seed một nguồn |
| `cf0cefd` | `platform/messaging`: exchange `tmc.events`, outbox, relay, publisher AMQP, dedup JDBC, `AuditEvents` |
| `16ed90c` | Consumer RabbitMQ thật ở reporting, hr-integration, audit-admin (hàng đợi + DLQ + dedup JDBC); attendance phát lên `tmc.events`; `TmcContainers` |
| `96252e1` | user-qr, identity: outbox + relay + publisher sẵn cho audit (C-08); IT của `JdbcDeviceKeyRegistry` |
| `c685fbc` | gate-station: database `gate_store` (profile `gate-store`), IT Redis và Gate Store |
| `fa71ce0` | Contract test chính sách token QR (user-qr, kiosk-simulator, employee app) |
| `0c2abfd` | identity trong local stack và job demo CI; bỏ theo dõi `tsconfig.tsbuildinfo`; danh sách việc CODEOWNERS |
| (ADR) | ADR-11, 12, 13 Accepted; ADR-14, ADR-15 mới |
| (docs) | Runbook, implementation-notes, nhật ký này |

Các commit nhật ký trung gian (`docs(handover): ...`) không liệt kê.

## 2. Blocker đã gỡ

### 2.1 AttendanceCorrected (B-06 ↔ D-04/D-06)

- Hợp đồng: `docs/api/attendance-events.asyncapi.yaml`, kênh `attendance.AttendanceCorrected`, schema `CorrectionPayload`
  (`correctionId`, `originalAttemptId`, `revision`, `employeeUuid`, `direction`, `scannedAt`, `previousDirection`,
  `previousScannedAt`, `reason` là mã UPPER_SNAKE_CASE, `correctedBy`, `source`).
- `eventId` = `attemptId` gốc; `aggregateVersion` = phiên bản lượt gốc + `revision`
  (`AttendanceEventFactory.correctedVersion`). Consumer áp bản có `aggregateVersion` lớn nhất.
- Mã: `services/attendance/.../domain/AttendanceCorrection.java`, `.../port/out/IAttendanceCorrectionRepository.java`,
  `InMemoryAttendanceCorrectionRepository`, `JdbcAttendanceCorrectionRepository`, `db/migration/V2__attendance_corrections.sql`.
- Reporting chiếu cả `AttendanceCorrected` (`ProjectionTest.should_apply_a_correction_event_on_top_of_the_original` đã bật,
  và `AttendanceEventListenerIT` qua RabbitMQ thật). hr-integration đã bind `attendance.AttendanceCorrected` nhưng use case
  chưa xử lý (D-06). Không có test `@Disabled` nào về hiệu chỉnh trong hr-integration để bật.

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
  `tmc.security.allow-dev-headers` = `true` chỉ ở `local`, `inmem`. Test: `BearerTokenApiTest`; thử thật D-01 (201 với Bearer, 403 với header dev).
- Chưa gắn (opt-in, theo mẫu user-qr, mỗi chỗ vài dòng): gate-station `SecurityController`, reporting, hr-integration,
  audit-admin. Web portal và employee app vẫn gửi header dev (profile `local`).

### 2.3 `employees` trong gói đồng bộ (C-02 ↔ A, D-kiosk)

- user-qr `GET /internal/v1/sync-bundle` có thêm `employees[]` (`employeeUuid`, `status` ACTIVE/INACTIVE, `factories`, `updatedAt`).
  Nguồn hiện tại: `IEmployeeDirectory` + `InMemoryEmployeeDirectory`, nạp từ `tmc.qr.employees`. C-02 chỉ cần thay adapter.
- gate-station: `HttpKioskSyncSource` đọc `employees` (bỏ qua trường lạ, bản cũ không có mảng = rỗng);
  `IEmployeeDirectoryCache` + `SyncEmployeeDirectoryUseCase` làm mới danh bạ cổng 30 giây/lần
  (`EmployeeDirectorySyncJob`, tắt bằng `EMPLOYEE_SYNC_ENABLED=false`, tắt sẵn ở `inmem`);
  `GET /kiosk/v1/sync-bundle` chuyển tiếp `employees` cho kiosk.
- Seed nhân viên một nguồn: `docs/api/fixtures/employee-seed.json`; `EmployeeSeedContractTest` ở identity, user-qr, gate-station.

### 2.4 Hạ tầng sự kiện dùng chung (ADR-15; B-02, C-08, D-03)

- Exchange `tmc.events`, khóa `<nguồn>.<EventType>` (`platform/messaging/TmcExchange`). attendance giữ khóa cũ, đổi exchange.
- `platform/messaging`: `outbox/OutboxStore`, `InMemoryOutboxStore`, `OutboxRelay`, `AuditOutbox`; `jdbc/JdbcOutboxStore`,
  `jdbc/JdbcProcessedMessageStore`; `amqp/AmqpEnvelopePublisher`; `AuditEvents`; `IdempotentConsumer.TransactionRunner`.
  DDL tham khảo: `platform/messaging/src/main/resources/tmc-messaging/*.sql`.
- Consumer thật (profile mặc định; tắt ở `inmem` và profile mới `local`): reporting `tmc.reporting.attendance` ← `attendance.*`;
  hr-integration `tmc.hr.attendance` ← `AttendanceConfirmed`, `AttendanceCorrected`; audit-admin `tmc.audit.events` ← `*.AuditRecorded`.
  Mỗi hàng đợi có DLQ `<tên>.dlq`, thử lại 3 lần. Dedup: bảng `processed_messages` (Flyway V1 của từng service).
- Bên phát: user-qr (outbox JDBC, Flyway V2) và identity (outbox **trong bộ nhớ**, chưa có DB) đã có bean `AuditOutbox`,
  relay 2 giây/lần, publisher RabbitMQ. C-08 chỉ còn trỏ bean `IAuditLog` sang `AuditOutbox`.
- Build: reporting, hr-integration, audit-admin thêm jdbc, flyway, postgresql, testcontainers postgres/rabbit;
  user-qr, identity thêm amqp + messaging; gate-station thêm jdbc, flyway, postgresql, messaging.
- `platform/test-support/TmcContainers`: Postgres, RabbitMQ (chờ 3 phút), Redis cho IT của mọi team.

### 2.5 Database `gate_store` (ADR-14; A-04)

- `docker/init/edge/02-gate-store.sql` (DB `gate_store`, login `gatestation`). Container cũ: chạy tay script này hoặc `make nuke && make up`.
- gate-station profile `gate-store` (`GateStoreConfig`, `application-gate-store.yml`, `GATE_STORE_DB_*`), Flyway `db/gate-store/V1__gate_store.sql`:
  bảng `gates`, `outbox_events`. A-04 còn viết `JdbcGateConfigRepository`.

### 2.6 Local stack, CI, sẵn sàng merge

- `scripts/dev/local-stack.sh` khởi động identity như `.ps1` (cả hai profile `local`); job `demo` của CI kiểm tra login và JWKS.
- `apps/web-portal/tsconfig.tsbuildinfo` không còn được theo dõi, `*.tsbuildinfo` vào `.gitignore`.
- CODEOWNERS giữ handle giữ chỗ; các bước thay ở `docs/runbook/release-checklist.md` ("Việc còn mở trước khi bật bảo vệ nhánh").

### 2.7 Chính sách token QR (mục 7)

- `QrTokenPolicyContractTest` ở user-qr và kiosk-simulator, test mới trong `apps/employee-app/src/lib/token.test.ts`; cả ba đọc
  `docs/api/fixtures/qr-token-policy.json`. device-simulator chưa kiểm (TTL mặc định nằm ở tùy chọn CLI). Ghi trong `docs/api/README.md`.

## 3. Những gì đã chạy

Máy: macOS arm64, JDK 21, Docker đang chạy (4 container `make up` healthy).

| Lệnh | Kết quả |
| --- | --- |
| `./gradlew spotlessCheck test` (toàn repo, sau mọi thay đổi) | spotless xanh; **209 test, 10 bỏ qua (TODO của team), 0 lỗi** |
| `./gradlew integrationTest --rerun` (toàn repo) | **27 test, 0 lỗi** trên container thật: messaging 5 (`MessagingInfraIT`), attendance 8 (`PersistenceAndBrokerIT`), gate-station 7 (`RedisAdaptersIT` 5, `GateStoreIT` 2), user-qr 3 (`UserQrPersistenceIT`), hr-integration 2, reporting 1, audit-admin 1 (IT listener) |
| `cd apps/employee-app && npm ci && npm test && npm run lint && npm run build` | 5 test xanh, lint và build xanh |
| D-01 thủ công: 7 jar ở profile mặc định trên hạ tầng `make up` | cả 7 khởi động; login → Bearer 201, header dev 403, sync-bundle 5 nhân viên, danh bạ cổng cập nhật, sự kiện `identity.AuditRecorded` qua RabbitMQ vào `processed_messages` của audit-admin (chi tiết: `docs/runbook/troubleshooting.md`, mục Kết quả D-01) |
| `scripts/dev/local-stack.sh start` + `scripts/demo/online.sh`, `offline.sh`, `timeout.sh` | 5 tiến trình lên; cả 3 demo `passed` |

Phân bổ test đơn vị (209): common-kernel 2, messaging 4, security 13, attendance 34, audit-admin 10 (2 bỏ qua),
gate-station 54, hr-integration 11 (3 bỏ qua), identity 16 (3 bỏ qua), reporting 10 (2 bỏ qua), user-qr 24,
device-simulator 6, kiosk-simulator 21, turnstile-simulator 4.

**Không chạy:** `apps/web-portal` `npm test` (mã portal không đổi, chỉ bỏ theo dõi tệp build); GitHub Actions (chưa push).

## 4. Việc còn lại theo team (độc lập, không chờ ai)

- **Team A:** A-03/A-06 dùng `IAttendanceGateway.lastDirection` và `AttemptState.direction` trong `RecordScanUseCase`;
  A-04 `JdbcGateConfigRepository` trên `gate_store`; gắn `PrincipalResolver` vào `SecurityController`; audit cổng qua
  `JdbcOutboxStore` + `AuditOutbox` trên `gate_store`; A-09 đo tải.
- **Team B:** B-06 use case + endpoint hiệu chỉnh (dùng `AttendanceCorrection`, repository, `attendanceCorrected`);
  B-02 test dừng và bật lại RabbitMQ; B-07, B-08.
- **Team C:** C-02 thay `InMemoryEmployeeDirectory` của user-qr bằng nguồn HR; C-08 trỏ `IAuditLog` của user-qr và identity
  sang `AuditOutbox` (bọc transaction ở user-qr); C-05 TOTP; database cho identity (để outbox bền); xoay khóa JWT; C-10.
- **Team D:** read model JDBC (D-04) chung transaction với dedup; hr-integration xử lý `AttendanceCorrected` (D-06);
  kho audit bền + chuỗi băm (D-08); cảnh báo DLQ; kiosk dùng `employees` khi offline; portal và employee app gửi Bearer;
  thay handle CODEOWNERS; D-05, D-07, D-12.

## 5. Chưa làm được hoặc chưa kiểm chứng

- Dừng/bật lại RabbitMQ giữa lúc phát (B-02): chưa có test.
- 3 demo trên stack Docker đầy đủ: chưa chạy (chỉ chạy trên `local`).
- identity outbox chưa bền (không có DB); read model reporting, log HR, kho audit vẫn trong bộ nhớ.
- JWT chỉ gắn ở user-qr; các service khác vẫn chỉ đọc header.
- device-simulator chưa nằm trong contract test chính sách token.
- CI trên GitHub chưa chạy cho các thay đổi này.

## 6. Quyết định mới

- ADR-14: database `gate_store` riêng cho Gate Station, bật bằng profile `gate-store`.
- ADR-15: exchange `tmc.events`, khóa `<nguồn>.<EventType>`, hàng đợi + DLQ riêng mỗi consumer, outbox và dedup dùng chung;
  thông điệp không ai bind được bỏ đi (không chặn outbox).
- ADR-11, 12, 13: Accepted, ADR khớp mã ở các điểm lệch 1–6 (`implementation-notes.md` mục 1).
- Header dev chỉ được tin ở profile `local`, `inmem` (user-qr); profile mặc định cần Bearer.
- Spring JDBC/AMQP/Jackson là `compileOnly` trong `platform/messaging`.
