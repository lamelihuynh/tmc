# Cập nhật Các góc nhìn Kiến trúc (Task 1.2)

> Phần này trả lời yêu cầu **Task 1.2** của đề bài M2: cập nhật Module View, Component & Connector View và Allocation/Deployment View phản ánh thay đổi từ ADR-11, ADR-12 và ADR-13. Tệp gồm **sơ đồ bảo mật và danh tính của Team C** (R-C) và **máy trạng thái lượt quét cùng luồng Outbox / Exchange `tmc.events` (ADR-15) của Team B**; sơ đồ của Team A (luồng quét QR 15.2) và Team D (HR) sẽ được hợp nhất tại D-12.

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

## 2. Sơ đồ máy trạng thái của một lượt quét — `AccessAttempt` (Team B)

Nguồn sự thật trong mã là enum `AttemptStatus` và aggregate `AccessAttempt` (`services/attendance/src/main/java/vn/tmc/attendance/domain/`). Khác với bản nháp đầu của ADR-11:
- `RECEIVED` chỉ tồn tại trong SQLite Journal tại kiosk.
- `PUBLISHED` là `publish_status` của bảng `outbox_events`, **không** nằm trong `AttemptStatus`.
- `GATE_FAILED` là trạng thái kết thúc (không đi qua `COMMITTED`).

```mermaid
stateDiagram-v2
  [*] --> VALIDATED: Edge nhận lượt đã xác thực
  VALIDATED --> RESERVED: Giữ chỗ jti và slot anti-passback
  VALIDATED --> DENIED: Chính sách hoặc QR từ chối
  RESERVED --> GATE_COMMANDED: Ghi trước khi gửi lệnh (write-ahead)
  RESERVED --> DENIED: Quá TTL giữ chỗ (SYSTEM_RETRY)
  GATE_COMMANDED --> GATE_OPENED: Turnstile xác nhận OPENED
  GATE_COMMANDED --> GATE_FAILED: Lỗi cứng (ERROR)
  GATE_COMMANDED --> GATE_UNKNOWN: TIMEOUT hoặc crash sau lệnh
  GATE_UNKNOWN --> GATE_OPENED: Đối soát thấy cổng đã mở
  GATE_UNKNOWN --> GATE_FAILED: Đối soát thấy cổng chưa mở
  GATE_OPENED --> COMMITTED: Lưu công + Outbox cùng transaction
  DENIED --> [*]
  GATE_FAILED --> [*]
  COMMITTED --> [*]
```

*Hình 12.7.1 — Máy trạng thái giao dịch tại cổng (`AccessAttempt`), khớp `docs/architecture/state-machine.md`.*

### Bảng sự kiện phát ra theo bước chuyển trạng thái

| Chuyển trạng thái | Sự kiện vào `outbox_events` | Ghi chú hợp đồng (`attendance-events.asyncapi.yaml`) |
| --- | --- | --- |
| Tạo `RESERVED` | `AccessAttemptRecorded` | Chưa đủ điều kiện gửi HR |
| `DENIED` do chính sách, QR hoặc quá TTL giữ chỗ (`SYSTEM_RETRY`) | `AuditRecorded` | `action = "ACCESS_DENIED"` hoặc `"RESERVATION_RELEASED"` |
| `GATE_COMMANDED` | Không phát sự kiện | Chỉ là điểm ghi trước (write-ahead) |
| `GATE_OPENED`, `GATE_FAILED`, `GATE_UNKNOWN` | `GateResultRecorded` | Giữ nguyên `gateResult` (`OPENED`, `ERROR`, `UNKNOWN`) |
| `COMMITTED` (nguồn `ONLINE`/`OFFLINE`, không bất thường) | `AttendanceConfirmed` | Dữ liệu đủ điều kiện đồng bộ HR |
| `COMMITTED` với nguồn `MANUAL` | `AuditRecorded` | `action = "MANUAL_OPEN_PENDING_VERIFICATION"`, không tự tạo `AttendanceTransaction` |
| Nhập offline có bất thường | `AuditRecorded` | `action = "TOKEN_REUSED_OFFLINE"` hoặc `"ANTI_PASSBACK_OFFLINE"`, không tính công |

---

## 3. Kiến trúc Outbox và Exchange dùng chung `tmc.events` (Team B, ADR-15)

Để bảo đảm phát sự kiện ra trung tâm không mất và không trùng khi mạng WAN hoặc RabbitMQ gián đoạn:
- **Ghi nguyên tử trong `attendance`:** Các use case (`AttendanceCommandService`, `GateResultApplier`, `IngestOfflineBatchUseCase`, `RecoverStaleAttemptsUseCase`, `ReconcileUnknownUseCase`) ghi thay đổi của `access_attempts` / `attendance_transactions` và chèn bản ghi JSONB vào bảng `outbox_events` (`publish_status = 'PENDING'`, thứ tự `seq BIGSERIAL`) trong **cùng một transaction** qua `SpringTransactionRunner`.
- **Phát với xác nhận từ broker:** `MaintenanceScheduler` kích hoạt `PublishOutboxUseCase.run()` mỗi 1 giây, lấy các dòng `PENDING` theo thứ tự `seq` và gọi `AmqpEnvelopePublisher` đẩy lên một **Topic Exchange bền duy nhất là `tmc.events`** (`TmcExchange.NAME`) với khóa định tuyến **`attendance.<EventType>`** (`TmcExchange.ATTENDANCE`), chờ `ConfirmType.CORRELATED` (trường `source` bên trong envelope là `"attendance-edge"` theo `AttendanceEventFactory`). Chỉ khi broker xác nhận ACK mới cập nhật `publish_status = 'PUBLISHED'`; khi gặp lỗi, tăng `failure_count` và dừng ngay vòng lặp để bảo toàn thứ tự.
- **Khử trùng phía tiêu thụ:** Mỗi service phía Central có hàng đợi bền riêng (`tmc.reporting.attendance`, `tmc.hr.attendance`, `tmc.audit.events`) và Dead-Letter Queue (`<queue>.dlq`, thử lại 3 lần), dùng `IdempotentConsumer` và `JdbcProcessedMessageStore` (bảng `processed_messages` trong cùng transaction với handler). Hợp đồng `attendance.AttendanceCorrected` đã chốt trong `attendance-events.asyncapi.yaml` và `AttendanceEventFactory.attendanceCorrected(...)` (phần use case/endpoint thuộc B-06).

```mermaid
flowchart LR
  subgraph Edge["Edge: attendance (:8082)"]
    UC["Use Cases<br/>(Command / Reconcile / OfflineIngest)"]
    TX["SpringTransactionRunner<br/>(1 PostgreSQL Transaction)"]
    DB[("PostgreSQL Edge<br/>access_attempts<br/>attendance_transactions<br/>outbox_events (PENDING -> PUBLISHED)")]
    PUB["MaintenanceScheduler (1s)<br/>PublishOutboxUseCase<br/>AmqpEnvelopePublisher (Publisher Confirm)"]
    UC --> TX --> DB
    DB -- "fetchPending ORDER BY seq" --> PUB
  end

  EX[["RabbitMQ Topic Exchange<br/>tmc.events"]]
  PUB -- "routing key: attendance.&lt;EventType&gt;" --> EX

  subgraph Central["Central Consumers (processed_messages + DLQ)"]
    REP["reporting<br/>queue: tmc.reporting.attendance<br/>bind: attendance.*"]
    HR["hr-integration<br/>queue: tmc.hr.attendance<br/>bind: attendance.AttendanceConfirmed<br/>attendance.AttendanceCorrected"]
    AUD["audit-admin<br/>queue: tmc.audit.events<br/>bind: *.AuditRecorded"]
  end

  EX --> REP
  EX --> HR
  EX --> AUD
```

*Hình 12.7.2 — Luồng Transactional Outbox và định tuyến sự kiện trên exchange `tmc.events` (ADR-15).*

---

## 4. Phần Team A, D *(chờ ghép khi hợp nhất D-12)*

- **Team A:** Sơ đồ luồng quét QR và triển khai tại biên (`15.2`, A-04 `gate_store`).
- **Team D:** Sơ đồ tích hợp HR/Payroll và đối soát (`D-06`, `D-04`, `D-08`).
