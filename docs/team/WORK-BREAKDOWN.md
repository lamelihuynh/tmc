# Phân chia công việc M2 (4 team, mỗi team 2 người)

Tài liệu này giữ **quyền sở hữu, backlog story, hợp đồng giữa các team, phân công báo cáo và định nghĩa "xong"**. Mọi thay đổi phân công phải sửa ở đây trong một PR riêng (`docs(team): ...`) và báo trong họp đứng.

> **Lịch:** 16/11 đến 26/11/2026. Đóng băng mã 23:00 ngày 24/11, hợp nhất báo cáo 25/11, **nộp 26/11**. Lịch từng người từng ngày, kiểm tích hợp tùy chọn và thứ tự cắt nằm ở [`docs/handover/01-ke-hoach.md`](../handover/01-ke-hoach.md); chi tiết từng story (tệp, fixture, tiêu chí) ở `docs/handover/team-*.md`. Kế hoạch 4 tuần cũ (W1 đến W4) đã bỏ; xem mục 7.

## 1. Bốn team và quyền sở hữu

Nguyên tắc chia: **theo bounded context** (ADR-01), không theo actor. Mỗi thư mục chỉ có **một team chủ sở hữu**; team khác muốn sửa phải mở PR và được team chủ sở hữu duyệt (xem `CODEOWNERS`).

| Team | Thành viên | Sở hữu mã nguồn | Sở hữu tài liệu báo cáo |
| --- | --- | --- | --- |
| **A. Gate Station** | `<Tên A1 (lead)>`, `<Tên A2>` | `services/gate-station`, `simulators/turnstile-simulator` | Mục 12.4, 13.1 (luồng quét QR), 16.2, 16.3 (API Kiosk, Turnstile), ADR-13, ADR-14 |
| **B. Attendance** | `<Tên B1 (lead)>`, `<Tên B2>` | `services/attendance` | ADR-11, máy trạng thái, mô hình dữ liệu, mục 12.5, 12.7 (Outbox, nhất quán), phần "Issues Encountered: mất kết nối" |
| **C. User/QR và Identity** | `<Tên C1 (lead)>`, `<Tên C2>` | `services/user-qr`, `services/identity`, `simulators/device-simulator`, `platform/security` | ADR-08, ADR-09, ADR-12, NFR-03/04/05, phần "Issues Encountered: QR tĩnh" |
| **D. Kiosk, Portal và Central** | `<Tên D1 (lead)>`, `<Tên D2>` | `simulators/kiosk-simulator`, `apps/*`, `services/reporting`, `services/hr-integration`, `services/audit-admin`, `.github`, `docker`, `scripts` | Mục 13.2, 15, 16.4 (HR, triển khai), ma trận truy vết, tích hợp và biên tập báo cáo cuối |

Dùng chung (cả bốn team phải duyệt thay đổi): `platform/common-kernel`, `platform/messaging`, `platform/test-support`, `docs/api`, `docs/adr`. Quy tắc ở `CONTRIBUTING.md`.

### Vai trò trong mỗi cặp

| Vai trò | Trách nhiệm |
| --- | --- |
| Lead (người 1) | Chia nhỏ việc thành issue, giữ hợp đồng với team khác, đại diện team trong họp, reviewer cuối cho thư mục của team |
| Đối ứng (người 2) | Làm song song, review chéo mọi PR của lead (không tự merge PR của chính mình), thay lead khi vắng |

Hai người trong một team **luôn review PR của nhau**; thêm **một reviewer thuộc team khác** cho PR chạm vào hợp đồng (API, event, port) hoặc `platform`. Xem `docs/conventions/pr-review-checklist.md`.

## 2. Hiện trạng (16/11/2026)

Số liệu kiểm chứng trên macOS arm64, JDK 21, Docker, sau bước gỡ phụ thuộc ([`docs/handover/_decoupling-changelog.md`](../handover/_decoupling-changelog.md) mục 3): `./gradlew spotlessCheck test` **209 test, 10 bỏ qua, 0 lỗi**; `./gradlew integrationTest` **27 test, 0 lỗi**. Bảng hiện trạng scaffold cũ (145 test, "chưa kiểm chứng mọi thứ cần Docker") không còn đúng và đã bỏ. Những gì còn chưa kiểm chứng: [`docs/handover/README.md`](../handover/README.md), mục "Hiện trạng".

## 3. Mốc thời gian

| Mốc | Ngày (23:00) | Điều kiện hoàn thành (kiểm tra được) |
| --- | --- | --- |
| Việc 0 | 16/11 (sáng) | Nhánh `chore/x/decouple-teams` đã vào `main`, CI xanh |
| K1 Kiểm tích hợp giữa kỳ | 19/11 | I-1, I-2, I-3 đã thử (tùy chọn, không chặn) |
| Bản nháp báo cáo | 20/11 | Team A, B, C có bản nháp trong `docs/report/` |
| K2 Tích hợp đầy đủ | 23/11 | Mọi story mức B đã vào `main` hoặc có quyết định cắt; I-4 đến I-7 đã thử |
| Đóng băng mã | 24/11 | Chỉ còn sửa lỗi; tổng duyệt demo lần 1; bản cuối phần báo cáo của từng team |
| Hợp nhất báo cáo | 25/11 | PDF theo `docs/runbook/release-checklist.md`; tổng duyệt lần 2 |
| Nộp | 26/11 | Báo cáo PDF có URL GitHub trên LMS, thẻ `m2-submission` |

## 4. Backlog theo team

**Mức:** `B` = Bắt buộc, `N` = Nên có, `T` = Tùy thời gian, `Cắt` = không làm trong đợt này. **Cỡ:** S ≤ 0,5 ngày, M ≈ 1 đến 2 ngày, L ≈ 3 ngày (một người). Cột **Trạng thái** tính ngày 16/11. Không story nào phụ thuộc mã chưa xong của team khác; story trong cùng team có thể nối tiếp nhau.

### Team A: Gate Station

| ID | Story | Mức | Cỡ | Trạng thái | Tiêu chí hoàn thành |
| --- | --- | --- | --- | --- | --- |
| A-01 | Adapter Redis với Redis thật (Testcontainers) | B | M | Phần lớn xong (`RedisAdaptersIT` 5 test); còn khởi động lại với AOF, A2 17/11 | Một `jti` chỉ một bên giữ được; pending chặn quét song song; `USED` còn sau khi Redis khởi động lại |
| A-02 | Đồng bộ anti-passback sau nhập offline | B | M | A2, 18 đến 19/11 | Quét offline `ENTRY`, gửi lô, quét online `ENTRY` ở cổng một chiều bị `ANTI_PASSBACK_VIOLATION` |
| A-03 | Dựng lại `last` từ attendance khi Redis mất dữ liệu | B | M | A2, 16/11 (`lastDirection` đã có) | Xóa trạng thái, quét lần hai cùng chiều vẫn bị từ chối |
| A-04 | Kho cấu hình cổng trên `gate_store` (`JdbcGateConfigRepository`), API quản trị cho `SYSTEM_ADMIN` | B | L | A1, 16 đến 18/11 (database `gate_store` đã có, ADR-14) | CRUD cổng có Rbac; bảo trì cổng có hiệu lực ngay |
| A-05 | Test hợp đồng `HttpTurnstileAdapter` với `turnstile-simulator` thật | N | S | A2, 20/11 | 5 chế độ lỗi, ngân sách 800 ms |
| A-06 | `replay` trả `direction` (`AttemptState.direction`) | N | S | A2, 17/11 | Gửi lại nhận đủ chiều như lần đầu |
| A-07 | Cảnh báo anti-passback và mất tín hiệu kiosk cho bảo vệ | Cắt | L | | |
| A-08 | Callback `onPhysicalPassCompleted` từ turnstile | Cắt | M | | |
| A-09 | Kiểm thử tải 800 lượt/30 phút (NFR-06) | B | M | A2 chuẩn bị 20/11, A1 và A2 đo 23/11 | Bảng p50, p95, p99, tỷ lệ lỗi, điều kiện đo, lệnh chạy lại |
| A-10 | Danh bạ nhân viên từ gói đồng bộ | N | M | **Xong** (`SyncEmployeeDirectoryUseCase`, `EmployeeSyncTest`) | Vô hiệu hóa ở user-qr thì cổng từ chối sau một chu kỳ |
| A-11 | `PrincipalResolver` (JWT) ở `SecurityController` và API quản trị cổng | B | S | A1, 20/11 | Bearer hợp lệ được; token hết hạn hoặc khóa lạ 401 |
| A-12 | Audit cổng qua `JdbcOutboxStore` và `AuditOutbox` trên `gate_store` | B | M | A1, 19/11 | Mở thủ công ghi dòng outbox cùng transaction; relay phát `gate-station.AuditRecorded` |

### Team B: Attendance

| ID | Story | Mức | Cỡ | Trạng thái | Tiêu chí hoàn thành |
| --- | --- | --- | --- | --- | --- |
| B-01 | Flyway và repository JDBC với PostgreSQL thật | B | M | Phần lớn xong (`PersistenceAndBrokerIT` 8 test); còn test rollback, B2 16/11 | Use case lỗi giữa chừng thì không còn lượt quét lẫn dòng outbox |
| B-02 | Outbox với RabbitMQ thật, dừng và bật lại broker | B | M | Phát lên `tmc.events` đã chạy; còn test dừng/bật broker, B2 16 đến 17/11 | Không mất, đúng thứ tự, không phát lại dòng đã `PUBLISHED` |
| B-03 | Hàng đợi bất thường cho HR (UC-09), `IAttendanceQuery.anomalies` | B | M | B2, 18 đến 19/11 | HR lọc theo nhà máy và thời gian; vai khác 403 |
| B-04 | `direction`, `employeeUuid` trong `AttemptView` | N | S | **Xong** | |
| B-05 | `lastDirection` cho gate-station | B | S | **Xong** | |
| B-06 | Hiệu chỉnh chấm công (FR-26): use case, endpoint HR | B | L | B1, 16 đến 18/11 (lược đồ, bảng, repository, factory đã có) | Bản mới liên kết bản gốc, `AttendanceCorrected` và `AuditRecorded` cùng transaction, lũy đẳng, chỉ `HR_STAFF` |
| B-07 | Xác minh lượt mở thủ công bởi HR (FR-20) | N | M | B1, 19 đến 20/11 | Lượt đã xác minh sinh `AttendanceConfirmed` |
| B-08 | Chỉ số vận hành: outbox tồn > 5 phút, số `GATE_UNKNOWN` | N | S | B2, 20/11 | Chỉ số ở `/actuator` |
| B-09 | `RecoveryPolicy` cấu hình qua bảng tham số | Cắt | S | | |
| B-10 | `JdbcProcessedMessageStore` trong `platform/messaging` | N | M | **Xong** (`MessagingInfraIT`) | |

### Team C: User/QR và Identity

| ID | Story | Mức | Cỡ | Trạng thái | Tiêu chí hoàn thành |
| --- | --- | --- | --- | --- | --- |
| C-01 | `JdbcDeviceKeyRegistry` với PostgreSQL thật | B | M | Phần lớn xong (`UserQrPersistenceIT`); còn đăng ký song song và 409, C2 16/11 | Hai lệnh song song: đúng một bên thắng, bên kia 409 |
| C-02 | Nguồn HR cho danh bạ nhân viên trong gói đồng bộ (thay `InMemoryEmployeeDirectory`) | B | L | Hợp đồng và bản bộ nhớ đã có; C2 16 đến 18/11 | Đổi trạng thái thì lần đồng bộ sau trả `INACTIVE`, thiết bị bị thu hồi |
| C-03 | Hồ sơ và vòng đời nhân viên đầy đủ | Cắt | L | | |
| C-04 | Identity cấp JWT ES256, JWKS | B | L | **Xong** | |
| C-05 | TOTP cho MFA | B | M | C2, 19/11 | Mã đúng, sai, lệch một bước; không dùng lại |
| C-06 | Xác thực JWT ở service | B | M | **Xong cho user-qr** và `platform/security`; service khác tự gắn (A-11, B-06, D-13) | Token sai 401; header giả bị bỏ qua ở profile mặc định |
| C-07 | Quản lý tài khoản, đổi vai kết thúc phiên | Cắt | L | | |
| C-08 | Audit của user-qr và identity qua `AuditOutbox` | B | M | Bean `AuditOutbox` đã có; C1 16 đến 17/11 | Đăng ký, thu hồi, đăng nhập thành sự kiện; không mất khi broker tắt |
| C-09 | `device-simulator`: lệnh `revoke`, `rotate` | Cắt | S | | |
| C-10 | Mô hình đe dọa QR, `privacy-notes.md` | B | M | C2, 20/11 | Mỗi mối đe dọa trỏ tới một test |
| C-11 | Database cho identity, outbox bền | N | M | C1, 18 đến 19/11 (database `identity` đã có trong `docker/init/central`) | Dòng outbox còn sau khi khởi động lại và được phát |
| C-12 | Xoay khóa ký JWT | N | S | C1, 20/11 | Token ký bằng khóa cũ còn hạn vẫn hợp lệ sau khi đổi khóa |

### Team D: Kiosk, Portal và Central

| ID | Story | Mức | Cỡ | Trạng thái | Tiêu chí hoàn thành |
| --- | --- | --- | --- | --- | --- |
| D-01 | Kiểm chứng hạ tầng Docker | B | M | **Xong** (`docs/runbook/troubleshooting.md`, "Kết quả D-01") | |
| D-02 | CI và bảo vệ nhánh | B | M | 5 job đã có; còn CODEOWNERS, bảo vệ nhánh, 5 check bắt buộc, D1 19/11 | PR đỏ không hợp nhất được; đẩy thẳng lên `main` bị từ chối |
| D-03 | Consumer thật, DLQ, retry, dedup JDBC | B | L | Phần lớn xong (3 IT listener); còn cảnh báo DLQ (N), D1 19/11 | Thông điệp lỗi vào DLQ và có cảnh báo |
| D-04 | Reporting: read model JDBC chung transaction với dedup, áp hiệu chỉnh | B | M | Áp hiệu chỉnh đã có; D2 17 đến 18/11. Báo cáo theo nhóm, ca, xuất file: cắt | Hiện diện còn sau khởi động lại; lỗi thì không ghi gì |
| D-05 | Quy tắc gian lận R4 | B | M | D2, 16/11 | BD rồi LA cách 10 phút sinh cảnh báo |
| D-06 | `HttpHrPayrollAdapter`, tra cứu trước khi gửi lại, xử lý `AttendanceCorrected` | B | M | D1, 16 đến 17/11. Scheduler backoff: cắt | HR không trả lời thì tra cứu trước, không gửi trùng |
| D-07 | Cảnh báo hạn chót 08:00 và 12:00 ngày 1 | B | M | D1, 18/11. Đối soát kỳ, lô chốt: cắt | Test với đồng hồ giả |
| D-08 | Kho audit PostgreSQL chỉ INSERT, chuỗi băm | B | M | D2, 18 đến 19/11. Truy vấn 5 giây, lưu 12 tháng: cắt | Sửa dữ liệu bị phát hiện; role không có UPDATE, DELETE |
| D-09 | Kịch bản sự cố P1 đến P5 tự động | Cắt | M | | |
| D-10 | Portal đăng nhập qua màn hình riêng, Guard Console cảnh báo trực tiếp | Cắt | L | Phần gửi Bearer chuyển sang D-13 | |
| D-11 | Dockerfile từng service, profile `apps` | Cắt | L | | |
| D-12 | Hợp nhất báo cáo và nộp | B | L | D1, 23 đến 25/11; nộp 26/11 | PDF theo `release-checklist.md` |
| D-13 | Portal và app nhân viên gửi Bearer; `PrincipalResolver` ở reporting, hr-integration, audit-admin | N | M | D2, 20 đến 23/11 | Bearer được gửi, 401 về đăng nhập; service trung tâm nhận Bearer |
| D-14 | Test `kiosk-simulator` trên Windows (đóng `Journal` sau mỗi test) | N | S | D2, 23/11 | Xanh trên Windows |
| D-15 | Kiosk dùng `employees` của gói đồng bộ khi offline | N | M | D1, 20/11 | Offline: nhân viên `INACTIVE` hoặc sai nhà máy bị từ chối |
| D-16 | Thử trang `/kiosk` bằng webcam và điện thoại thật | T | S | D2, 24/11 trong tổng duyệt | Kết quả ghi vào runbook |

**Về mã D-13:** trước đây D-13 được dùng cho hai việc ("app nhân viên dùng JWT" trong kế hoạch, "quét bằng webcam" trong bảng hiện trạng). Từ 16/11, **D-13 chỉ là việc gửi Bearer**; thử webcam là **D-16**.

Báo cáo của từng team (không phải story mã): **R-A**, **R-B**, **R-C** (nháp 20/11, bản cuối 24/11); phần của Team D nằm trong D-12.

## 5. Hợp đồng giữa các team

| Hợp đồng | Bên cung cấp | Bên dùng | Nơi định nghĩa | Ai duyệt khi đổi |
| --- | --- | --- | --- | --- |
| `IAttendanceCommand`, `IAttendanceQuery`, `IOfflineAttendanceIngest` (REST `/internal/v1/**`, kể cả last-direction) | B | A | `docs/api/internal-attendance.openapi.yaml` | A và B |
| Sự kiện trên exchange `tmc.events` (`attendance.*`, `*.AuditRecorded`, `AttendanceCorrected`) | B, C, A | D | `docs/api/attendance-events.asyncapi.yaml`, ADR-15 | Bên phát và D |
| `IQRValidation` (`/internal/v1/qr/verify`), sync bundle (`/internal/v1/sync-bundle`, có `employees`) | C | A, D | `docs/api/user-qr.openapi.yaml` | A và C |
| Kiosk API (`/kiosk/v1/**`) | A | D (kiosk-simulator, trang `/kiosk`) | `docs/api/kiosk-edge.openapi.yaml` | A và D |
| `ITurnstileControlPort` và API turnstile | A | A | `docs/api/turnstile.openapi.yaml` | A |
| Quan sát cổng (`/internal/v1/attempts/{id}/gate-observation`) | A | B | `docs/api/kiosk-edge.openapi.yaml` (mục internal) | A và B |
| JWT, JWKS, `PrincipalResolver`; header dev chỉ ở `local`, `inmem` | C | tất cả | `docs/api/identity.openapi.yaml`, `docs/conventions/api-design-guidelines.md` | C và bên dùng |
| Fixture dùng chung | C (seed, chính sách token) | A, C, D | `docs/api/fixtures/employee-seed.json`, `qr-token-policy.json` | Bốn lead |

**Quy tắc:** hợp đồng đổi thì sửa tệp `docs/api` **trong cùng PR** với mã, và thêm reviewer của bên dùng. Đổi không tương thích phải có ADR.

## 6. Phụ thuộc

Không còn phụ thuộc chặn giữa các team (bước gỡ phụ thuộc đã làm xong mọi phần mà nhiều team cùng cần). Chỗ cần mã thật của nhau là 7 lần kiểm tích hợp tùy chọn I-1 đến I-7 ở [`01-ke-hoach.md`](../handover/01-ke-hoach.md) mục 4. Đồ thị đường găng cũ (D-01 → B-01 → B-02 → D-03) đã bỏ vì các story đó đã xong.

## 7. Kế hoạch theo ngày

Lịch W1 đến W4 cũ đã bỏ. Lịch hiện hành: [`docs/handover/01-ke-hoach.md`](../handover/01-ke-hoach.md) mục 3 và [`docs/handover/03-gantt.md`](../handover/03-gantt.md). Việc mức `T` rồi `N` bị cắt trước khi trễ, theo thứ tự ở `01-ke-hoach.md` mục 6.3; quyết định ghi vào `MEETING-CADENCE.md`, mục "Nhật ký quyết định".

## 8. Phân công viết báo cáo

Báo cáo nộp là **tài liệu M1 đã sửa + phần M2**. Mỗi team viết phần của mình bằng Markdown trong `docs/report/`, Team D hợp nhất (D-12).

| Phần | Nội dung | Team viết | Hạn |
| --- | --- | --- | --- |
| Issues Encountered: mất kết nối | Vấn đề, 7 điểm đứt, cơ chế (ADR-11), giảm thiểu, vị trí trong từng view, đánh đổi, bằng chứng | B (viết), A và D (bằng chứng) | Nháp 20/11, cuối 24/11 |
| Issues Encountered: QR tĩnh | Mối đe dọa, QR động ký bằng khóa thiết bị (ADR-12), chống replay nhiều lớp, đánh đổi | C (viết), A và D (bằng chứng) | Nháp 20/11, cuối 24/11 |
| Cập nhật yêu cầu, use case, đặc tính | NFR-05, NFR-09, UC-07, UC-08, AC-03, AC-04 theo `docs/adr/README.md` mục 3 | C (NFR-05, UC-07), B (NFR-09, UC-08) | Nháp 20/11, cuối 24/11 |
| Component, sequence, deployment | Sơ đồ mới hoặc đã sửa | A (quét QR, 15.2), B (outbox), C (bảo mật), D (HR) | Cuối 24/11 |
| Áp dụng SOLID | Mỗi nguyên tắc gắn với lớp thật (`docs/architecture/solid-map.md`) | Cả bốn | Cuối 24/11 |
| Phản ánh | Mỗi team một đoạn | Cả bốn, D hợp nhất | Cuối 24/11 |
| Task 2: hiện thực | Module đã chọn, cách chạy, ảnh kết quả test và demo, URL GitHub | D | 25/11 |
| Hợp nhất, ma trận truy vết, định dạng | Một giọng văn, đánh số hình, tài liệu tham khảo | D | 25/11 |

Quy tắc viết: **mọi khẳng định về hành vi phải trỏ tới một test hoặc một kịch bản demo** (tên lớp test hoặc `make demo-*`). Không viết điều mà mã chưa làm; việc chưa làm ghi vào mục "Hạn chế và việc tiếp theo".

## 9. Định nghĩa "xong" cho một story

1. Mã theo `docs/conventions/coding-standards.md`, `./gradlew spotlessCheck` sạch.
2. Có test theo `docs/conventions/testing-strategy.md`; test mới viết **trước hoặc cùng lúc** với mã.
3. `ArchitectureTest` của service vẫn xanh; story có test tích hợp thì `integrationTest` của module xanh.
4. Hợp đồng đổi thì `docs/api` đổi trong cùng PR.
5. Quyết định kiến trúc mới thì có ADR (`docs/adr/ADR-template.md`).
6. PR được duyệt đủ người theo checklist, CI xanh.
7. Tiêu chí hoàn thành được chứng minh trong mô tả PR (lệnh đã chạy hoặc ảnh chụp). Không cần kiểm tích hợp với team khác để tính là xong.
