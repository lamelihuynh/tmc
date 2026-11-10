# Chỉ mục quyết định kiến trúc (ADR)

ADR-01 đến ADR-10 được **chấp thuận ở M1** và mô tả đầy đủ trong báo cáo `docs/document.md` mục 7 (bảng 7.1.1). Ở đây chỉ tóm tắt và chỉ ra chúng nằm ở đâu trong mã. ADR-11 đến ADR-13 là mới cho M2 và có tệp riêng trong thư mục này.

| ADR | Quyết định | Trạng thái | Nơi trong mã | Đã có test chứng minh |
| --- | --- | --- | --- | --- |
| 01 | Microservices theo bounded context | Accepted (M1) | `services/*`, mỗi service một module Gradle | `ArchitectureTest.should_respect_hexagonal_dependency_rules_and_not_import_other_services` ở mọi service |
| 02 | Event-Driven cho tác vụ không cần phản hồi ngay | Accepted (M1) | `AttendanceEvents`; `attendance` phát, `reporting`, `hr-integration`, `audit-admin` nhận | `Adr11ResilienceTest`, `ProjectionTest`, `SyncAttendanceEventTest` (AMQP thật chưa kiểm chứng, B-02) |
| 03 | Attendance là System of Record | Accepted (M1) | `services/attendance`; `AttendanceTransaction` duy nhất theo `attempt_id` | `Adr11ResilienceTest` |
| 04 | Transactional Outbox + Message Broker | Accepted (M1) | `IOutboxRepository`, `PublishOutboxUseCase`, `AmqpEventPublisher`, bảng `outbox_events` | `Adr11ResilienceTest.should_keep_events_pending_while_the_broker_is_down_...`; `PersistenceAndBrokerIT` (cần Docker) |
| 05 | Idempotency cho attendance event | Accepted (M1), **mở rộng bởi ADR-11** | `attempt_id` xuyên suốt; `IdempotentConsumer`; unique `attempt_id` | `RecordScanUseCaseTest`, `IdempotentConsumerTest` |
| 06 | Turnstile qua Adapter / ACL | Accepted (M1) | `ITurnstileControlPort`, `HttpTurnstileAdapter` | `HttpTurnstileAdapterTest` |
| 07 | HR/Payroll qua Integration Service | Accepted (M1) | `services/hr-integration`, `IHrPayrollPort` | `SyncAttendanceEventTest` (adapter HTTP: D-06) |
| 08 | Identity tập trung + RBAC + audit | Accepted (M1) | `platform/security`, `Rbac`, `services/identity`, `services/audit-admin` | `RbacTest`, `AuthenticationTest`, `AuditTest` (JWT: C-04) |
| 09 | Privacy by Design | Accepted (M1), **mở rộng bởi ADR-12** | `NimbusTokenCodec` (từ chối claim lạ), `AttendanceEventFactory` | xem `docs/architecture/privacy-notes.md` |
| 10 | Database per service + read model cho báo cáo | Accepted (M1) | `docker/init/**`, mỗi service schema riêng, `reporting` có read model | `ProjectionTest` (JDBC: D-04) |
| **11** | Offline-first, store-and-forward, máy trạng thái giao dịch | **Proposed (M2)** | `AccessAttempt`, `RecordScanUseCase`, `KioskApp`, `Journal` | `Adr11ResilienceTest`, `RecordScanUseCaseTest`, `KioskAppTest`; demo `make demo-timeout`, `make demo-offline` |
| **12** | QR động ký bằng khóa thiết bị, chống replay nhiều lớp, bỏ QR tĩnh | **Proposed (M2)** | `VerifyQrTokenUseCase`, `DeviceManagementService`, `OfflineVerifier`, `TokenSigner` | `Adr12TokenVerificationTest`, `DeviceAndVerifyApiTest`, `KioskAppTest`; demo `make demo-online` |
| **13** | Gate Station điều phối luồng quét tại biên | **Proposed (M2)** | `RecordScanUseCase`, `AccessPolicyChain` | `RecordScanUseCaseTest`, `AccessPolicyTest` |

Các tệp: [ADR-11](ADR-11-offline-first-store-and-forward.md), [ADR-12](ADR-12-dynamic-qr-device-binding.md), [ADR-13](ADR-13-edge-access-orchestrator.md). Tóm tắt thay đổi so với M1: [README.md](README.md).

## Chỗ mã khác ADR

Ba ADR M2 là bản **đề xuất** và mã đã lệch ở vài điểm có lý do. Danh sách đầy đủ, kèm việc cần sửa ADR hay mã: `docs/architecture/implementation-notes.md` mục 1. Trước khi chuyển ADR-11, 12, 13 sang **Accepted**, các lead phải quyết định từng điểm: sửa ADR cho khớp mã (đề xuất cho các điểm 1 đến 5) hoặc sửa mã cho khớp ADR.

## ADR tiếp theo

Số kế tiếp: **14**. Ứng viên đã thấy cần: chuỗi băm cho audit (D-08), cách xử lý lượt mở thủ công chờ xác minh (B-07), mô hình xác thực service-to-service (C-06).
