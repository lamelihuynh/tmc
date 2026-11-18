# Team D: Kiosk, Portal và Central

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này. Lịch chung: [01-ke-hoach.md](01-ke-hoach.md).

> **Bạn có thể bắt đầu ngay (16/11)**
>
> **Đã có sẵn cho Team D** (bước gỡ phụ thuộc, nhánh `chore/x/decouple-teams`):
>
> - D-01 xong và đã kiểm chứng: 7 jar ở profile mặc định trên hạ tầng `make up` (kết quả ở `docs/runbook/troubleshooting.md`, mục "Kết quả D-01").
> - D-03 phần lớn xong: consumer RabbitMQ thật ở reporting (`tmc.reporting.attendance`), hr-integration (`tmc.hr.attendance`), audit-admin (`tmc.audit.events`), mỗi hàng đợi có DLQ `<tên>.dlq`, thử lại 3 lần, dedup JDBC bảng `processed_messages` (Flyway V1 của từng service); IT `AttendanceEventListenerIT`, `HrAttendanceListenerIT`, `AuditEventListenerIT` xanh.
> - Reporting đã áp `AttendanceCorrected` (`ProjectionTest.should_apply_a_correction_event_on_top_of_the_original` đã bật). hr-integration đã bind `attendance.AttendanceCorrected` nhưng use case chưa xử lý (D-06).
> - `local-stack.sh` và `local-stack.ps1` cùng khởi động 5 tiến trình (có identity); job `demo` của CI kiểm login và JWKS.
> - identity cấp JWT, user-qr nhận Bearer; gói đồng bộ có `employees` và gate-station chuyển tiếp nó qua `GET /kiosk/v1/sync-bundle`.
>
> **Lệnh:**
>
> ```bash
> ./gradlew :services:reporting:test :services:hr-integration:test :services:audit-admin:test :simulators:kiosk-simulator:test
> ./gradlew :services:reporting:integrationTest :services:hr-integration:integrationTest :services:audit-admin:integrationTest   # cần Docker
> (cd apps/employee-app && npm ci && npm test && npm run lint && npm run build)
> (cd apps/web-portal && npm ci && npm test && npm run lint && npm run build)
> make up            # hạ tầng thật; container edge cũ thiếu database gate_store thì: make nuke && make up
> ```
>
> **Code theo:** `docs/api/attendance-events.asyncapi.yaml` (mọi sự kiện, `CorrectionPayload`), `docs/api/hr-payroll.openapi.yaml`, `docs/api/central.openapi.yaml`, `docs/api/identity.openapi.yaml`, `docs/api/kiosk-edge.openapi.yaml` (`employees` trong sync bundle), `docs/api/fixtures/*`; trong test: sự kiện tự dựng theo AsyncAPI, `TestTokens`, `TmcContainers`, `FakeEdge` (trong `KioskAppTest`).
>
> **Không chờ team nào.** Mọi story xong được với sự kiện tự tạo, HR giả và token của `TestTokens`. Phần cần mã thật của team khác là kiểm tích hợp **tùy chọn** ở mục 5.

## 1. Nhiệm vụ của team

Team D giữ **hai đầu của hệ thống** và phần gắn kết cả nhóm: thiết bị tại cổng (kiosk, hai ứng dụng web), phần trung tâm nghe sự kiện (báo cáo, HR, kiểm toán), cùng CI, hạ tầng và bản báo cáo cuối.

| Sở hữu | Nội dung |
| --- | --- |
| `simulators/kiosk-simulator` | Kiosk dòng lệnh: journal SQLite, xác thực offline, gửi bù |
| `apps/employee-app` (cổng 5174) | App nhân viên và trang kiosk web (Team C phối hợp phần token) |
| `apps/web-portal` (cổng 5173) | Cổng thông tin cho bảo vệ, quản đốc, HR |
| `services/reporting` (8084), `services/hr-integration` (8085), `services/audit-admin` (8086) | Read model hiện diện và gian lận; đồng bộ HR/Payroll; nhật ký kiểm toán chỉ thêm |
| `docker`, `scripts`, `.github`, `CODEOWNERS` | Hạ tầng, script, CI, bảo vệ nhánh |
| Tài liệu | Phần HR và triển khai, Task 2, hợp nhất báo cáo cuối |

Hai người: **D1** (lead) giữ hr-integration, kiosk, hạ tầng, CI và hợp nhất báo cáo; **D2** giữ reporting, audit-admin và hai ứng dụng web.

## 2. Bản đồ mã

Ba service trung tâm cùng một khuôn: listener `@RabbitListener` bọc `IdempotentConsumer`, gọi một use case; một controller chỉ đọc.

| Service | Use case nhận sự kiện | Port ra | Adapter hiện có | Test |
| --- | --- | --- | --- | --- |
| `reporting` | `ProjectAttendanceEventUseCase` | `IPresenceRepository` | `InMemoryPresenceRepository` | `ProjectionTest` (2 `@Disabled`), `AttendanceEventListenerIT` |
| `hr-integration` | `SyncAttendanceEventUseCase` | `IHrPayrollPort`, `ISyncLogRepository` | `SimulatedHrPayrollAdapter`, `InMemorySyncLogRepository` | `SyncAttendanceEventTest` (3 `@Disabled`), `HrAttendanceListenerIT` |
| `audit-admin` | `RecordAuditUseCase` | `IAuditStore` (không có hàm sửa hay xóa) | `InMemoryAuditStore` | `AuditTest` (2 `@Disabled`), `AuditEventListenerIT` |

Các test `@Disabled` là đặc tả: mỗi cái ghi `TODO Team D` và yêu cầu tương ứng.

| Module | Tệp chính | Việc |
| --- | --- | --- |
| `kiosk-simulator` | `KioskApp`, `Journal`, `OfflineVerifier`, `HttpEdgeClient`, `KioskSimulatorCli` | Ghi journal trước khi gọi mạng; gửi lại cùng `attempt_id`; tự xác thực khi mất mạng |
| `apps/employee-app` | `src/lib/token.ts`, `deviceKey.ts`, `api.ts`; `src/pages/Login.tsx`, `Home.tsx`, `Kiosk.tsx` | Ký token bằng WebCrypto; trang `/kiosk` quét bằng `jsqr` |
| `apps/web-portal` | `src/App.tsx`, `src/pages/*`, `src/lib/api.ts`, `identity.ts` | Ba trang; thanh danh tính dùng cho phát triển |
| `docker` | `docker-compose.yml`, `init/edge/*.sql`, `init/central/01-databases.sql` | PostgreSQL edge (5432, có `gate_store`), central (5433, có `reporting`, `hr_integration`, `audit_admin`, `identity`), Redis, RabbitMQ |
| `scripts` | `dev/local-stack.sh`, `dev/local-stack.ps1`, `demo/*.sh`, `docs/*.mjs` | Stack local; 3 demo; ảnh chụp và ảnh Gantt |

## 3. Việc phải làm (16/11 đến 25/11)

**Mọi hạn là 23:00 của ngày cuối**, tính theo lúc PR hợp nhất vào `main`. Mức: **B** bắt buộc, **N** nên có, **T** chỉ làm nếu còn thời gian.

| Story | Mức | Người | Bắt đầu | Hạn |
| --- | --- | --- | --- | --- |
| Việc 0: hợp nhất nhánh `chore/x/decouple-teams` vào `main` | B | D1 (bốn lead duyệt) | 16/11 sáng | 16/11 |
| D-06 Adapter HR/Payroll và `AttendanceCorrected` | B | D1 | 16/11 | 17/11 |
| D-07 Cảnh báo hạn chót ngày 1 | B | D1 | 18/11 | 18/11 |
| D-02 (phần còn lại) CODEOWNERS, bảo vệ nhánh, 5 check bắt buộc | B | D1 | 19/11 sáng | 19/11 |
| D-03 (phần còn lại) Cảnh báo DLQ | N | D1 | 19/11 chiều | 19/11 |
| D-15 Kiosk dùng `employees` khi offline | N | D1 | 20/11 | 20/11 |
| D-12 Hợp nhất báo cáo và nộp | B | D1 (D2 hỗ trợ 25/11) | 23/11 | 25/11 (nộp 26/11) |
| D-05 Quy tắc gian lận R4 | B | D2 | 16/11 | 16/11 |
| D-04 Read model JDBC chung transaction với dedup | B | D2 | 17/11 | 18/11 sáng |
| D-08 Kho audit bền và chuỗi băm | B | D2 | 18/11 chiều | 19/11 |
| D-13 Portal và app nhân viên gửi Bearer | N | D2 | 20/11 | 23/11 sáng |
| D-14 Test kiosk trên Windows | N | D2 | 23/11 chiều | 23/11 |
| D-16 Thử trang `/kiosk` bằng webcam và điện thoại thật | T | D2 | 24/11 (trong tổng duyệt) | 24/11 |

Ngày 24/11: đóng băng mã lúc 23:00; D1 điều phối tổng duyệt demo lần 1 trên mã đóng băng. Ngày 25/11: hợp nhất báo cáo, tổng duyệt lần 2, dự phòng. Ngày 26/11: nộp.

### Việc 0. Hợp nhất nhánh gỡ phụ thuộc (D1, sáng 16/11)

Mọi "đã có sẵn" trong bộ tài liệu này nằm trên nhánh `chore/x/decouple-teams`. D1 mở PR vào `main`, bốn lead duyệt, CI xanh trên GitHub (lần đầu cho các thay đổi này), squash merge. Sau đó mọi người `git switch main && git pull` rồi mới tạo nhánh story. CI đỏ thì sửa ngay trên nhánh đó trước khi hợp nhất.

### D-06. Adapter HR/Payroll và `AttendanceCorrected` (B, M, D1, 16 đến 17/11)

| | |
| --- | --- |
| Mục tiêu | Gửi công sang HR/Payroll qua HTTP, không bao giờ gửi trùng; bản hiệu chỉnh cũng được gửi |
| Sửa ở | `adapter/out/http/HttpHrPayrollAdapter.java` (mới); mở rộng `IHrPayrollPort` với thao tác tra cứu; `SyncAttendanceEventUseCase` xử lý `UNKNOWN` (tra cứu trước, chỉ gửi lại khi HR xác nhận chưa nhận) và xử lý `AttendanceCorrected` (bản có `aggregateVersion` lớn hơn thay bản cũ) |
| Code theo | `docs/api/hr-payroll.openapi.yaml`; `CorrectionPayload` trong AsyncAPI; HR giả là máy chủ HTTP trong test (cách `HttpTurnstileAdapterTest` của gate-station làm) |
| Xong khi | Bật `SyncAttendanceEventTest.should_look_up_the_result_before_resending_an_unknown_event`; test mới cho `AttendanceCorrected` (sự kiện tự dựng); `HrAttendanceListenerIT` vẫn xanh. Không làm scheduler có backoff |

### D-07. Cảnh báo hạn chót ngày 1 (B, M, D1, 18/11)

| | |
| --- | --- |
| Mục tiêu | Bản ghi chưa `SYNCED` của kỳ vừa qua: cảnh báo lúc 08:00 ngày 1, báo động nếu còn sót lúc 12:00 (NFR-08) |
| Sửa ở | `services/hr-integration`: use case kiểm tra, scheduler gọi use case |
| Code theo | `IClock` giả (`platform/common-kernel`) |
| Xong khi | Bật `should_raise_a_deadline_alert_before_noon_on_the_first_day_of_the_month`. `should_send_a_correction_batch_for_missing_or_failed_records` vẫn `@Disabled` (lô chốt bị cắt) |

### D-02 (phần còn lại). CODEOWNERS, bảo vệ nhánh, 5 check bắt buộc (B, S, D1, 19/11 sáng)

| | |
| --- | --- |
| Mục tiêu | `main` được bảo vệ đúng [`CONTRIBUTING.md`](../../CONTRIBUTING.md) |
| Việc | Làm theo [`release-checklist.md`](../runbook/release-checklist.md), mục "Việc còn mở trước khi bật bảo vệ nhánh": lead A, B, C gửi tài khoản GitHub trước họp đứng 19/11; thay `@REPLACE-ME-team-*` trong `CODEOWNERS`; bật bảo vệ nhánh với **5 check bắt buộc: `build`, `employee-app`, `portal`, `integration`, `demo`** (đúng tên job trong `.github/workflows/ci.yml`) |
| Xong khi | PR thử cố ý làm đỏ một test không hợp nhất được; đẩy thẳng lên `main` bị từ chối; GitHub không báo lỗi CODEOWNERS. Lead nào chưa gửi tài khoản thì giữ handle giữ chỗ, chưa bật "Require review from Code Owners", ghi vào nhật ký quyết định. Không chặn team nào |

### D-03 (phần còn lại). Cảnh báo DLQ (N, S, D1, 19/11 chiều)

| | |
| --- | --- |
| Mục tiêu | Thông điệp rơi vào `<hàng đợi>.dlq` thì có người biết |
| Sửa ở | Ba service trung tâm: log mức `WARN` và chỉ số (hoặc endpoint chỉ đọc) số thông điệp trong DLQ |
| Code theo | `HrAttendanceListenerIT.should_park_a_message_that_keeps_failing_in_the_dead_letter_queue` |
| Xong khi | Mở rộng IT đó: sau khi thông điệp vào DLQ, chỉ số hoặc endpoint báo 1 |

### D-15. Kiosk dùng `employees` khi offline (N, M, D1, 20/11)

| | |
| --- | --- |
| Mục tiêu | Khi mất mạng, kiosk từ chối nhân viên `INACTIVE` hoặc không được vào nhà máy này, như gate-station làm khi online |
| Sửa ở | `simulators/kiosk-simulator`: `KioskApp` lưu `employees` từ `GET /kiosk/v1/sync-bundle` vào journal; `OfflineVerifier` kiểm trạng thái và nhà máy |
| Code theo | `employees[]` trong `kiosk-edge.openapi.yaml`; `docs/api/fixtures/employee-seed.json`; `FakeEdge` |
| Xong khi | Test trong `KioskAppTest`: offline, nhân viên `INACTIVE` bị từ chối với `EMPLOYEE_INACTIVE`, nhân viên chỉ có `BD` bị từ chối ở kiosk `LA`; gói đồng bộ cũ không có `employees` thì giữ hành vi cũ |

### D-12. Hợp nhất báo cáo và nộp (B, L, D1, 23 đến 25/11; nộp 26/11)

| | |
| --- | --- |
| Đầu vào | Bản nháp của A, B, C trong `docs/report/` (20/11), bản cuối (24/11); số đo A-09 (23/11) |
| Việc | Cập nhật `docs/document.md` theo `docs/adr/README.md` mục 3; viết phần HR, triển khai và Task 2 (module đã hiện thực, cách chạy, ảnh kết quả test và demo, URL GitHub); hợp nhất một giọng văn; đánh số hình; gom "Hạn chế và việc tiếp theo" từ [01-ke-hoach.md](01-ke-hoach.md) mục 6 |
| Xong khi | Qua hết [`release-checklist.md`](../runbook/release-checklist.md); PDF xong 25/11; nộp 26/11. Ảnh và số liệu chụp trên mã đã đóng băng (24/11) |

### D-05. Quy tắc gian lận R4 (B, M, D2, 16/11)

| | |
| --- | --- |
| Mục tiêu | Lớp chống dùng lại thứ tư của ADR-12 |
| Sửa ở | `services/reporting/.../domain/`: `ImpossibleTravelRule`, `RepeatedDenialRule`; gọi từ `ProjectAttendanceEventUseCase`; lưu và truy vấn cảnh báo |
| Code theo | `FraudRule`; sự kiện tự dựng theo AsyncAPI |
| Xong khi | Bật `ProjectionTest.should_flag_two_entries_in_different_factories_ten_minutes_apart` |

### D-04. Read model JDBC chung transaction với dedup (B, M, D2, 17 đến 18/11 sáng)

| | |
| --- | --- |
| Mục tiêu | Hiện diện không mất khi reporting khởi động lại; ghi read model và đánh dấu `processed_messages` cùng một transaction |
| Sửa ở | `adapter/out/persistence/JdbcPresenceRepository.java` (mới), migration `V2__presence.sql` trong database `reporting`; dùng `IdempotentConsumer.TransactionRunner` |
| Code theo | `MessagingInfraIT.should_roll_the_dedup_marker_back_when_the_handler_fails_in_the_same_transaction`; `Presence.supersededBy` (so `aggregateVersion`) |
| Xong khi | IT mới: chiếu sự kiện, khởi động lại context, hiện diện còn; handler ném lỗi thì cả read model lẫn dấu dedup đều không được ghi; `AttendanceEventListenerIT` vẫn xanh. Không làm báo cáo theo nhóm, ca, xuất file (`should_build_a_group_attendance_report_within_the_role_scope` vẫn `@Disabled`) |

### D-08. Kho audit bền và chuỗi băm (B, M, D2, 18/11 chiều đến 19/11)

| | |
| --- | --- |
| Mục tiêu | Nhật ký kiểm toán chỉ thêm trên PostgreSQL, phát hiện được sửa dữ liệu (AC-05) |
| Sửa ở | `services/audit-admin`: `JdbcAuditStore` với migration `V2__audit_entries.sql` trong `audit_admin` (role của service chỉ có `INSERT`, `SELECT`); mỗi `AuditEntry` mang băm của nội dung nối với băm bản trước; thao tác kiểm cả chuỗi |
| Code theo | `AuditRecorded` trong AsyncAPI; `IAuditStore` (không thêm hàm sửa, xóa) |
| Xong khi | Bật `AuditTest.should_detect_a_modified_entry_through_the_hash_chain`; `should_have_no_way_to_change_or_remove_history_through_the_store_port` vẫn xanh; IT mới: `UPDATE` bằng role của service bị từ chối. `should_meet_the_query_time_and_retention_requirements` vẫn `@Disabled` |

### D-13. Portal và app nhân viên gửi Bearer (N, M, D2, 20 đến 23/11 sáng)

| | |
| --- | --- |
| Mục tiêu | Hai ứng dụng web dùng JWT của identity; ba service trung tâm nhận Bearer |
| Sửa ở | `apps/employee-app/src/lib/api.ts`, `apps/web-portal/src/lib/api.ts`, `identity.ts`: lưu `accessToken` từ câu trả lời đăng nhập, gửi `Authorization: Bearer`; token hết hạn thì về màn hình đăng nhập. Reporting, hr-integration, audit-admin: bean `PrincipalResolver` theo mẫu `UserQrConfig.principalResolver` |
| Code theo | `docs/api/identity.openapi.yaml`; `TestTokens` cho test Java; token giả trong test vitest |
| Chuyển tiếp | Ở profile `local`, ứng dụng web gửi **cả** Bearer **và** header dev: service đã có resolver ưu tiên Bearer, service chưa có vẫn đọc header. Vì vậy không phải chờ A-11 hay B-06 |
| Xong khi | Test vitest: header `Authorization` được gửi, 401 đưa về đăng nhập; test MockMvc ở mỗi service trung tâm: Bearer hợp lệ được, token lạ 401. `npm test`, `npm run lint`, `npm run build` xanh ở cả hai app |

### D-14. Test kiosk trên Windows (N, S, D2, 23/11 chiều)

`Journal` đã là `AutoCloseable`, nhưng `KioskAppTest` chưa đóng nó trong `@AfterEach`, nên trên Windows thư mục tạm không xóa được (`kiosk.db` bị khóa). **Xong khi:** `.\gradlew.bat :simulators:kiosk-simulator:test` xanh trên một máy Windows; bỏ dòng `-x :simulators:kiosk-simulator:test` trong [02-chay-local.md](02-chay-local.md). Không có máy Windows thì ghi "chưa kiểm chứng".

### D-16. Thử trang `/kiosk` bằng webcam và điện thoại thật (T, D2, 24/11 trong tổng duyệt)

Chạy `npm run dev:lan`, quét QR trên điện thoại thật bằng webcam ([02-chay-local.md](02-chay-local.md) mục 5.4, 5.5); ghi kết quả vào `docs/runbook/troubleshooting.md`. Không đổi mã. Không có thiết bị thì bỏ, ghi vào phần hạn chế.

## 4. Đã xong, không cần làm lại

| Story | Ở đâu |
| --- | --- |
| D-01 Kiểm chứng hạ tầng Docker | `docs/runbook/troubleshooting.md`, mục "Kết quả D-01" |
| D-03 Consumer thật, DLQ, dedup JDBC | Xem hộp đầu tài liệu |
| D-02 CI 5 job | `.github/workflows/ci.yml`; còn bảo vệ nhánh (ở trên) |
| Reporting áp `AttendanceCorrected` | `ProjectionTest`, `AttendanceEventListenerIT` |

## 5. Kiểm tích hợp tùy chọn

Team D điều phối các buổi kiểm tích hợp nhưng không story nào của team chờ chúng.

| Mã | Ngày | Cùng team | Kiểm gì |
| --- | --- | --- | --- |
| I-3 | 19/11 | B | Hiệu chỉnh thật qua RabbitMQ: reporting áp, hr-integration gửi bản hiệu chỉnh |
| I-4 | 23/11 | A, C | Audit từ user-qr, identity, gate-station vào `GET /admin/v1/audit` trên kho bền (D-08) |
| I-5 | 23/11 | A, C | App và portal dùng Bearer trên profile mặc định, tới user-qr, gate-station và ba service trung tâm |
| I-6 | 23/11 | cả nhóm | 7 service ở profile mặc định và turnstile-simulator trên `make up`, chạy 3 demo (hiện **chưa kiểm chứng**, chỉ mới chạy trên `local`) |
| I-7 | 23/11 | A | Đo tải A-09 trên stack Docker |

## 6. Bẫy cần tránh

- **Consumer phải lũy đẳng.** Ghi "đã xử lý" và cập nhật dữ liệu cùng một transaction.
- **Thông điệp đến sai thứ tự.** So `aggregateVersion`; bản hiệu chỉnh có phiên bản lớn hơn bản gốc.
- **HR không trả lời nghĩa là "chưa biết".** Tra cứu trước khi gửi lại.
- **Nhật ký kiểm toán không có hàm sửa hay xóa.** Có test kiểm bằng reflection.
- **Bộ xác thực offline của kiosk phải cùng quy tắc với `user-qr`** (`QrTokenPolicyContractTest`).
- **Trang kiosk web không thay được `kiosk-simulator`.** Demo ADR-11 dùng bản dòng lệnh.
- **Sửa `docker`, `scripts`, CI là sửa cho cả nhóm.** `local-stack.sh` và `local-stack.ps1` phải khởi động cùng một bộ service.

## 7. Phần báo cáo của Team D

Viết trong D-12 (23 đến 25/11): sơ đồ và mô tả tích hợp HR, triển khai; bằng chứng phía kiosk cho ADR-11 (P6) và ADR-12 (R2, R4); SOLID (`IHrPayrollPort` với bản giả lập và bản HTTP, `IAuditStore` chỉ thêm, `FraudRule`); Task 2; một đoạn phản ánh.
