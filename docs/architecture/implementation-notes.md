# Ghi chú hiện thực: chỗ mã khác ADR và các khoảng trống đã biết

Tài liệu này trả lời câu hỏi "mã đang làm gì khác với ADR và báo cáo M1, và vì sao". **Báo cáo cuối phải khớp với mã, nên mọi chỗ khác biệt ở đây hoặc phải sửa mã, hoặc phải sửa ADR/báo cáo.** Mỗi mục có người nhận việc trong `docs/team/WORK-BREAKDOWN.md`.

## Bổ sung B-06/B-07 (25/11/2026)

- `CorrectAttendanceUseCase` và `POST /hr/v1/attendance/{attemptId}/corrections` thêm revision bất biến; bản gốc không bị sửa. `AttendanceCorrected` và `AuditRecorded` được ghi cùng transaction. Tra cứu last-direction áp revision mới nhất trước khi lọc và sắp xếp theo thời gian.
- `VerifyManualAttendanceUseCase` và `POST /hr/v1/attempts/{attemptId}/verify-manual` chỉ tính công cho `MANUAL` đã `COMMITTED`, không bất thường, có nhân viên và chiều. Migration V3 lưu người/lý do/thời điểm xác minh. Trạng thái vẫn `COMMITTED`; phiên bản tăng một lần; không gọi lại thiết bị cổng.
- Cả hai use case kiểm `HR_STAFF` và phạm vi nhà máy. API dùng `PrincipalResolver`; chỉ profile `local`/`inmem` cho phép dev headers. Subject từ danh tính đã xác thực là người thực hiện; không nhận actor do client tự khai.
- Hai thao tác khóa hàng `access_attempts` trong transaction để tránh trùng công/revision. Adapter từ chối ghi đè bằng snapshot cũ hoặc thay đổi bằng chứng xác minh đã lưu, khiến transaction liên quan rollback.
- Sự kiện audit của Attendance mang `actor` là UUID HR, `reason` là mã lý do và `operationId` là khóa thao tác. Hợp đồng này phục vụ consumer của Team D; B-06/B-07 không thay đổi mã consumer.
- Các bằng chứng kiểm thử của phiên bản 16/11 bên dưới không bao gồm thay đổi này. Test mới nằm trong `CorrectAttendanceUseCaseTest`, `ManualAttendanceVerificationTest`, `HrAttendanceApiTest`, `HrAttendanceDevProfileTest`, `InMemoryAttemptRepositoryTest` và `PersistenceAndBrokerIT`. Cần chạy `:services:attendance:test` và `:services:attendance:integrationTest` để xác nhận toàn bộ tiêu chí; kiểm tra trên bộ nhớ không chứng minh rollback PostgreSQL.

## 1. Khác biệt có chủ đích so với ADR

**Đã quyết định (16/11/2026):** điểm 1 đến 6 theo hướng **ADR khớp mã**; ADR-11, 12, 13 chuyển sang Accepted, mỗi ADR có mục
"Ghi chú khi chấp thuận" ghi lại điều chỉnh. Điểm 7 giữ nguyên (khớp M1). Báo cáo cuối chỉ cần mô tả theo cột "Mã làm".

| # | ADR nói | Mã làm | Lý do | Cần làm với báo cáo |
| --- | --- | --- | --- | --- |
| 1 | ADR-11: `PUBLISHED` là trạng thái của lượt quét | `PUBLISHED` là `publish_status` của hàng outbox, **không** phải `AttemptStatus` | Publisher chạy nền không nên ghi vào aggregate; M1 mục 12.7 cũng tách `PENDING_GATE` khỏi `PENDING`/`PUBLISHED` của outbox | Sửa sơ đồ trạng thái ADR-11 cho khớp (`state-machine.md` đã vẽ đúng theo mã) |
| 2 | ADR-11: `GATE_FAILED → COMMITTED → PUBLISHED` | `GATE_FAILED` là trạng thái **cuối**; không có `COMMITTED` cho lượt lỗi cổng. Sự kiện `GateResultRecorded` vẫn được phát | M1 bảng 16.3.2: lỗi cổng "không tính là đã vào/ra"; gọi nó `COMMITTED` dễ nhầm với công đã ghi | Sửa sơ đồ ADR-11 |
| 3 | ADR-12 và ADR-13: `ITokenReplayStore` thuộc User/QR | Kiểm tra replay một lần dùng nằm ở `ITokenReservation` của **Gate Station** (Redis `SET NX`); `VerifyQrTokenUseCase` chỉ đọc, không đánh dấu đã dùng | Cần nhả token khi cổng lỗi (M1 bảng 16.3.2). Nếu verify đánh dấu "đã dùng" thì không nhả được | Sửa bảng port ở ADR-13, bỏ `ITokenReplayStore` khỏi `user-qr` |
| 4 | ADR-12 test 2: token của thiết bị bị thu hồi trả `QR_REVOKED` | Trả `DEVICE_REVOKED` | ADR-12 mục API đã thêm `DEVICE_REVOKED`; hai chỗ trong ADR mâu thuẫn nhau. `QR_REVOKED` dành cho QR bị thu hồi theo nhân viên | Sửa ADR-12 test 2 (đã sửa trong repo) |
| 5 | ADR-12: token tối đa theo cấu hình, không nêu giới hạn | `TokenVerificationPolicy` từ chối token có `exp - iat > 60 s` (`MALFORMED_TOKEN`) | Chặn token sống lâu giống QR tĩnh, kể cả khi app bị sửa. Kịch bản demo `--long-lived` | Thêm vào ADR-12 mục "Quyết định" |
| 6 | ADR-13: QR Verifier là instance chỉ-đọc của User/QR đặt tại Edge | Hiện là endpoint `/internal/v1/qr/verify` của chính service `user-qr` | Chưa có triển khai Edge riêng cho `user-qr`. Hợp đồng không đổi nên chuyển sang Edge sau này không ảnh hưởng Gate Station | Ghi rõ trong mục triển khai: "mô hình hóa như instance Edge, chạy cùng tiến trình khi demo" |
| 7 | M1 mục 12.6: mở thủ công không tự thành công hợp lệ | `GateResultApplier` commit lượt `MANUAL` nhưng **không** tạo `AttendanceTransaction` và không phát `AttendanceConfirmed`; chỉ phát `AuditRecorded` | Khớp M1. Việc xác minh bởi HR là story B-07 | Giữ nguyên, mô tả luồng xác minh |

## 2. Quyết định kỹ thuật bổ sung (chưa có ADR riêng)

| Chủ đề | Quyết định | Nơi trong mã |
| --- | --- | --- |
| Không lưu token thô | Journal kiosk chỉ lưu SHA-256 của token. Khi gửi lại lượt chưa có kết quả, kiosk gửi **hash** thay token. Nếu Edge đã biết `attempt_id` thì trả kết quả đã lưu, nếu chưa thì từ chối `MALFORMED_TOKEN` và nhân viên quét lại | `KioskApp.flush`, `RecordScanUseCase` (bước tra `find` đầu tiên) |
| `Idempotency-Key` header = `attempt_id` | Cùng một UUID v7 đi suốt: kiosk, Edge, Turnstile (`cmd-ts-<attempt_id>`), Outbox | `KioskController`, `HttpTurnstileAdapter` |
| Thời hạn giữ chỗ | Token: giữ 10 giây khi đang gửi lệnh; sau `OPENED` hoặc `TIMEOUT` chặn đến `exp + 60 s`. Anti-passback pending: 60 giây | `ScanSettings` |
| Lỗi kết nối sớm với Turnstile | Retry **một lần** cùng khóa idempotency rồi coi là `TIMEOUT`. Chỉ lỗi kết nối được retry, chậm trả lời thì không | `HttpTurnstileAdapter` |
| Khi Attendance không lưu được trước lệnh cổng | Nhả token và anti-passback, ném lỗi, **không** gửi lệnh mở. Hàng `RESERVED` còn lại được `RecoverStaleAttemptsUseCase` giải phóng | `RecordScanUseCase` bước 4 |
| Khi cổng mở nhưng không ghi được kết quả | Trả `UNCONFIRMED`; hàng ở `GATE_COMMANDED` sẽ được phục hồi sang `GATE_UNKNOWN` rồi đối soát | `RecordScanUseCase.onOpened` |
| Kiosk hỏi lại | Khi Edge trả `IN_PROGRESS`, kiosk hỏi lại cùng `attempt_id` tối đa 6 lần, mỗi lần chờ 300 ms, rồi mới kết luận `PENDING`. **Không** mở cổng cục bộ khi Edge đang giữ lượt | `KioskApp.scan` |
| Thứ tự xác thực QR | Cấu trúc và PII, thiết bị, trạng thái thiết bị, chủ khóa phải là chủ token, chữ ký, rồi mới đến thời gian | `VerifyQrTokenUseCase` |
| JWT của nhân sự (ADR-08) | identity ký ES256, sống 15 phút, JWKS ở `/.well-known/jwks.json`; service kiểm tra bằng `JwtPrincipalVerifier`. Header dev chỉ được tin ở profile `local`, `inmem` và khi không có `Authorization`; Bearer sai không bao giờ rơi về header | `platform/security/jwt/*`, `PrincipalResolver`, identity `LoginUseCase`, user-qr `UserQrConfig` |
| Một exchange cho mọi sự kiện | `tmc.events`, khóa `<nguồn>.<EventType>`, mỗi consumer một hàng đợi + DLQ, thử lại 3 lần (ADR-15) | `platform/messaging/TmcExchange`, các `*QueueConfig` |
| Outbox và khử trùng dùng chung | `OutboxStore`/`OutboxRelay`/`AmqpEnvelopePublisher`; `JdbcProcessedMessageStore` chạy chung transaction với handler (ADR-15) | `platform/messaging`, `DedupStoreConfig` ở 3 consumer |
| Database riêng của Gate Station | `gate_store` trên PostgreSQL Edge, chỉ bật với profile `gate-store` (ADR-14) | `GateStoreConfig`, `db/gate-store` |
| Danh bạ nhân viên tại Edge | Nạp từ seed, làm mới từ gói đồng bộ của User/QR mỗi 30 giây; lỗi thì giữ bản cuối | `SyncEmployeeDirectoryUseCase`, `EmployeeDirectorySyncJob` |
| Hiệu chỉnh công (B-06) | Bản hiệu chỉnh là hàng mới trong `attendance_corrections`, không sửa hàng gốc; `aggregateVersion` = phiên bản gốc + `revision` | `AttendanceCorrection`, `AttendanceEventFactory.attendanceCorrected` |
| Một nguồn cho dữ liệu dùng chung trong test | `docs/api/fixtures/employee-seed.json`, `qr-token-policy.json`; mỗi bên có test đọc tệp này | `EmployeeSeedContractTest` (3 service), `QrTokenPolicyContractTest` (user-qr, kiosk-simulator), `token.test.ts` |

## 3. Anti-passback sau khi nhập offline (Team A: A-02, Team B: B-03; cập nhật 23/11/2026)

Trước A-02, `IngestOfflineBatchUseCase` ghi lượt vào Attendance nhưng không cập nhật `IAntiPassbackStore` của Gate Station. A-02 hiện cập nhật `last` bằng `rememberIfNewer` sau khi Attendance chấp nhận bản ghi `OFFLINE_OPENED`; bản ghi offline cũ không ghi đè trạng thái mới hơn, còn bản ghi từ chối không tính là lượt qua cổng. Unit test và Redis integration test đều xanh; `RedisAdaptersIT` chạy đủ 9 test sau khi Docker Engine được bật.

## 4. Trạng thái lạnh và danh bạ nhân viên (Team A: A-03, Team C: C-02)

- **A-03 đã xử lý trạng thái Redis mất khóa `last`:** `RecordScanUseCase` dựng lại từ Attendance qua `lastDirection`, ghi bằng `rememberIfNewer`, và lỗi dependency khiến request bị fail-secure (503 `DEPENDENCY_UNAVAILABLE`). Unit và HTTP test xác nhận lỗi dependency không mở cổng; kiểm tích hợp I-1 với Attendance thật chưa xác minh trong lượt rà soát này. AOF giảm khả năng mất dữ liệu nhưng không thay thế bước phục hồi.
- **`IEmployeeDirectory`** của Gate Station nay được làm mới từ gói đồng bộ của User/QR. Nhưng User/QR vẫn lấy danh sách từ cấu hình (`tmc.qr.employees`); nguồn HR thật là C-02. Thay đổi trạng thái nhân viên đến cổng trễ tối đa một chu kỳ (30 giây).
- Gói đồng bộ của kiosk **đã mang** danh bạ đủ điều kiện, nhưng kiosk-simulator **chưa dùng** nó khi offline: kiểm tra offline chỉ dựa vào khóa thiết bị, chữ ký, thời gian và replay. Nhân viên đã nghỉ việc nhưng thiết bị chưa bị thu hồi vẫn qua được khi kiosk offline, đến lúc đồng bộ sẽ bị gắn bất thường. M1 mục 10.4 đã thừa nhận giới hạn "không cam kết thu hồi tức thời trên thiết bị mất mạng"; phải nêu rõ trong báo cáo.

## 5. Replay scan (Team A: A-06, Team B: B-04)

- **A-06 đã xử lý:** replay của `RecordScanUseCase` trả lại `AttemptState.direction`; test xác nhận lượt gửi lại giữ nguyên chiều và trường hợp dữ liệu cũ không có chiều vẫn trả `null`.
- `ReasonCode.GATE_DIRECTION_MISMATCH` chưa có rule nào dùng; chỉ dùng trong test mở rộng chính sách.

## 6. Đã và chưa được kiểm chứng

Cập nhật 16/11/2026 trên máy có Docker. Số test chính xác: `docs/handover/_decoupling-changelog.md` mục 3.

| Hạng mục | Trạng thái | Bằng chứng |
| --- | --- | --- |
| Adapter JDBC và Flyway của attendance, outbox trên PostgreSQL | **Đã kiểm chứng** | `PersistenceAndBrokerIT` |
| `JdbcDeviceKeyRegistry`, outbox V2 của user-qr | **Đã kiểm chứng** | `UserQrPersistenceIT` |
| `RedisTokenReservation`, `RedisAntiPassbackStore` | **Đã kiểm chứng** | `RedisAdaptersIT` |
| Migration `gate_store`, profile `gate-store` | **Đã kiểm chứng** | `GateStoreIT`; khởi động thật trên container edge |
| Outbox → RabbitMQ → consumer idempotent, `JdbcProcessedMessageStore`, hoàn tác dấu khi lỗi | **Đã kiểm chứng** | `MessagingInfraIT` |
| `@RabbitListener` của reporting, hr-integration (gồm DLQ), audit-admin | **Đã kiểm chứng** | `AttendanceEventListenerIT`, `HrAttendanceListenerIT`, `AuditEventListenerIT` |
| JWT: cấp, JWKS, kiểm tra Bearer qua mạng | **Đã kiểm chứng** | `LoginApiTest`, `BearerTokenApiTest`; thử thật D-01 (`troubleshooting.md`) |
| 7 service khởi động ở profile mặc định trên hạ tầng `make up` | **Đã kiểm chứng** (D-01) | `troubleshooting.md`, mục Kết quả D-01 |
| Dừng và bật lại RabbitMQ khi đang phát (B-02) | **Chưa** | Cần test dừng container giữa chừng |
| 3 kịch bản demo trên stack Docker đầy đủ | **Chưa** (chỉ chạy trên profile `local`) | Chạy demo với service ở profile mặc định |
| Read model JDBC của reporting, nhật ký HR, kho audit | **Chưa có mã** (vẫn trong bộ nhớ) | D-04, D-06, D-08 |
| Mọi kiểm thử hiệu năng (NFR-06) | **Chưa** | A-09 |
| mTLS, TLS 1.2+ | **Chưa** cấu hình | D-11 |

**Đã kiểm chứng từ trước:** toàn bộ test đơn vị và kiến trúc, và 3 kịch bản demo trên các tiến trình thật qua HTTP (profile `local`, nay 5 tiến trình có identity).

## 7. Điều báo cáo không được khẳng định

- Không nói "hệ thống đạt p95 ≤ 1 giây" cho tới khi A-09 có số đo.
- Không nói "không mất bản ghi khi broker khởi động lại" cho tới khi B-02 có test dừng và bật lại RabbitMQ. Có thể nói: "outbox và phát với xác nhận đã chạy trên PostgreSQL và RabbitMQ thật; mỗi điểm đứt có quy tắc phục hồi được test đơn vị".
- Không nói audit đăng nhập là bền vững: outbox của identity còn ở bộ nhớ (ADR-15).
- Không nói "có MFA" cho tới khi C-05 có TOTP thật.
