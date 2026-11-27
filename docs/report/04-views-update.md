# Cập nhật Các góc nhìn Kiến trúc (Task 1.2)

> Phần này trả lời yêu cầu **Task 1.2** của đề bài M2: cập nhật Module View, Component & Connector View và Allocation/Deployment View phản ánh thay đổi từ ADR-11, ADR-12 và ADR-13. Hiện tại tệp chứa đầy đủ **phần sơ đồ bảo mật và danh tính của Team C** (R-C); các sơ đồ của Team A (luồng quét QR 15.2), Team B (outbox) và Team D (HR) sẽ được hợp nhất tại D-12.

---

## 1. Phần Team C: Sơ đồ Kiến trúc Bảo mật, Định danh & QR động

### 1.1. Component & Connector View — Luồng Bảo mật & Định danh (bổ sung cho mục 12.4 M1)

```mermaid
flowchart LR
  app["Điện thoại nhân viên<br/>apps/employee-app hoặc device-simulator<br/>(Khóa riêng EC P-256 non-extractable)"]
  portal["Web Portal<br/>apps/web-portal"]
  kiosk["Kiosk<br/>kiosk-simulator<br/>(Public Key & Eligibility Cache)"]

  subgraph central["Trung tâm (Security & Identity)"]
    idn["identity :8087<br/>LoginUseCase, TotpMfaVerifier<br/>SigningKeys, JwksController"]
    qr["user-qr :8083<br/>DeviceManagementService, EmployeeEligibilityService<br/>VerifyQrTokenUseCase, BuildSyncBundleUseCase"]
    aud["audit-admin :8086<br/>AuditEventListener (chỉ thêm)"]
  end

  subgraph edge["Biên nhà máy (Edge)"]
    gs["gate-station :8081<br/>RecordScanUseCase, RedisTokenReservation"]
    redis[("Redis<br/>jti SET NX (R1), anti-passback")]
  end

  pgQr[("PostgreSQL user_qr<br/>devices, employees, outbox_events")]
  pgIdn[("PostgreSQL identity<br/>outbox_events")]
  mq[["RabbitMQ tmc.events<br/>*.AuditRecorded"]]

  app -- "1. POST /auth/v1/login" --> idn
  portal -- "1b. Login + mfaCode (TOTP)" --> idn
  app -- "2. POST /self-service/v1/devices (Bearer)" --> qr
  portal -- "HR: PUT /hr/v1/employees/{id}/eligibility (Bearer)" --> qr
  qr --> pgQr
  idn --> pgIdn

  app -- "3. QR token ES256 (tự ký offline, 30s)" --> kiosk
  kiosk -- "POST /kiosk/v1/scans" --> gs
  gs -- "POST /internal/v1/qr/verify (chỉ đọc)" --> qr
  gs -- "SET NX jti (R1)" --> redis
  gs -- "GET /internal/v1/sync-bundle?since= (30s)" --> qr
  kiosk -. "Kiosk sync định kỳ" .-> gs
  gs -. "GET /.well-known/jwks.json" .-> idn
  qr -- "GET /.well-known/jwks.json" --> idn

  qr -- "user-qr.AuditRecorded (OutboxRelay)" --> mq
  idn -- "identity.AuditRecorded (OutboxRelay)" --> mq
  mq --> aud
```

*Hình 12.4.2 Component và Connector của luồng Bảo mật, Định danh và Xác thực QR động (Team C)*

---

### 1.2. Sequence Diagram — Đăng ký thiết bị, Tự ký QR offline, Xác thực và Đồng bộ thu hồi (bổ sung cho mục 13.1 M1)

```mermaid
sequenceDiagram
  autonumber
  participant P as Điện thoại (employee-app)
  participant I as identity (:8087)
  participant Q as user-qr (:8083)
  participant K as Kiosk
  participant G as gate-station (:8081)

  P->>I: POST /auth/v1/login (username, password)
  I-->>P: 200 accessToken (JWT ES256, 15 phút)

  Note over P: Sinh cặp khóa EC P-256 (khóa riêng không rời máy)
  P->>Q: POST /self-service/v1/devices (Bearer, publicKey)
  Q->>Q: PrincipalResolver + Rbac(EMPLOYEE) + kiểm subject
  Q->>Q: Transaction: thu hồi ACTIVE cũ, lưu thiết bị mới, ghi AuditRecorded outbox
  alt Hai luồng đăng ký chạy song song
    Q-->>P: 409 CONFLICT (bên thua rollback, chỉ audit CONFLICT)
  else Thành công
    Q-->>P: 201 { deviceId }
  end

  Note over P: Không cần mạng: tự ký JWS ES256 (kid=deviceId, ttl=30s, 6 claims)
  P->>K: Hiển thị QR token
  K->>G: POST /kiosk/v1/scans (attempt_id, qr_token)
  G->>Q: POST /internal/v1/qr/verify (chỉ đọc)
  Q->>Q: VerifyQrTokenUseCase: cấu trúc/PII -> kid -> ACTIVE -> sub -> ES256 -> time policy
  Q-->>G: 200 { valid, employeeUuid, jti, expiresAt }
  G->>G: Redis SET NX jti (R1) + chính sách ra vào

  Note over Q,G: Khi HR đặt nhân viên INACTIVE (PUT /hr/v1/employees/{id}/eligibility)
  Q->>Q: Transaction: lưu employees(INACTIVE) + thu hồi thiết bị + outbox audit
  G->>Q: GET /internal/v1/sync-bundle?since= (mỗi 30s)
  Q-->>G: deviceKeys (revoked=true) + employees (status=INACTIVE)
```

*Hình 13.1.2 Sequence diagram luồng đăng ký thiết bị, ký QR động và đồng bộ thu hồi (Team C)*

---

### 1.3. Sequence Diagram — Đăng nhập nhân sự có MFA TOTP, Kiểm tra JWT qua JWKS và Xoay khóa (C-05, C-12)

```mermaid
sequenceDiagram
  autonumber
  participant U as HR_STAFF / SYSTEM_ADMIN
  participant I as identity (:8087)
  participant S as Service tài nguyên (user-qr)

  U->>I: POST /auth/v1/login (username, password, mfaCode=null)
  I->>I: AuthenticateUseCase: kiểm khóa tài khoản (5 lần/15p) + PBKDF2
  I-->>U: 401 { status: MFA_REQUIRED } + outbox LOGIN_MFA_REQUIRED

  U->>I: POST /auth/v1/login (username, password, mfaCode=6 chữ số TOTP)
  I->>I: TotpMfaVerifier: RFC 6238 (±1 bước, chống dùng lại bước đã dùng)
  alt Mã sai hoặc đã dùng lại
    I-->>U: 401 { status: DENIED } + outbox LOGIN_FAILED(BAD_MFA_CODE)
  else Hợp lệ
    I->>I: NimbusAccessTokenIssuer ký ES256 bằng khóa hiện tại (kid mới)
    I-->>U: 200 { accessToken, expiresIn: 900 } + outbox LOGIN_SUCCEEDED
  end

  U->>S: PUT /hr/v1/employees/{id}/eligibility (Authorization: Bearer)
  S->>I: GET /.well-known/jwks.json (cache 5 phút)
  I-->>S: { keys: [khóa hiện tại, khóa trước (nếu đang xoay C-12)] }
  S->>S: JwtPrincipalVerifier chọn khóa theo kid, kiểm iss/aud/exp/roles
  S->>S: Rbac.requireRole(HR_STAFF) -> lưu DB + outbox
  S-->>U: 200 EmployeeEligibilityView
```

*Hình 13.1.3 Sequence diagram luồng MFA TOTP, xác thực Bearer JWKS và xoay khóa ký (Team C)*

---

### 1.4. Cập nhật Module View & Deployment View (Phần Team C)

| Góc nhìn | Thay đổi chi tiết trong mã hiện tại |
| --- | --- |
| **Module View** | - **`services/user-qr`** (`vn.tmc.userqr`): `domain` (`Device`, `DeviceStatus`, `QrClaims`, `TokenVerificationPolicy`); `application.port.in` (`IDeviceManagement`, `IEmployeeEligibility`, `IQRValidation`, `ISyncBundle`); `application.port.out` (`IDeviceKeyRegistry`, `IEmployeeDirectory`, `ITokenCodec`, `IAuditLog`, `ITransactionRunner`, `ActiveDeviceConflictException`); `adapter.out.persistence` (`JdbcDeviceKeyRegistry`, `JdbcEmployeeDirectory`, `SpringTransactionRunner`); `adapter.out.messaging` (`OutboxAuditLog`).<br/>- **`services/identity`** (`vn.tmc.identity`): `domain` (`Account`); `application.usecase` (`AuthenticateUseCase`, `LoginUseCase`); `adapter.out.crypto` (`NimbusAccessTokenIssuer`, `SigningKeys`, `TotpMfaVerifier`); `adapter.in.rest` (`AuthController`, `JwksController`); `adapter.out.messaging` (`OutboxAuditLog`).<br/>- **`platform/security`**: thư viện dùng chung (`PrincipalResolver`, `JwtPrincipalVerifier`, `AccessTokenSigner`, `AccessTokenContract`, `Rbac`, `Role`, `DataScope`), có `ArchitectureTest` ở mọi service kiểm tra không có phụ thuộc ngược hay import chéo giữa các service. |
| **Deployment View (C-11, C-02, ADR-12)** | - **Điện thoại / Trình duyệt:** giữ khóa riêng EC P-256 không thể xuất (`IndexedDB` trên web thử nghiệm; keystore/enclave trên app gốc).<br/>- **PostgreSQL Central (`:5433`):** database `user_qr` (`devices`, `outbox_events`, `employees` qua Flyway `V1`–`V3`) và database `identity` (`outbox_events` qua Flyway `V1`, profile mặc định C-11).<br/>- **Edge Node / Kiosk:** giữ bản sao khóa công khai thiết bị và danh bạ đủ điều kiện kéo từ `/internal/v1/sync-bundle`; `user-qr` hiện triển khai endpoint `/internal/v1/qr/verify` tại cùng service `user-qr` theo mô hình logic Edge (`implementation-notes.md` mục 1 điểm 6). |

---

## 2. Phần Team A, B, D *(chờ ghép khi hợp nhất D-12)*

- **Team A:** Sơ đồ luồng quét QR và triển khai tại biên (`15.2`, A-04 `gate_store`).
- **Team B:** Sơ đồ outbox, phục hồi `AccessAttempt` và đối soát `GATE_UNKNOWN`.
- **Team D:** Sơ đồ tích hợp HR/Payroll và đối soát (`D-06`, `D-04`, `D-08`).
