# Team A: Gate Station

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này. Lịch chung: [01-ke-hoach.md](01-ke-hoach.md).

> **Bạn có thể bắt đầu ngay (16/11)**
>
> **Đã có sẵn cho Team A** (bước gỡ phụ thuộc, nhánh `chore/x/decouple-teams`):
>
> - `IAttendanceGateway.lastDirection(UUID)` (hiện thực ở `HttpAttendanceGateway`, `FakeAttendanceGateway`) và `AttemptState.direction`, `AttemptState.employeeUuid`; endpoint thật `GET /internal/v1/employees/{employeeUuid}/last-direction` của attendance đã chạy (`HttpAttendanceGatewayTest`).
> - Database `gate_store` (ADR-14): profile `gate-store`, `GateStoreConfig`, Flyway `db/gate-store/V1__gate_store.sql` với bảng `gates` và `outbox_events`; `GateStoreIT` (2 test) xanh.
> - Danh bạ nhân viên tự làm mới từ gói đồng bộ của user-qr (A-10 đã xong): `SyncEmployeeDirectoryUseCase`, `EmployeeDirectorySyncJob`, `EmployeeSyncTest`.
> - `RedisAdaptersIT` (5 test) xanh với Redis thật.
> - `platform/messaging`: `JdbcOutboxStore`, `AuditOutbox`, `OutboxRelay`, `AmqpEnvelopePublisher`; `platform/security`: `PrincipalResolver`, `JwtPrincipalVerifier`, fixture `TestJwtKeys`, `TestTokens`.
>
> **Lệnh:**
>
> ```bash
> ./gradlew :services:gate-station:test :simulators:turnstile-simulator:test   # không cần Docker
> ./gradlew :services:gate-station:integrationTest                              # cần Docker đang chạy (Testcontainers)
> make up                       # hạ tầng thật để chạy service; thiếu database gate_store thì: make nuke && make up
> SPRING_PROFILES_ACTIVE=default,gate-store ./gradlew :services:gate-station:bootRun
> ```
>
> **Code theo:** `docs/api/kiosk-edge.openapi.yaml`, `docs/api/internal-attendance.openapi.yaml` (last-direction, `AttemptView`), `docs/api/identity.openapi.yaml` (JWT), `docs/api/attendance-events.asyncapi.yaml` (`AuditRecorded` trên `tmc.events`), `docs/api/fixtures/employee-seed.json`; trong test: `FakeAttendanceGateway`, `GateFixture`, `TestTokens`, `TmcContainers`.
>
> **Không chờ team nào.** Mọi story dưới đây xong được bằng bản giả và fixture. Phần cần mã thật của team khác là kiểm tích hợp **tùy chọn** ở mục 5.

## 1. Nhiệm vụ của team

Team A giữ **bộ não tại cổng**: nhận lượt quét từ kiosk, quyết định mở hay không, ra lệnh cho cổng xoay, và bảo đảm cổng không bao giờ mở hai lần cho một lượt quét.

| Sở hữu | Nội dung |
| --- | --- |
| `services/gate-station` (cổng 8081) | Điều phối lượt quét, quy tắc ra vào, anti-passback, giữ chỗ token, mở thủ công, kho cấu hình cổng (`gate_store`) |
| `simulators/turnstile-simulator` (cổng 8090) | Cổng xoay giả lập, chèn lỗi |
| Hợp đồng | `docs/api/kiosk-edge.openapi.yaml`, `docs/api/turnstile.openapi.yaml` |
| Tài liệu | ADR-13, ADR-14, phần luồng quét QR của báo cáo |

Hai người: **A1** (lead) giữ kho cấu hình cổng, bảo mật và audit; **A2** giữ anti-passback, cổng xoay và đo tải.

## 2. Bản đồ mã của gate-station

Gói gốc: `services/gate-station/src/main/java/vn/tmc/gatestation/`.

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | `GateConfig`, `GateMode`, `EmployeeAccess`, `LastPass`, `DirectionResolver` | Mô hình cổng và chiều ra vào |
| `domain.policy` | `AccessPolicyChain`, `ActiveEmployeeRule`, `FactoryPermissionRule`, `AntiPassbackRule` | Quy tắc ra vào; thêm quy tắc bằng một lớp `AccessPolicy` mới |
| `application.usecase` | **`RecordScanUseCase`** | Luồng 7 bước của một lượt quét |
| | `ManualOpenUseCase` | Bảo vệ mở cổng thủ công, bắt buộc có lý do |
| | `ObserveGateUseCase` | Hỏi cổng "lệnh kia đã làm gì" cho attendance đối soát |
| | `KioskSupportService` | Gói đồng bộ, heartbeat, nhập lô offline |
| | `SyncEmployeeDirectoryUseCase` | Làm mới danh bạ nhân viên từ gói đồng bộ |
| `application.port.out` | `IAttendanceGateway`, `IQRValidation`, `ITokenReservation`, `IAntiPassbackStore`, `ITurnstileControlPort`, `IGateConfigRepository`, `IEmployeeDirectory`, `IKioskSyncSource`, `IHeartbeatStore`, `IAuditLog` | Mọi thứ use case cần ở bên ngoài |
| `adapter.in.rest` | `KioskController`, `SecurityController`, `InternalController` | API cho kiosk, bảo vệ, attendance |
| `adapter.out.inmem` | `InMemoryTokenReservation`, `InMemoryAntiPassbackStore`, `InMemoryGateConfigRepository`, `FakeAttendanceGateway`, `LoggingAuditLog`, ... | Bản dùng cho test và profile `local`, `inmem` |
| `adapter.out.redis` | `RedisTokenReservation`, `RedisAntiPassbackStore` | Bản thật, đã có `RedisAdaptersIT` |
| `adapter.out.http` | `HttpAttendanceGateway`, `HttpQrValidationClient`, `HttpKioskSyncSource`, `HttpTurnstileAdapter` | Gọi attendance, user-qr, cổng xoay |
| `bootstrap` | `GateStationConfig`, `InMemoryProfileConfig`, `InfrastructureProfileConfig`, `GateStoreConfig`, `EmployeeDirectorySyncJob`, `GateProperties` | Chọn adapter theo profile |

Dữ liệu mẫu cổng nằm ở `src/main/resources/application.yml`, mục `tmc.gate`; nhân viên phải khớp `docs/api/fixtures/employee-seed.json` (`EmployeeSeedContractTest`).

| Lớp test | Kiểm gì |
| --- | --- |
| `RecordScanUseCaseTest` | Luồng quét, mọi nhánh từ chối, gửi lại, hết giờ chờ, lỗi phần cứng |
| `ManualOpenUseCaseTest` | Quyền của bảo vệ, lý do bắt buộc, audit |
| `AccessPolicyTest` | Từng quy tắc ra vào |
| `KioskApiTest` | API qua MockMvc với profile `inmem` |
| `HttpTurnstileAdapterTest`, `HttpAttendanceGatewayTest`, `EmployeeSyncTest` | Adapter HTTP đọc đúng định dạng của bên kia |
| `RedisAdaptersIT`, `GateStoreIT` | Redis và `gate_store` thật (thẻ `integration`) |
| `ArchitectureTest` | Chiều phụ thuộc giữa các gói |

`GateFixture` (trong `src/test/.../support`) dựng cả ứng dụng trên adapter trong bộ nhớ, không cần Spring. Test use case mới bắt đầu từ đây.

## 3. Việc phải làm (16/11 đến 24/11)

**Mọi hạn là 23:00 của ngày cuối**, tính theo lúc PR hợp nhất vào `main`. Cỡ: S khoảng nửa ngày, M một đến hai ngày, L ba ngày, cho một người. Mức: **B** bắt buộc, **N** nên có (cắt trước nếu trễ).

| Story | Mức | Người | Bắt đầu | Hạn |
| --- | --- | --- | --- | --- |
| A-04 Kho cấu hình cổng trên `gate_store` | B | A1 | 16/11 | 18/11 |
| A-12 Audit cổng qua outbox của `gate_store` | B | A1 | 19/11 | 19/11 |
| A-11 JWT ở `SecurityController` | B | A1 | 20/11 sáng | 20/11 |
| A-03 Dựng lại anti-passback từ attendance | B | A2 | 16/11 | 16/11 |
| A-06 Trả chiều khi kiosk gửi lại | N | A2 | 17/11 sáng | 17/11 |
| A-01 (phần còn lại) Redis khởi động lại, AOF | B | A2 | 17/11 chiều | 17/11 |
| A-02 Đồng bộ anti-passback sau lô offline | B | A2 | 18/11 | 19/11 |
| A-05 Test hợp đồng với turnstile-simulator thật | N | A2 | 20/11 sáng | 20/11 |
| A-09 Kiểm thử tải | B | A2 chuẩn bị, A1 và A2 đo | 20/11 chiều | 23/11 |
| R-A Phần báo cáo của Team A | B | A1 | 20/11 chiều (nháp) | 24/11 (bản cuối) |

Ngày 24/11: đóng băng mã lúc 23:00, chỉ sửa lỗi; cả hai tham gia tổng duyệt demo. Cuối tuần 21 và 22/11 không xếp việc; dùng nếu cần đuổi kịp.

### A-04. Kho cấu hình cổng trên `gate_store` (B, L, A1, 16 đến 18/11)

| | |
| --- | --- |
| Mục tiêu | Thay `InMemoryGateConfigRepository` (đọc `application.yml`) bằng bảng `gates` của `gate_store`, có API quản trị cho `SYSTEM_ADMIN` (FR-11 đến FR-13, FR-16) |
| Sửa ở | `adapter/out/persistence/JdbcGateConfigRepository.java` (mới, dùng bean `gateStoreJdbc`); mở rộng `IGateConfigRepository` với thao tác ghi; use case quản trị trong `application/usecase/`; controller quản trị trong `adapter/in/rest/`; nối ở `GateStoreConfig`. Migration `V2__...` nếu cần nạp dữ liệu mẫu |
| Code theo | Bảng `gates` trong `db/gate-store/V1__gate_store.sql` (đã có); thêm đường dẫn quản trị vào `docs/api/kiosk-edge.openapi.yaml` trong cùng PR (tệp của Team A); kiểm quyền bằng `Rbac` và header dev (A-11 đổi sang JWT sau) |
| Xong khi | Test IT mới trong `adapter/it/` (thẻ `integration`): thêm, sửa, đặt bảo trì một cổng rồi đọc lại; test use case: vai trò khác `SYSTEM_ADMIN` nhận 403, đặt bảo trì thì lượt quét kế tiếp bị `GATE_IN_MAINTENANCE` ngay. `./gradlew :services:gate-station:test :services:gate-station:integrationTest` xanh; profile `inmem` và `local` vẫn chạy bằng dữ liệu mẫu (`KioskApiTest` xanh) |

### A-12. Audit cổng qua outbox của `gate_store` (B, M, A1, 19/11)

| | |
| --- | --- |
| Mục tiêu | Mở thủ công và thay đổi cấu hình cổng được ghi thành sự kiện `AuditRecorded` (khóa `gate-station.AuditRecorded`, ADR-15) thay vì chỉ ghi log |
| Sửa ở | Adapter mới hiện thực `IAuditLog` bọc `AuditOutbox` trên `JdbcOutboxStore` của `gate_store`; `OutboxRelay` và `AmqpEnvelopePublisher` trong `GateStoreConfig`; giữ `LoggingAuditLog` cho `inmem`, `local`. Ghi audit trong cùng transaction với thay đổi `gates` |
| Code theo | `platform/messaging` (`AuditOutbox`, `AuditEvents`, `JdbcOutboxStore`); bảng `outbox_events` của V1; mẫu đã làm ở user-qr (`services/user-qr/.../bootstrap/OutboxConfig.java`) |
| Xong khi | Mở rộng `GateStoreIT` (đã có `should_hold_gate_audit_events_in_the_shared_outbox_table`): mở thủ công thì có một dòng `PENDING`; một use case ném lỗi giữa chừng thì không có dòng nào; relay đẩy lên RabbitMQ của `TmcContainers` và dòng thành `PUBLISHED` |

### A-11. JWT ở `SecurityController` (B, S, A1, 20/11 sáng)

| | |
| --- | --- |
| Mục tiêu | API bảo vệ (`/security/v1/**`) và API quản trị cổng nhận `Authorization: Bearer`; header dev chỉ còn ở `local`, `inmem` |
| Sửa ở | Bean `PrincipalResolver` trong `bootstrap` (theo mẫu `UserQrConfig.principalResolver`), `SecurityController` và controller quản trị dùng resolver; cấu hình `tmc.security.jwks-url` (env `IDENTITY_JWKS_URL`), `tmc.security.allow-dev-headers`; thêm `testImplementation(testFixtures(project(":platform:security")))` |
| Code theo | `docs/api/identity.openapi.yaml`; fixture `TestJwtKeys`, `TestTokens`; mẫu test `BearerTokenApiTest` của user-qr |
| Xong khi | Test MockMvc mới: Bearer hợp lệ vai `SECURITY_GUARD` mở được cổng; token hết hạn hoặc ký bằng khóa lạ nhận 401; ở profile mặc định, header `X-Subject` không có Bearer nhận 401 hoặc 403 |

### A-03. Dựng lại anti-passback từ attendance (B, M, A2, 16/11)

| | |
| --- | --- |
| Mục tiêu | Redis mất khóa `last` thì nhân viên đã vào không được vào lần nữa |
| Sửa ở | `RecordScanUseCase`: khi `antiPassback.last(...)` trống, gọi `attendance.lastDirection(employee)` rồi ghi lại vào store; port `IAntiPassbackStore` cần một thao tác ghi `last` (dùng chung với A-02) |
| Code theo | `IAttendanceGateway.lastDirection` (đã có), `FakeAttendanceGateway`; hợp đồng `GET /internal/v1/employees/{employeeUuid}/last-direction` (404 nghĩa là chưa có lượt nào) |
| Xong khi | Test mới trong `RecordScanUseCaseTest`: sau một lượt `ENTRY`, xóa trạng thái anti-passback, lượt `ENTRY` thứ hai ở cổng một chiều vẫn bị `ANTI_PASSBACK_VIOLATION`; attendance không trả lời thì quyết định ghi trong mô tả PR (fail-secure, không biến thành chấp thuận vì thiếu dữ liệu) và có test cho quyết định đó |

### A-06. Trả chiều khi kiosk gửi lại (N, S, A2, 17/11 sáng)

| | |
| --- | --- |
| Mục tiêu | `ScanResult.direction` của lượt gửi lại không còn `null` |
| Sửa ở | `RecordScanUseCase.replay` dùng `AttemptState.direction` (đã có, điền ở `HttpAttendanceGateway`) |
| Code theo | `AttemptView` trong `internal-attendance.openapi.yaml`; `FakeAttendanceGateway` |
| Xong khi | Test gửi lại nhận đủ chiều như lần đầu; attendance cũ không gửi `direction` thì vẫn trả `null`, không lỗi |

### A-01 (phần còn lại). Redis khởi động lại với AOF (B, S, A2, 17/11 chiều)

| | |
| --- | --- |
| Mục tiêu | Bốn trong năm tiêu chí của A-01 đã có trong `RedisAdaptersIT`; còn thiếu: khóa `USED` còn sau khi Redis khởi động lại |
| Sửa ở | `RedisAdaptersIT` (container Redis bật `--appendonly yes`) |
| Code theo | `TmcContainers` |
| Xong khi | Test mới: `markUsed` rồi dừng và bật lại container, `reserve` cùng `jti` vẫn trả `false`. `./gradlew :services:gate-station:integrationTest` xanh |

### A-02. Đồng bộ anti-passback sau lô offline (B, M, A2, 18 đến 19/11)

| | |
| --- | --- |
| Mục tiêu | Lỗ hổng ở [`implementation-notes.md`](../architecture/implementation-notes.md) mục 3: vào lúc mất mạng, kiosk gửi bù, rồi vào lại online ngay thì không bị chặn |
| Sửa ở | `KioskSupportService.ingestOffline`: mỗi bản ghi attendance trả `ACCEPTED` thì cập nhật `last` trong `IAntiPassbackStore` (thao tác ghi thêm ở A-03, có ở cả bản bộ nhớ và Redis) |
| Code theo | `FakeAttendanceGateway` (kết quả nhập lô), `docs/api/kiosk-edge.openapi.yaml` mục offline-batch |
| Xong khi | Test: quét offline `ENTRY`, gửi lô, quét online `ENTRY` ở cổng một chiều bị `ANTI_PASSBACK_VIOLATION`; bản ghi offline **cũ hơn** `last` hiện có không ghi đè; cùng test chạy trên Redis trong `RedisAdaptersIT` |

### A-05. Test hợp đồng với turnstile-simulator thật (N, S, A2, 20/11 sáng)

| | |
| --- | --- |
| Mục tiêu | `HttpTurnstileAdapter` được thử với chính `turnstile-simulator`, không chỉ máy chủ HTTP tự dựng |
| Sửa ở | Test mới trong gate-station hoặc `simulators/turnstile-simulator/src/test/` (khởi động simulator ở cổng ngẫu nhiên) |
| Code theo | `docs/api/turnstile.openapi.yaml` |
| Xong khi | Bao phủ `JAM`, `OFFLINE`, `GATEWAY_TIMEOUT`, `DELAY_OPENED`, `DELAY_NOT_OPENED` và ngưỡng chờ 800 ms |

### A-09. Kiểm thử tải (B, M, A2 chuẩn bị 20/11 chiều; A1 và A2 đo 23/11)

| | |
| --- | --- |
| Mục tiêu | Đo NFR-06: 800 lượt trong 30 phút, p95 không quá 1,0 giây, p99 không quá 2,0 giây |
| Sửa ở | Script hoặc test trong `services/gate-station` (hoặc `scripts/`, báo Team D duyệt): sinh token hợp lệ bằng mã của `device-simulator` hoặc `PhoneSimulator`, gọi `POST /kiosk/v1/scans` với nhiều nhân viên, ghi thời gian từng lượt |
| Code theo | Stack `local` (`scripts/dev/local-stack.sh start`), `turnstile-simulator` không lỗi. Không cần stack Docker của team khác |
| Xong khi | Bảng p50, p95, p99, tỷ lệ lỗi, cấu hình máy, lệnh chạy lại; nộp vào `docs/report/` trước 23:00 ngày 23/11. Ghi rõ điều kiện đo (máy cá nhân, cổng giả lập, profile nào). Đo trên stack Docker là kiểm tích hợp tùy chọn I-7 |

### R-A. Phần báo cáo của Team A (A1; nháp 20/11, bản cuối 24/11)

- Sơ đồ sequence của luồng quét QR, khớp với `RecordScanUseCase` hiện tại.
- Bằng chứng cho ADR-11 phía gate-station: tên test cho P1, P3, P5 và kịch bản `demo-timeout`; ADR-14 (`GateStoreIT`).
- SOLID trên mã của team: `AccessPolicyChain` (mở rộng không sửa), `ITurnstileControlPort` với ba adapter (thay thế được), các port vào tách riêng `IGateAccess`, `IGateManualAccess`, `IGateObservation`, `IKioskSupport` (interface nhỏ).
- Số đo A-09 và một đoạn phản ánh.

## 4. Đã xong, không cần làm lại

| Story | Ở đâu |
| --- | --- |
| A-10 Danh bạ nhân viên từ gói đồng bộ | `SyncEmployeeDirectoryUseCase`, `EmployeeDirectorySyncJob` (30 giây, tắt bằng `EMPLOYEE_SYNC_ENABLED=false`), `EmployeeSyncTest` |
| A-01 (bốn tiêu chí đầu) | `RedisAdaptersIT` |
| Tạo database `gate_store` | `docker/init/edge/02-gate-store.sql`, `GateStoreConfig`, `GateStoreIT` |

## 5. Kiểm tích hợp tùy chọn

Không story nào ở trên chờ những mục này. Làm vào ngày kiểm tích hợp nếu bên kia đã hợp nhất; nếu chưa, ghi "chưa kiểm" và đi tiếp.

| Mã | Ngày | Cùng team | Kiểm gì |
| --- | --- | --- | --- |
| I-1 | 19/11 | B | A-03 với attendance thật: xóa khóa Redis, lượt `ENTRY` thứ hai vẫn bị chặn |
| I-2 | 19/11 | C | C-02: HR đổi trạng thái nhân viên ở user-qr, sau tối đa 30 giây cổng trả `EMPLOYEE_INACTIVE` |
| I-4 | 23/11 | C, D | A-12: mở thủ công xuất hiện trong `GET /admin/v1/audit` của audit-admin |
| I-5 | 23/11 | C, D | A-11: portal gửi Bearer tới `/security/v1/**` trên profile mặc định |
| I-7 | 23/11 | D | A-09 chạy lại trên stack Docker đầy đủ |

## 6. Hợp đồng Team A giữ ổn định

| Cho ai | Hợp đồng |
| --- | --- |
| Team B | `GET /internal/v1/attempts/{attemptId}/gate-observation` giữ nguyên hành vi (attendance đối soát) |
| Team D | API kiosk `/kiosk/v1/**` giữ nguyên; `kiosk-simulator` và trang `/kiosk` đang gọi; `employees` trong `GET /kiosk/v1/sync-bundle` giữ định dạng |

Thêm trường thì sửa OpenAPI trong cùng PR và mời một người của bên dùng duyệt; đổi phá vỡ cần ADR.

## 7. Bẫy cần tránh

- **Không bao giờ gửi lại lệnh mở sau `TIMEOUT`.** `HttpTurnstileAdapter` chỉ thử lại khi chưa kết nối được.
- **Thứ tự trong `RecordScanUseCase` là có chủ ý:** ghi `GATE_COMMANDED` trước, ra lệnh sau.
- **Mọi lối thoát sớm phải nhả token và chỗ anti-passback**, trừ khi kết quả là "chưa biết".
- **Adapter Redis và adapter bộ nhớ phải cùng hành vi.** Thêm thao tác ghi `last` thì thêm ở cả hai và test cả hai.
- **Audit và thay đổi `gates` cùng một transaction** trên `gate_store`, giống outbox của attendance.
- **Lớp trong `application` và `domain` không có chú thích Spring.** Nối dây ở `bootstrap`.
