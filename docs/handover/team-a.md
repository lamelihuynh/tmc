# Team A: Gate Station

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này.

## 1. Nhiệm vụ của team

Team A giữ **bộ não tại cổng**: nhận lượt quét từ kiosk, quyết định mở hay không, ra lệnh cho cổng xoay, và bảo đảm cổng không bao giờ mở hai lần cho một lượt quét.

| Sở hữu | Nội dung |
| --- | --- |
| `services/gate-station` (cổng 8081) | Điều phối lượt quét, quy tắc ra vào, anti-passback, giữ chỗ token, mở thủ công |
| `simulators/turnstile-simulator` (cổng 8090) | Cổng xoay giả lập, chèn lỗi |
| Hợp đồng | `docs/api/kiosk-edge.openapi.yaml`, `docs/api/turnstile.openapi.yaml` |
| Tài liệu | ADR-13, phần luồng quét QR của báo cáo |

## 2. Bản đồ mã của gate-station

Gói gốc: `services/gate-station/src/main/java/vn/tmc/gatestation/`.

| Gói | Lớp chính | Việc |
| --- | --- | --- |
| `domain` | `GateConfig`, `GateMode`, `EmployeeAccess`, `LastPass`, `DirectionResolver` | Mô hình cổng và chiều ra vào |
| `domain.policy` | `AccessPolicyChain`, `ActiveEmployeeRule`, `FactoryPermissionRule`, `AntiPassbackRule` | Quy tắc ra vào; thêm quy tắc mới bằng cách thêm một lớp `AccessPolicy` |
| `application.usecase` | **`RecordScanUseCase`** | Luồng 7 bước của một lượt quét |
| | `ManualOpenUseCase` | Bảo vệ mở cổng thủ công, bắt buộc có lý do |
| | `ObserveGateUseCase` | Hỏi cổng "lệnh kia đã làm gì" cho attendance đối soát |
| | `KioskSupportService` | Gói đồng bộ, heartbeat, nhập lô offline |
| `application.port.out` | `IAttendanceGateway`, `IQRValidation`, `ITokenReservation`, `IAntiPassbackStore`, `ITurnstileControlPort`, `IGateConfigRepository`, `IEmployeeDirectory`, `IKioskSyncSource`, `IHeartbeatStore`, `IAuditLog` | Mọi thứ use case cần ở bên ngoài |
| `adapter.in.rest` | `KioskController`, `SecurityController`, `InternalController` | API cho kiosk, bảo vệ, attendance |
| `adapter.out.inmem` | `InMemoryTokenReservation`, `InMemoryAntiPassbackStore`, `FakeAttendanceGateway`, `ProgrammableQrValidation`, ... | Bản dùng cho test và profile `local` |
| `adapter.out.redis` | `RedisTokenReservation`, `RedisAntiPassbackStore` | Bản thật, **chưa kiểm chứng** |
| `adapter.out.http` | `HttpAttendanceGateway`, `HttpQrValidationClient`, `HttpKioskSyncSource`, `HttpTurnstileAdapter` | Gọi attendance, user-qr, cổng xoay |
| `adapter.out.simulated` | `SimulatedTurnstileAdapter` | Cổng xoay trong tiến trình, chèn lỗi được |
| `bootstrap` | `GateStationConfig`, `InMemoryProfileConfig`, `InfrastructureProfileConfig`, `GateProperties` | Chọn adapter theo profile |

Dữ liệu mẫu (cổng và nhân viên) nằm ở `src/main/resources/application.yml`, mục `tmc.gate`.

## 3. Chạy và kiểm thử phần của team

```bash
# toàn bộ test của team
./gradlew :services:gate-station:test :simulators:turnstile-simulator:test

# một lớp test
./gradlew :services:gate-station:test --tests '*RecordScanUseCaseTest'

# test tích hợp (cần Docker), sau khi A-01 thêm vào
./gradlew :services:gate-station:integrationTest
```

| Lớp test | Kiểm gì |
| --- | --- |
| `RecordScanUseCaseTest` | Luồng quét, mọi nhánh từ chối, gửi lại, hết giờ chờ, lỗi phần cứng |
| `ManualOpenUseCaseTest` | Quyền của bảo vệ, lý do bắt buộc, audit |
| `AccessPolicyTest` | Từng quy tắc ra vào |
| `KioskApiTest` | API qua MockMvc với profile `inmem` |
| `HttpTurnstileAdapterTest` | Ánh xạ mã HTTP của cổng xoay sang `OPENED`, `ERROR`, `TIMEOUT` |
| `ArchitectureTest` | Chiều phụ thuộc giữa các gói |

`GateFixture` (trong `src/test/.../support`) dựng cả ứng dụng trên adapter trong bộ nhớ, không cần Spring. Viết test use case mới thì bắt đầu từ đây.

Chạy riêng gate-station không cần service nào khác: `SPRING_PROFILES_ACTIVE=inmem ./gradlew :services:gate-station:bootRun`. Chạy cùng các service thật: xem runbook mục 3.

## 4. Việc phải làm

**Mọi hạn là 23:00 của ngày được ghi**, tính theo thời điểm PR hợp nhất vào `main`. Khi một story ghi khoảng ngày (ví dụ "16 đến 17/11"), hạn là 23:00 của ngày cuối. Toàn cảnh lịch: [03-gantt.md](03-gantt.md).

Cỡ: S khoảng nửa ngày, M một đến hai ngày, L ba ngày, tính cho một người.

### A-05. Test hợp đồng với turnstile-simulator thật (nên có, S, 12/11, A2)

`HttpTurnstileAdapterTest` hiện chạy với một máy chủ HTTP tự dựng. Thêm test chạy `HttpTurnstileAdapter` với chính `turnstile-simulator` (khởi động bằng `@SpringBootTest` ở cổng ngẫu nhiên).

- **Sửa ở:** `simulators/turnstile-simulator/src/test/` hoặc một test mới trong gate-station gọi qua HTTP.
- **Xong khi:** test bao phủ các chế độ lỗi `JAM`, `OFFLINE`, `GATEWAY_TIMEOUT`, `DELAY_OPENED`, `DELAY_NOT_OPENED` và kiểm ngưỡng chờ 800 ms.

### A-01. Kiểm chứng adapter Redis (bắt buộc, M, 16 đến 17/11, A2)

Hai lớp `RedisTokenReservation` và `RedisAntiPassbackStore` đã viết nhưng chưa từng chạy với Redis. Việc của story này là chứng minh chúng đúng, sửa nếu sai.

- **Sửa ở:** `adapter/out/redis/`, thêm `src/test/.../adapter/RedisAdaptersIT.java` gắn thẻ `@Tag("integration")`. Thêm phụ thuộc Testcontainers vào `build.gradle.kts` (xem cách attendance khai báo).
- **Xong khi** có test chứng minh:
  1. Hai luồng cùng `reserve` một `jti`: đúng một bên được `true`.
  2. `release` không xóa token đã `markUsed`.
  3. Chỗ `pending` chặn lượt quét thứ hai của cùng nhân viên, và tự hết hạn sau TTL.
  4. `confirm` chuyển `pending` thành `last`.
  5. Khởi động lại container Redis (bật AOF) thì khóa `USED` vẫn còn.
- **Lưu ý:** hành vi phải giống hệt bản trong bộ nhớ. Cách chắc nhất là viết một bộ test chung chạy trên cả hai bản.

### A-04. Kho cấu hình cổng bằng PostgreSQL (bắt buộc, L, 13 đến 19/11, A1)

Thay `InMemoryGateConfigRepository` (đọc từ `application.yml`) bằng bảng PostgreSQL có API quản trị.

- **Ngày 13/11:** chốt lược đồ bảng và API, đưa vào `docs/api/kiosk-edge.openapi.yaml` (hoặc tệp OpenAPI quản trị riêng).
- **Sửa ở:** thêm `adapter/out/persistence/JdbcGateConfigRepository.java`, migration `src/main/resources/db/migration/V1__gate_store.sql`, controller quản trị trong `adapter/in/rest/`, use case trong `application/usecase/`, nối ở `InfrastructureProfileConfig`. Mở rộng `IGateConfigRepository` với các thao tác ghi.
- **Xong khi:** `SYSTEM_ADMIN` thêm, sửa, đặt bảo trì một cổng qua API; vai trò khác nhận 403; mỗi thay đổi ghi `IAuditLog`; đặt bảo trì thì lượt quét kế tiếp nhận `GATE_IN_MAINTENANCE` ngay; profile `inmem` và `local` vẫn chạy bằng dữ liệu mẫu.
- **Cần quyết định trước:** gate-station dùng chung PostgreSQL edge với attendance nhưng **khác schema hoặc khác database** (ADR-10: mỗi service một kho). Báo Team D để thêm vào `docker/docker-compose.yml`.

### A-03. Dựng lại anti-passback khi Redis mất dữ liệu (bắt buộc, M, 18 đến 19/11, A2)

Nếu Redis mất khóa `last`, nhân viên đã vào có thể vào lần nữa. Khi không có `last`, hỏi attendance.

- **Phụ thuộc:** H2 (B-05, hạn 13/11) và A-01.
- **Sửa ở:** thêm phương thức `lastDirection(UUID)` vào `IAttendanceGateway`, hiện thực ở `HttpAttendanceGateway` và `FakeAttendanceGateway`; gọi nó trong `RecordScanUseCase` khi `antiPassback.last(...)` trống, rồi ghi lại vào store.
- **Xong khi:** test xóa trạng thái anti-passback sau một lượt `ENTRY`, lượt `ENTRY` thứ hai ở cổng một chiều vẫn bị `ANTI_PASSBACK_VIOLATION`.
- **Nếu B-05 trễ:** viết theo hợp đồng trong OpenAPI, test bằng `FakeAttendanceGateway`. Phần HTTP nối sau.
- **Cân nhắc:** attendance chết thì sao? Lượt quét không được biến thành chấp thuận do thiếu dữ liệu (fail-secure), nhưng cũng không nên chặn mọi người. Ghi quyết định vào mô tả PR.

### A-02. Đồng bộ anti-passback sau khi nhập lô offline (bắt buộc, M, 19 đến 20/11, A2)

Lỗ hổng thật, mô tả ở [`implementation-notes.md`](../architecture/implementation-notes.md) mục 3: nhân viên vào lúc mất mạng, kiosk gửi bù, rồi vào lại online ngay thì không bị chặn, vì `IngestOfflineBatchUseCase` chỉ ghi vào attendance.

- **Sửa ở:** `KioskSupportService.ingestOffline`: với mỗi bản ghi được attendance trả `ACCEPTED`, cập nhật `last` của nhân viên trong `IAntiPassbackStore`. Cần thêm một phương thức ghi thẳng `last` (ví dụ `recordConfirmed(employee, direction, at)`) vào port và cả hai adapter.
- **Xong khi:** test quét offline `ENTRY`, gửi lô, rồi quét online `ENTRY` ở cổng một chiều bị `ANTI_PASSBACK_VIOLATION`.
- **Lưu ý:** chỉ ghi đè `last` khi bản ghi offline **mới hơn** `last` hiện có, vì lô offline có thể tới muộn.

### A-10. Danh bạ nhân viên từ gói đồng bộ (nên có, S đến M, 20/11, A1)

Hiện `IEmployeeDirectory` đọc danh sách cứng từ `application.yml`. Sau khi Team C thêm `employees` vào sync bundle (C-02), gate-station đọc từ đó.

- **Phụ thuộc:** H6 (hợp đồng, 13/11) và H11 (dữ liệu thật, 19/11).
- **Sửa ở:** adapter mới hiện thực `IEmployeeDirectory` lấy dữ liệu từ user-qr và làm mới định kỳ; nối ở `InfrastructureProfileConfig`.
- **Xong khi:** vô hiệu hóa một nhân viên ở user-qr thì sau một chu kỳ làm mới, cổng trả `EMPLOYEE_INACTIVE`, không cần khởi động lại.
- **Nếu C-02 trễ:** cắt A-10, giữ danh sách trong `application.yml`, ghi vào phần hạn chế.

### A-06. Trả `direction` khi trả lời lượt gửi lại (nên có, S, 20/11, A1)

Khi kiosk gửi lại, `ScanResult.direction` hiện là `null`. Sau B-04, `AttemptView` có `direction`.

- **Sửa ở:** thêm `direction` vào `IAttendanceGateway.AttemptState`, điền ở `HttpAttendanceGateway.toState`, dùng trong `RecordScanUseCase.replay`.
- **Xong khi:** test gửi lại nhận đủ chiều như lần đầu.

### A-09. Kiểm thử tải (bắt buộc, M, 23/11, cả hai)

Đo NFR-06: 800 lượt trong 30 phút, p95 không quá 1,0 giây, p99 không quá 2,0 giây.

- **Phụ thuộc:** hạ tầng thật đã chạy (M2): A-01, B-01.
- **Cách làm:** một script hoặc test sinh token hợp lệ (dùng `device-simulator` hoặc mã của `PhoneSimulator`), gọi `POST /kiosk/v1/scans` với nhiều nhân viên, ghi thời gian từng lượt. Cổng xoay dùng `turnstile-simulator` không lỗi.
- **Xong khi:** có bảng số đo (p50, p95, p99, tỷ lệ lỗi), cấu hình máy, và lệnh để chạy lại; nộp cho Team D đưa vào báo cáo.
- **Ghi trung thực:** đo trên máy cá nhân với cổng giả lập không phải là đo trên hệ thống thật. Ghi rõ điều kiện đo.

## 5. Team A cần gì và nợ gì

| Chiều | Hạn (23:00) | Nội dung |
| --- | --- | --- |
| Nhận từ B | 13/11 | B-05 `last-direction` (cho A-03), B-04 `direction` trong `AttemptView` (cho A-06) |
| Nhận từ C | 13/11, 19/11 | Hợp đồng rồi dữ liệu `employees` trong sync bundle (cho A-10) |
| Nhận từ D | 12/11, 16/11 | Compose chạy được; CI |
| Giao cho B | luôn luôn | `GET /internal/v1/attempts/{id}/gate-observation` giữ nguyên hành vi (attendance dùng để đối soát) |
| Giao cho D | luôn luôn | API kiosk `/kiosk/v1/**` giữ nguyên; `kiosk-simulator` và `apps/employee-app` đang gọi |
| Giao cho D | 20/11, 23/11 | Bản nháp báo cáo; số đo A-09 |

**Đổi API kiosk hoặc API nội bộ thì phải báo trước.** Hai client của Team D (`kiosk-simulator`, trang `/kiosk`) và reconcile của Team B phụ thuộc vào chúng. Đổi tương thích ngược (thêm trường) thì sửa OpenAPI trong cùng PR; đổi phá vỡ thì cần ADR.

## 6. Bẫy cần tránh

- **Không bao giờ gửi lại lệnh mở sau `TIMEOUT`.** `HttpTurnstileAdapter` chỉ thử lại khi chưa kết nối được (lệnh chưa rời máy). Giữ nguyên tính chất này.
- **Thứ tự trong `RecordScanUseCase` là có chủ ý:** ghi `GATE_COMMANDED` trước, ra lệnh sau. Đảo thứ tự là mất khả năng phục hồi.
- **Mọi lối thoát sớm phải nhả token và chỗ anti-passback**, trừ khi kết quả là "chưa biết".
- **Adapter Redis và adapter bộ nhớ phải cùng hành vi.** Test use case chạy trên bản bộ nhớ; nếu hai bản lệch nhau, test xanh mà chạy thật thì sai.
- **Lớp trong `application` và `domain` không có chú thích Spring.** Nối dây ở `bootstrap`.

## 7. Phần báo cáo của Team A

Nộp bản nháp ngày 20/11 vào `docs/report/`:

- Sơ đồ sequence của luồng quét QR, khớp với `RecordScanUseCase` hiện tại.
- Bằng chứng cho ADR-11 phía gate-station: tên test cho P1, P3, P5 và kịch bản `demo-timeout`.
- Áp dụng SOLID trên mã của team, ví dụ: `AccessPolicyChain` (mở rộng không sửa), `ITurnstileControlPort` với ba adapter (thay thế được), các port vào tách riêng `IGateAccess`, `IGateManualAccess`, `IGateObservation`, `IKioskSupport` (interface nhỏ).
- Một đoạn phản ánh: điều gì khó, điều gì kiến trúc này giúp được.
