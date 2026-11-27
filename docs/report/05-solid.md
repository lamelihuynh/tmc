# Áp dụng Nguyên tắc SOLID trong Mã nguồn (Task 1.3)

> Phần này trả lời yêu cầu **Task 1.3** của đề bài M2: chứng minh cách các nguyên tắc **SOLID** được hiện thực cụ thể trong mã nguồn, mỗi khẳng định đều gắn với **lớp thật** và **bài kiểm thử chạy được** trong repo (không trích dẫn test `@Disabled`). Tệp gồm phần của **Team C** (`services/user-qr`, `services/identity`, `platform/security`) và **Team B** (`services/attendance`) theo [`docs/architecture/solid-map.md`](../architecture/solid-map.md), và **Team D** (`services/reporting`, `services/hr-integration`, `services/audit-admin`, mục 3); phần của Team A sẽ được hợp nhất tại D-12.

---

## 1. Tổng hợp áp dụng SOLID của Team C

### S — Single Responsibility Principle (Đơn trách nhiệm)

| Trách nhiệm tách biệt | Lớp thật trong mã | Lý do thay đổi duy nhất | Bằng chứng (Test chạy xanh) |
| --- | --- | --- | --- |
| **Quy tắc thời gian & tuổi thọ QR token** | `TokenVerificationPolicy` (`services/user-qr/domain`) | Chính sách bảo mật QR (`maxLifetime = 60s`, `clockSkew = 60s`), tách biệt hoàn toàn khỏi phân tích JWS và lưu trữ thiết bị | `Adr12TokenVerificationTest` (nhóm test 6), `QrTokenPolicyContractTest` |
| **Phân tích định dạng JWS & chặn PII** | `NimbusTokenCodec` (`services/user-qr/adapter/out/crypto`) | Chi tiết thư viện `nimbus-jose-jwt` và quy tắc 6 claim được phép của ADR-09 | `Adr12TokenVerificationTest.should_reject_a_token_that_carries_a_pii_claim` |
| **Điều phối xác thực QR (chỉ đọc)** | `VerifyQrTokenUseCase` (`services/user-qr/application/usecase`) | Thứ tự fail-secure khi xác thực QR; không ôm trách nhiệm đánh dấu token đã dùng (thuộc `gate-station`) | `Adr12TokenVerificationTest` (14 ca) |
| **Bất biến khóa tài khoản & yêu cầu MFA** | `Account` (`services/identity/domain`) | Quy tắc NFR-03 (khóa 15 phút sau 5 lần sai, vai trò bắt buộc MFA) | `AuthenticationTest` |
| **Điều phối đăng nhập & phát hành JWT** | `AuthenticateUseCase` tách khỏi `LoginUseCase` (`services/identity/application/usecase`) | `AuthenticateUseCase` chỉ kiểm thông tin xác thực + ghi audit; `LoginUseCase` chỉ bổ sung cấp JWT khi `GRANTED` | `AuthenticationTest`, `LoginApiTest` |

### O — Open/Closed Principle (Đóng với sửa đổi, mở với mở rộng)

- **Thay thế bộ kiểm tra MFA mà không sửa `AuthenticateUseCase` (trọng tâm C-05):**
  `AuthenticateUseCase` chỉ phụ thuộc vào `@FunctionalInterface IMfaVerifier`. Khi hoàn thành story **C-05**, nhóm chỉ cần thêm lớp adapter mới **`TotpMfaVerifier`** (hiện thực RFC 6238, cửa sổ `±1` bước, chống replay) và đổi bean `mfa(...)` trong `IdentityConfig` từ bản giả `(u, code) -> false` sang `TotpMfaVerifier` **mà không sửa bất kỳ dòng nào của `AuthenticateUseCase`**.
  - *Bằng chứng:* `TotpMfaVerifierTest.should_work_with_authenticate_use_case_for_hr_staff_and_system_admin`, `TotpLoginApiTest`.
- **Mở rộng cơ chế lưu trữ và audit qua Spring profile:**
  `DeviceManagementService`, `EmployeeEligibilityService` và `BuildSyncBundleUseCase` không đổi khi chuyển từ `inmem`/`local` (`InMemoryDeviceKeyRegistry`, `InMemoryEmployeeDirectory`, `LoggingAuditLog`) sang PostgreSQL + RabbitMQ ở profile mặc định (`JdbcDeviceKeyRegistry`, `JdbcEmployeeDirectory`, `OutboxAuditLog`).
  - *Bằng chứng:* `SyncBundleTest` (in-memory) và `EmployeeDirectoryIT`, `DeviceAuditOutboxIT` (PostgreSQL).

### L — Liskov Substitution Principle (Thay thế Liskov)

| Port (Hợp đồng) | Các cài đặt thay thế lẫn nhau | Điều khoản hợp đồng bắt buộc giữ nguyên | Bằng chứng |
| --- | --- | --- | --- |
| **`IDeviceKeyRegistry`** | `InMemoryDeviceKeyRegistry` ↔ `JdbcDeviceKeyRegistry` | Cả hai đều bảo toàn bất biến tối đa 1 thiết bị `ACTIVE`/nhân viên và cùng ném `ActiveDeviceConflictException` khi xung đột (C-01), thay vì rò rỉ `DuplicateKeyException` của Spring | `DeviceRegistrationConflictTest`, `UserQrPersistenceIT`, `ConcurrentDeviceRegistrationIT` |
| **`IEmployeeDirectory`** | `InMemoryEmployeeDirectory` ↔ `JdbcEmployeeDirectory` | Cả hai đều giữ bản ghi `INACTIVE` (không xóa), cập nhật theo quy tắc `updatedAt` mới hơn thắng, không ghi đè khi `saveIfAbsent`, và trả `changedSince` cùng thứ tự `(updatedAt, employeeUuid)` | `SyncBundleTest`, `EmployeeDirectoryIT` |
| **`IAuditLog`** | `LoggingAuditLog` ↔ `OutboxAuditLog` | Cả hai đều ghi audit chỉ bằng định danh/mã lý do, không làm rò rỉ bí mật hoặc khóa công khai | `DeviceAuditOutboxIT`, `LoginAuditApiTest`, `IdentityOutboxIT` |

### I — Interface Segregation Principle (Phân tách giao diện)

Thay vì gộp mọi thao tác nhân viên và thiết bị vào một interface hoặc controller lớn, Team C chia nhỏ theo từng nhóm tác nhân gọi vào (`docs/conventions/api-design-guidelines.md`):
- **Driving ports của `user-qr`:** `IDeviceManagement` (tự đăng ký/thu hồi & HR thu hồi thiết bị), `IEmployeeEligibility` (chỉ HR cập nhật trạng thái/nhà máy — C-02), `IQRValidation` (nội bộ xác thực QR), `ISyncBundle` (Edge kéo đồng bộ).
- **REST Controllers tách rời:** `DeviceController` (`/self-service/v1/devices/**` & thu hồi), `EmployeeEligibilityController` (`/hr/v1/employees/{id}/eligibility`), `QrVerifyController` (`/internal/v1/**`). Một client nội bộ gọi `QrVerifyController` không bị ép phụ thuộc vào các hàm quản trị thiết bị hay cập nhật HR.
- *Bằng chứng:* `DeviceAndVerifyApiTest`, `EmployeeEligibilityApiTest`, `BearerTokenApiTest`.

### D — Dependency Inversion Principle (Đảo ngược phụ thuộc)

- Mọi luồng nghiệp vụ cốt lõi của Team C đều nằm ở `domain` và `application.usecase`, chỉ phụ thuộc vào **port do chính `application` định nghĩa**:
  - `VerifyQrTokenUseCase` chỉ biết **`ITokenCodec`** và `IDeviceKeyRegistry`, không phụ thuộc thư viện `com.nimbusds.jose` nằm trong `NimbusTokenCodec`.
  - `DeviceManagementService` và `EmployeeEligibilityService` chỉ biết **`ITransactionRunner`**, **`IAuditLog`**, **`IClock`** — hoàn toàn không import `org.springframework.*` hay `JdbcTemplate`.
  - `AuthenticateUseCase` chỉ biết `IAccountRepository`, `IPasswordVerifier`, **`IMfaVerifier`**, `IAuditLog`, `IClock`.
- Điểm nối duy nhất (Composition Root) là lớp cấu hình trong gói `bootstrap` (`UserQrConfig`, `IdentityConfig`, `OutboxConfig`).
- *Bằng chứng tự động:* `ArchitectureTest` ở cả `services/user-qr` và `services/identity` chạy `HexagonalRules.verifyAll(...)` kiểm tra trực tiếp bytecode bằng ArchUnit; toàn bộ test tầng `application` chạy thuần Java không khởi động Spring hay CSDL.

### Nhận xét trung thực về giới hạn áp dụng SOLID trong mã Team C
- `IAccountRepository` và `IPasswordVerifier` của `identity` hiện mới chỉ có cài đặt trong bộ nhớ (`InMemoryAccountRepository`, `Pbkdf2PasswordVerifier`), và `TotpMfaVerifier` giữ bí mật trong bộ nhớ thay vì một `ITotpSecretStore` bền vững trên PostgreSQL (do story vòng đời tài khoản C-03/C-07 đã được cắt khỏi phạm vi đợt này). Việc tách sẵn các port giúp bổ sung adapter JDBC sau này mà không phải sửa `AuthenticateUseCase`.

---

## 2. Phần của Team B (`services/attendance`)

| Nguyên tắc | Lớp / Port trong `attendance` | Lý giải thiết kế | Bằng chứng kiểm thử |
| --- | --- | --- | --- |
| **S — Single Responsibility** | `GateResultApplier`, `AttendanceEventFactory`, `PublishOutboxUseCase` | `GateResultApplier` có một lý do thay đổi duy nhất: quy tắc chuyển đổi kết quả từ cổng (`OPENED`, `ERROR`, `UNKNOWN`) thành chuyển trạng thái `AccessAttempt`, bản ghi công `AttendanceTransaction` và sự kiện outbox. Cả luồng online (`AttendanceCommandService`) và luồng đối soát nền (`ReconcileUnknownUseCase`) đều dùng chung lớp này để không bị trôi lệch quy tắc. `AttendanceEventFactory` tách riêng trách nhiệm dựng payload không chứa dữ liệu cá nhân (`jti`, tên). | `Adr11ResilienceTest`, `ManualOpenTest`, `Adr11ResilienceTest.should_not_leak_the_token_id_or_any_name_into_event_payloads` |
| **O — Open/Closed** | `IAttemptRepository`, `IOutboxRepository`, `ITransactionRunner`, `EventPublisher` | Có thể thay đổi hoặc mở rộng hạ tầng lưu trữ và truyền thông điệp (từ bản in-memory sang JDBC PostgreSQL và RabbitMQ thật, hoặc thêm adapter mới) bằng cấu hình profile (`InMemoryProfileConfig` ↔ `InfrastructureProfileConfig`) mà không sửa mã nghiệp vụ trong `application/usecase`. | `InternalApiFlowTest` (inmem), `PersistenceAndBrokerIT` (PostgreSQL), `OutboxBrokerRestartIT` (RabbitMQ) |
| **L — Liskov Substitution** | `InMemoryAttemptRepository` ↔ `JdbcAttemptRepository`; `InMemoryOutboxRepository` ↔ `JdbcOutboxRepository`; `InMemoryEventBroker` ↔ `AmqpEnvelopePublisher` | Hai bộ cài đặt giữ nguyên hợp đồng: `save` upsert theo `attempt_id`, `saveIfAbsent` chống trùng, `fetchPending` trả về theo thứ tự ghi và ẩn dòng đã `PUBLISHED`, `publish` chỉ thành công khi broker xác nhận và ném `PublishFailedException` khi broker lỗi. | `Adr11ResilienceTest` (inmem), `PersistenceAndBrokerIT` & `OutboxBrokerRestartIT` (JDBC/RabbitMQ) |
| **I — Interface Segregation** | `IAttendanceCommand`, `IAttendanceQuery`, `IOfflineAttendanceIngest` | Chia nhỏ port vào theo vai trò người gọi: `gate-station` gọi ghi qua `IAttendanceCommand` và nhập lô qua `IOfflineAttendanceIngest`; luồng chỉ đọc (`findAttempt`, `lastPass` cho anti-passback, `anomalies` cho HR) chỉ phụ thuộc vào `IAttendanceQuery`, hoàn toàn không thấy và không thể gọi bất kỳ hàm ghi hay sửa trạng thái nào. | `InternalAttendanceController`, `HrAttendanceController`, `AnomalyQueueTest` |
| **D — Dependency Inversion** | `ITransactionRunner`, `EventPublisher`, `IAttemptRepository`, `IOutboxRepository`, `IGateStatusQuery`, `IClock` | Tất cả interface port ra đều nằm trong gói `vn.tmc.attendance.application.port.out` (hoặc `platform/`), do tầng nghiệp vụ định nghĩa. Tầng `domain` và `application/usecase` không phụ thuộc vào Spring, JDBC, RabbitMQ hay HTTP; việc nối `SpringTransactionRunner`, `AmqpEnvelopePublisher`, `Jdbc*Repository` chỉ thực hiện tại `bootstrap/`. Nhờ đó toàn bộ use case chạy unit test độc lập không cần Spring, DB hay broker. | `ArchitectureTest` (`HexagonalRules.verifyAll(...)`), `Adr11ResilienceTest`, `AnomalyQueueTest`, `OperationalMetricsTest` |

---

## 3. Phần của Team D (`services/reporting`, `services/hr-integration`, `services/audit-admin`)

Ba service cùng một khuôn lục giác: listener `@RabbitListener` bọc `IdempotentConsumer` (`platform/messaging`), gọi một use case; use case chỉ biết port do chính nó định nghĩa; adapter được chọn ở `bootstrap/` theo profile hoặc cấu hình.

| Nguyên tắc | Lớp / Port | Lý giải thiết kế | Bằng chứng kiểm thử |
| --- | --- | --- | --- |
| **S — Single Responsibility** | `AuditHashChain` (`services/audit-admin/src/main/java/vn/tmc/auditadmin/domain/AuditHashChain.java`), `JdbcAuditStore` (`.../adapter/out/persistence/JdbcAuditStore.java`), `HttpHrPayrollAdapter` (`services/hr-integration/src/main/java/vn/tmc/hrintegration/adapter/out/http/HttpHrPayrollAdapter.java`) | `AuditHashChain` chỉ đổi khi quy tắc băm đổi (dạng chuẩn của bản ghi, `SHA-256(prev_hash ‖ canonical)`, kiểm lại chuỗi); `JdbcAuditStore` chỉ lo lưu và tuần tự hóa bằng `pg_advisory_xact_lock`. `HttpHrPayrollAdapter` chỉ lo dịch câu trả lời HTTP sang kết quả của port; quy tắc "tra cứu trước khi gửi lại" nằm ở `SyncAttendanceEventUseCase`. | `AuditTest.should_detect_a_modified_entry_through_the_hash_chain`, `AuditStoreIT`, `HttpHrPayrollAdapterTest.should_map_each_http_status_to_what_it_says_about_the_batch` |
| **O — Open/Closed** | `FraudRule` (`services/reporting/src/main/java/vn/tmc/reporting/domain/FraudRule.java`), `ImpossibleTravelRule` | `ProjectAttendanceEventUseCase` nhận `List<FraudRule>` và chỉ gọi `evaluate`. Luật R4 di chuyển bất khả thi được thêm bằng một lớp mới và một phần tử trong bean `fraudRules` của `ReportingConfig`, không sửa use case; luật lặp từ chối (chưa làm, xem 08) sẽ thêm theo cùng cách. | `ProjectionTest.should_flag_two_entries_in_different_factories_ten_minutes_apart`, `should_not_flag_entries_in_two_factories_further_apart_than_the_travel_time` |
| **L — Liskov Substitution** | `IHrPayrollPort` (`services/hr-integration/src/main/java/vn/tmc/hrintegration/application/port/out/IHrPayrollPort.java`): `SimulatedHrPayrollAdapter` ↔ `HttpHrPayrollAdapter` | Hai adapter trả cùng tập kết quả của hợp đồng: `submit` → `ACCEPTED`, `REJECTED`, `REFUSED`, `TIMEOUT`; `lookup` → `ACCEPTED`, `REJECTED`, `NOT_FOUND`, `UNKNOWN`. Use case xử lý `TIMEOUT` là "chưa biết" bất kể adapter nào, nên thay bản giả lập bằng bản HTTP chỉ là đổi `tmc.hr.mode`. | `SyncAttendanceEventTest`, `HrRetryTest` (use case với `SimulatedHrPayrollAdapter`); `HttpHrPayrollAdapterTest` (bản HTTP với máy chủ HTTP giả); `HrIntegrationConfigTest.should_select_simulated_adapter_by_default`, `should_select_http_adapter_when_http_mode_is_enabled` |
| **L — Liskov Substitution** | `IPresenceRepository` (`services/reporting/src/main/java/vn/tmc/reporting/application/port/out/IPresenceRepository.java`): `InMemoryPresenceRepository` ↔ `JdbcPresenceRepository`; tương tự `IAuditStore`: `InMemoryAuditStore` ↔ `JdbcAuditStore`, `ISyncLogRepository`: `InMemorySyncLogRepository` ↔ `JdbcSyncLogRepository` | Cả hai bản giữ cùng hợp đồng: `upsertScan` chỉ thay khi phiên bản mới hơn, hiện diện là lượt có `scannedAt` mới nhất; `append` bỏ qua bản ghi đã có và nối chuỗi băm. Bản bộ nhớ dùng cho profile `local`, `inmem` và test đơn vị; bản JDBC cho profile mặc định. | `ProjectionTest` (bộ nhớ) và `PresencePersistenceIT` (PostgreSQL); `AuditTest` và `AuditStoreIT`; `HrRetryTest` và `SyncLogPersistenceIT` |
| **I — Interface Segregation** | `IAuditStore` (`services/audit-admin/src/main/java/vn/tmc/auditadmin/application/port/out/IAuditStore.java`); `IAuditQuery`, `IReporting`, `ISyncMonitoring` | Port lưu audit chỉ có `append`, `search`, `count`, `verifyChain`: **không có** hàm sửa hay xóa, nên không client nào phụ thuộc vào thao tác mà nhật ký chỉ thêm không được có. Phía đọc tách riêng: REST chỉ thấy `IAuditQuery`, `IReporting`, `ISyncMonitoring`, không thấy use case ghi. | `AuditTest.should_have_no_way_to_change_or_remove_history_through_the_store_port` (kiểm bằng reflection: không phương thức public nào bắt đầu bằng `update`, `delete`, `remove`, `clear`); `AuditStoreIT.should_let_the_service_login_insert_and_read_but_never_change_history` chặn thêm ở database |
| **D — Dependency Inversion** | `SyncAttendanceEventUseCase`, `PayrollDeadlineCheck`, `ProjectAttendanceEventUseCase`, `RecordAuditUseCase` | Use case chỉ phụ thuộc port do tầng `application` định nghĩa (`IHrPayrollPort`, `ISyncLogRepository`, `IPresenceRepository`, `IFraudAlertStore`, `IAuditStore`), cộng `IClock` của `platform/common-kernel` ở `hr-integration`; không import Spring, JDBC, RabbitMQ hay HTTP. Nhờ `IClock` giả, cảnh báo hạn chót 08:00 và 12:00 ngày 1 được test mà không chờ đồng hồ thật. Nối adapter chỉ ở `HrIntegrationConfig`, `ReportingConfig`, `AuditAdminConfig`. | `ArchitectureTest` ở cả ba service (`HexagonalRules`); `SyncAttendanceEventTest.should_raise_a_deadline_alert_before_noon_on_the_first_day_of_the_month` |

**Nhận xét trung thực:** use case HR chỉ được test với `SimulatedHrPayrollAdapter`; `HttpHrPayrollAdapter` được test riêng với máy chủ HTTP giả, chưa có test chạy use case với bản HTTP trên một HR thật hay giả lập đầy đủ. `FraudRule` hiện mới có một cài đặt, nên tính "mở để mở rộng" mới được chứng minh bằng cấu trúc, chưa bằng cài đặt thứ hai.

---

## 4. Phần Team A *(chờ ghép khi hợp nhất D-12)*

- **Team A (`services/gate-station`):** `RecordScanUseCase`, `AccessPolicyChain`, `ITurnstileControlPort`. *[Chờ Team A]*
