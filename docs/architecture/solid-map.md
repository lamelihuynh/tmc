# Bản đồ SOLID: nguyên tắc → lớp thật → bằng chứng

Dùng cho mục "Áp dụng SOLID" và "Phản ánh" của báo cáo. **Mỗi dòng trỏ tới lớp có trong mã và test chạy được.** Không đưa vào báo cáo dòng nào mà test đã bị `@Disabled`.

## S: Single Responsibility (một lý do để thay đổi)

| Trách nhiệm tách ra | Lớp | Lý do thay đổi duy nhất | Bằng chứng |
| --- | --- | --- | --- |
| Điều phối một lượt quét | `RecordScanUseCase` (gate-station) | Thứ tự các bước của luồng quét | `RecordScanUseCaseTest` |
| Chính sách ra vào | `ActiveEmployeeRule`, `FactoryPermissionRule`, `AntiPassbackRule` | Chính sách của ban quản lý | `AccessPolicyTest` |
| Chọn chiều cổng | `DirectionResolver` | Quy tắc cổng hai chiều | `AccessPolicyTest.should_resolve_direction_...` |
| Giao tiếp với thiết bị | `HttpTurnstileAdapter` | API của hãng turnstile | `HttpTurnstileAdapterTest` |
| Áp dụng kết quả cổng thành trạng thái, công, sự kiện | `GateResultApplier` (attendance) | Cách ghi nhận công | `Adr11ResilienceTest` |
| Dựng sự kiện không chứa PII | `AttendanceEventFactory` | Hợp đồng sự kiện | `Adr11ResilienceTest.should_not_leak_the_token_id_or_any_name_into_event_payloads` |
| Quy tắc thời gian của token | `TokenVerificationPolicy` (user-qr) | Chính sách bảo mật QR | `Adr12TokenVerificationTest` (nhóm test 6) |
| Khóa tài khoản | `Account` (identity) | Chính sách NFR-03 | `AuthenticationTest` |

Bốn lý do thay đổi của luồng quét (M1 mục 8.1.1) nằm ở bốn lớp khác nhau: mã hóa QR (`NimbusTokenCodec`), chính sách ra vào (các rule), ghi nhận công (`GateResultApplier`), API turnstile (`HttpTurnstileAdapter`).

## O: Open/Closed

| Điểm biến đổi | Cơ chế mở rộng | Cách thêm mà không sửa mã cũ | Bằng chứng |
| --- | --- | --- | --- |
| Quy tắc ra vào | `AccessPolicy` + `AccessPolicyChain`; trong Spring, mọi bean `AccessPolicy` tự vào chuỗi (`GateStationConfig.accessPolicyChain`) | Thêm một lớp hoặc bean `AccessPolicy` | `AccessPolicyTest.should_let_a_new_rule_deny_without_changing_existing_ones` (ADR-13 compliance 3) |
| Nhà cung cấp turnstile | `ITurnstileControlPort` + chọn bằng `tmc.gate.turnstile.mode` | Thêm adapter mới, đổi cấu hình | `GateFixture` dùng `SimulatedTurnstileAdapter`; `KioskApiTest`; demo dùng `HttpTurnstileAdapter` (ADR-13 compliance 2) |
| Loại sự kiện tiêu thụ | `IdempotentConsumer` nhận bất kỳ handler | Thêm consumer mới, không sửa Attendance | `IdempotentConsumerTest`, `ProjectionTest` |
| Lưu trữ | Port `I*Repository` + profile `inmem`/`local`/mặc định | Thêm adapter lưu trữ mới | `InternalApiFlowTest` (inmem), `PersistenceAndBrokerIT` (JDBC) |

Điểm cần thành thật: danh sách rule tiêu chuẩn vẫn do `AccessPolicyChain.standard()` và `GateStationConfig` quyết định thứ tự; thêm rule mới chỉ cần thêm bean, nhưng đặt rule **trước** `AntiPassbackRule` cần sửa thứ tự trong cấu hình.

## L: Liskov Substitution

| Hợp đồng | Các cài đặt thay thế được | Điều khoản phải giữ | Bằng chứng |
| --- | --- | --- | --- |
| `ITurnstileControlPort` | `HttpTurnstileAdapter`, `SimulatedTurnstileAdapter` | Không ném ngoại lệ vì lỗi thiết bị; trả đúng `OPENED`, `ERROR`, `TIMEOUT`; `TIMEOUT` nghĩa là chưa rõ, không bao giờ coi là chưa mở | `HttpTurnstileAdapterTest` (bảng ánh xạ lỗi) và toàn bộ `RecordScanUseCaseTest` chạy trên bản mô phỏng |
| `IAttemptRepository`, `IOutboxRepository` | bản in-memory và bản JDBC | Cùng ngữ nghĩa upsert, thứ tự outbox theo thêm vào, `fetchPending` ẩn hàng đã phát | `Adr11ResilienceTest` (in-memory) và `PersistenceAndBrokerIT` (JDBC, cần Docker) |
| `EventPublisher` | `AmqpEventPublisher`, `InMemoryEventBroker` | Chỉ trả về sau khi broker xác nhận; ném `PublishFailedException` khi không xác nhận | `Adr11ResilienceTest` (broker tắt) |
| `IAttendanceGateway` | `HttpAttendanceGateway`, `FakeAttendanceGateway` | Idempotent theo `attemptId` | `RecordScanUseCaseTest` |

## I: Interface Segregation

| Nhóm client | Interface hẹp | Không bị ép phụ thuộc vào |
| --- | --- | --- |
| Kiosk | `IGateAccess`, `IKioskSupport` | Mở thủ công, quan sát cổng |
| Bảo vệ | `IGateManualAccess` | Quét QR |
| Attendance reconcile | `IGateObservation` | Mọi thứ khác của Gate Station |
| Gate Station gọi Attendance | `IAttendanceCommand` | Nhập offline, truy vấn |
| Báo cáo | `IReporting`, `IAttendanceQuery` | **Không có hàm ghi nào**: module báo cáo không thể sửa công (AC-03, AC-05) |
| Kiểm toán | `IAuditStore` | **Không có hàm sửa hoặc xóa** |
| API theo actor | `/kiosk/**`, `/security/**`, `/self-service/**`, `/hr/**`, `/supervision/**`, `/admin/**` | Khớp với RBAC (ADR-08) |

Bằng chứng: `AuditTest.should_have_no_way_to_change_or_remove_history_through_the_store_port` kiểm bằng reflection rằng `IAuditStore` không có hàm `update`, `delete`, `remove`, `clear`.

## D: Dependency Inversion

| Chính sách cấp cao | Interface do tầng cấp cao sở hữu | Chi tiết hạ tầng (ở `adapter/out`) |
| --- | --- | --- |
| Luồng quét | `ITurnstileControlPort`, `IQRValidation`, `ITokenReservation`, `IAntiPassbackStore`, `IAttendanceGateway` (đều trong `gate-station/application/port/out`) | HTTP, Redis, JDBC |
| Ghi nhận công | `IAttemptRepository`, `IOutboxRepository`, `ITransactionRunner` | JDBC, `TransactionTemplate` |
| Đồng bộ HR | `IHrPayrollPort`, `ISyncLogRepository` | HTTP của HR (story D-06) |
| Thời gian | `IClock` | `Instant.now()` hoặc đồng hồ giả trong test |

Bằng chứng tự động: `ArchitectureTest` ở **mọi** service kiểm `domain` không phụ thuộc `application` hay `adapter`, và `application` không phụ thuộc `adapter`. Điểm nối duy nhất là gói `bootstrap` (composition root).

Mọi ca kiểm thử use case chạy mà **không có Spring, không có mạng, không có CSDL**: đó là kết quả cụ thể của DIP (`GateFixture`, `AttendanceFixture`).
