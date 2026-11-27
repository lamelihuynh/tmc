# Chỉ mục quyết định kiến trúc (ADR)

ADR-01 đến ADR-10 được **chấp thuận ở M1** và mô tả đầy đủ trong báo cáo `docs/document.md` mục 7 (bảng 7.1.1). Ở đây chỉ tóm tắt và chỉ ra chúng nằm ở đâu trong mã. ADR-11 đến ADR-16 là mới cho M2 và có tệp riêng trong thư mục này.

| ADR | Quyết định | Trạng thái | Nơi trong mã | Đã có test chứng minh |
| --- | --- | --- | --- | --- |
| 01 | Microservices theo bounded context | Accepted (M1) | `services/*`, mỗi service một module Gradle | `ArchitectureTest.should_respect_hexagonal_dependency_rules_and_not_import_other_services` ở mọi service |
| 02 | Event-Driven cho tác vụ không cần phản hồi ngay | Accepted (M1) | `AttendanceEvents`; `attendance` phát, `reporting`, `hr-integration`, `audit-admin` nhận | `Adr11ResilienceTest`, `ProjectionTest`, `SyncAttendanceEventTest`; AMQP thật: `MessagingInfraIT`, `AttendanceEventListenerIT`, `HrAttendanceListenerIT`, `AuditEventListenerIT` |
| 03 | Attendance là System of Record | Accepted (M1) | `services/attendance`; `AttendanceTransaction` duy nhất theo `attempt_id` | `Adr11ResilienceTest` |
| 04 | Transactional Outbox + Message Broker | Accepted (M1) | `IOutboxRepository`, `PublishOutboxUseCase`, `AmqpEnvelopePublisher`, `OutboxStore`/`OutboxRelay` (platform/messaging), bảng `outbox_events` | `Adr11ResilienceTest.should_keep_events_pending_while_the_broker_is_down_...`; `PersistenceAndBrokerIT` (cần Docker) |
| 05 | Idempotency cho attendance event | Accepted (M1), **mở rộng bởi ADR-11** | `attempt_id` xuyên suốt; `IdempotentConsumer`; unique `attempt_id` | `RecordScanUseCaseTest`, `IdempotentConsumerTest` |
| 06 | Turnstile qua Adapter / ACL | Accepted (M1) | `ITurnstileControlPort`, `HttpTurnstileAdapter` | `HttpTurnstileAdapterTest` |
| 07 | HR/Payroll qua Integration Service | Accepted (M1) | `services/hr-integration`, `IHrPayrollPort` | `SyncAttendanceEventTest` (adapter HTTP: D-06) |
| 08 | Identity tập trung + RBAC + audit | Accepted (M1) | `platform/security`, `Rbac`, `services/identity`, `services/audit-admin` | `RbacTest`, `AuthenticationTest`, `AuditTest`, `JwtPrincipalVerifierTest`, `LoginApiTest`, `BearerTokenApiTest` |
| 09 | Privacy by Design | Accepted (M1), **mở rộng bởi ADR-12** | `NimbusTokenCodec` (từ chối claim lạ), `AttendanceEventFactory` | xem `docs/architecture/privacy-notes.md` |
| 10 | Database per service + read model cho báo cáo | Accepted (M1) | `docker/init/**`, mỗi service schema riêng, `reporting` có read model | `ProjectionTest` (read model JDBC: D-04); database riêng của Gate Station: ADR-14 |
| **11** | Offline-first, store-and-forward, máy trạng thái giao dịch | **Accepted (M2, 16/11)** | `AccessAttempt`, `RecordScanUseCase`, `KioskApp`, `Journal` | `Adr11ResilienceTest`, `RecordScanUseCaseTest`, `KioskAppTest`; demo `make demo-timeout`, `make demo-offline` |
| **12** | QR động ký bằng khóa thiết bị, chống replay nhiều lớp, bỏ QR tĩnh | **Accepted (M2, 16/11)** | `VerifyQrTokenUseCase`, `DeviceManagementService`, `OfflineVerifier`, `TokenSigner` | `Adr12TokenVerificationTest`, `DeviceAndVerifyApiTest`, `KioskAppTest`, `RedisAdaptersIT`; demo `make demo-online` |
| **13** | Gate Station điều phối luồng quét tại biên | **Accepted (M2, 16/11)** | `RecordScanUseCase`, `AccessPolicyChain`, `SyncEmployeeDirectoryUseCase` | `RecordScanUseCaseTest`, `AccessPolicyTest`, `EmployeeSyncTest` |
| **14** | Gate Station có database riêng `gate_store` trên PostgreSQL của Edge (profile `gate-store`) | **Accepted (M2, 16/11)** | `GateStoreConfig`, `db/gate-store/V1__gate_store.sql`, `docker/init/edge/02-gate-store.sql` | `GateStoreIT` |
| **15** | Exchange dùng chung `tmc.events` (khóa `<nguồn>.<EventType>`) và outbox dùng chung trong `platform/messaging` | **Accepted (M2, 16/11)** | `TmcExchange`, `OutboxStore`, `OutboxRelay`, `AmqpEnvelopePublisher`, `JdbcProcessedMessageStore`, `AuditOutbox` | `MessagingInfraIT`, `OutboxRelayTest`, IT của 3 consumer |
| **16** | Nhật ký audit chỉ thêm, chuỗi băm, login chỉ INSERT | **Accepted (M2, 27/11)** | `AuditHashChain`, `JdbcAuditStore`, `db/migration/V2__audit_entries.sql` (audit-admin), `docker/init/central/01-databases.sql` | `AuditTest`, `AuditStoreIT` |

Các tệp: [ADR-11](ADR-11-offline-first-store-and-forward.md), [ADR-12](ADR-12-dynamic-qr-device-binding.md), [ADR-13](ADR-13-edge-access-orchestrator.md), [ADR-14](ADR-14-gate-store-database.md), [ADR-15](ADR-15-shared-event-exchange-and-outbox.md), [ADR-16](ADR-16-audit-hash-chain.md). Tóm tắt thay đổi so với M1: [README.md](README.md).

## Chỗ mã khác ADR

ADR-11, 12, 13 đã chuyển sang **Accepted** ngày 16/11/2026 theo hướng **ADR khớp mã** cho các điểm 1 đến 6 của `docs/architecture/implementation-notes.md` mục 1; mỗi ADR có mục "Ghi chú khi chấp thuận" nêu điều chỉnh và đánh đổi. Thân ADR gốc giữ nguyên để thấy quá trình; khi đọc, mục ghi chú thắng phần thân.

## ADR tiếp theo

Số kế tiếp: **17**. Ứng viên đã thấy cần: cách xử lý lượt mở thủ công chờ xác minh (B-07), mô hình xác thực service-to-service (C-06).
