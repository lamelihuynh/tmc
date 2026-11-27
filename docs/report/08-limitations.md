# 19 Hạn chế và việc tiếp theo

> Phần này trả lời yêu cầu **trình bày trung thực** của đề bài M2 (Task 2 và mục "Issues Encountered"): nêu rõ những gì đã cắt, đã thu nhỏ, chưa kiểm chứng và còn giới hạn trong mã, kèm việc tiếp theo cho từng mục. Nguồn: [`01-ke-hoach.md`](../handover/01-ke-hoach.md) mục 6, [`implementation-notes.md`](../architecture/implementation-notes.md) mục 3 đến 7, các test `@Disabled` trong mã, và kết quả của Team D (D-03 đến D-16).

---

## 19.1 Phạm vi đã thu nhỏ trong đợt này

| Story | Bản đầy đủ | Đã làm (bằng chứng) | Còn thiếu | Việc tiếp theo |
| --- | --- | --- | --- | --- |
| C-06 | Mọi service xác thực JWT | `platform/security`, user-qr; ba service trung tâm (D-13, `BearerTokenApiTest`); attendance dùng `PrincipalResolver` cho API HR (B-06/B-07) | user-qr và attendance vẫn trả `403` (không phải `401`) khi thiếu hoặc sai token; phần gate-station (A-11) do Team A báo cáo | Thống nhất `401 UNAUTHENTICATED` cho mọi service; kiểm Bearer đầu-cuối trên profile mặc định (I-5) |
| D-03 | Consumer thật, DLQ, retry, theo dõi | Consumer, DLQ, thử lại 3 lần, dedup JDBC; gauge `tmc.rabbitmq.dlq.depth` và log `WARN` ở cả ba service | Chỉ `hr-integration` có IT cho phần theo dõi DLQ; chưa có hệ thống cảnh báo nhận gauge này | Thêm IT tương tự cho reporting, audit-admin; nối gauge vào hệ thống giám sát |
| D-04 | Báo cáo theo nhóm, ca, xuất file, dashboard | Read model JDBC `attendance_projection`, áp hiệu chỉnh, bền qua khởi động lại (`PresencePersistenceIT`) | FR-24, FR-25, FR-28 (báo cáo nhóm/ca, dashboard ban giám đốc, xuất file); test `@Disabled` ở mục 19.3 | Thêm truy vấn tổng hợp trên `attendance_projection` và trang tương ứng trong portal |
| D-06 | Adapter HTTP, tra cứu, scheduler có backoff | Cả ba, kèm `AttendanceCorrected`; job đối soát có backoff (vượt phạm vi đã thu nhỏ) | Không có hệ thống HR thật: `tmc.hr.mode` mặc định `simulated`; `HttpHrPayrollAdapter` mới chạy với máy chủ HTTP giả trong `HttpHrPayrollAdapterTest` | Chạy `tmc.hr.mode=http` với một máy chủ giả lập theo `hr-payroll.openapi.yaml` trên stack Docker |
| D-07 | Đối soát kỳ, lô chốt, cảnh báo | Cảnh báo 08:00 `WARNING`, 12:00 `OVERDUE` ngày 1 (`PayrollDeadlineCheck`) | Cảnh báo chỉ ghi log; chưa đối soát kỳ với Attendance qua `IAttendanceQuery`, chưa gửi lô chốt `POST /api/v1/attendance-reconciliations` | Hiện thực đối soát kỳ và bật test `@Disabled` tương ứng; gửi cảnh báo tới người trực |
| D-08 | PostgreSQL chỉ INSERT, chuỗi băm, truy vấn 5 giây, lưu 12 tháng | PostgreSQL chỉ INSERT, trigger, chuỗi băm, `GET /admin/v1/audit/verification` (`AuditStoreIT`) | Truy vấn ≤ 5 giây và lưu 12 tháng (NFR-02) chưa làm, test `@Disabled` | Chỉ mục theo thời gian và hành động, đo thời gian truy vấn; chính sách phân vùng và lưu trữ |
| C-02 | Hồ sơ và vòng đời nhân viên từ HR | Bảng nhân viên trong user-qr, một endpoint HR đổi trạng thái và nhà máy | Vòng đời đầy đủ (C-03 bị cắt) | Xem 19.2 |

## 19.2 Đã cắt khỏi đợt này

| Mục | Nội dung | Ảnh hưởng | Việc tiếp theo |
| --- | --- | --- | --- |
| A-07 | Cảnh báo trực tiếp cho bảo vệ | Vi phạm anti-passback và cảnh báo gian lận R4 không được đẩy tới Guard Console (FR-18 phần "gửi cảnh báo ngay") | Kênh đẩy (SSE/WebSocket) từ Edge Gateway, nhận cả `FraudAlert` |
| A-08 | Callback từ cổng | Lượt `GATE_UNKNOWN` chỉ được chốt bằng hỏi trạng thái cổng | Nhận callback nếu API cổng hỗ trợ |
| B-09 | Tham số quản trị | Tham số vận hành (ví dụ cửa sổ di chuyển của R4, chu kỳ đối soát HR) chỉ đổi bằng cấu hình khi khởi động (FR-32) | API quản trị tham số có audit |
| C-03 | Vòng đời nhân viên | Chưa nhập hồ sơ từ HR (UC-01, UC-03 đầy đủ) | Consumer sự kiện nhân viên từ HR |
| C-07 | Quản lý tài khoản | Chưa tạo, đổi vai trò, vô hiệu tài khoản (FR-31) | Xem test `@Disabled` của identity ở 19.3 |
| C-09 | Lệnh thu hồi trong device-simulator | Thu hồi thiết bị chỉ thử qua API | Thêm lệnh `revoke` |
| D-09 | Kịch bản sự cố tự động | Không có kịch bản tự động dừng RabbitMQ, PostgreSQL trên stack đầy đủ (có `OutboxBrokerRestartIT` của Team B ở mức test) | Script fault-injection trên `make up` |
| D-10 | Portal đăng nhập thật qua màn hình riêng | Đã giảm một phần nhờ D-13: portal có trang đăng nhập với identity. Còn thiếu tài khoản bảo vệ có phạm vi cổng (xem 19.5) | Xem 19.5, Identity |
| D-11 | Dockerfile từng service | Không đóng gói container cho service; mTLS, TLS 1.2+ chưa cấu hình (NFR-04) | Dockerfile, chứng chỉ nội bộ |
| Hết hạn phiên theo hoạt động | Phiên hết hạn sau 15 phút không hoạt động (NFR-03) | JWT sống cố định 15 phút, không theo hoạt động | Xem test `@Disabled` ở 19.3 |

## 19.3 Test `@Disabled` còn lại

Tìm bằng `@Disabled` trên `services/`, `simulators/`, `platform/`: còn **5** test, đều là đặc tả của phần bị cắt hoặc thu nhỏ ở trên. `simulators/` và `platform/` không còn test bị tắt.

| Tệp | Test | Lý do ghi trong mã | Việc tiếp theo |
| --- | --- | --- | --- |
| `services/reporting/src/test/java/vn/tmc/reporting/ProjectionTest.java` | `should_build_a_group_attendance_report_within_the_role_scope` | FR-24, FR-28: báo cáo theo nhóm/ca/nhà máy và xuất file giới hạn theo vai trò | D-04 bản đầy đủ |
| `services/hr-integration/src/test/java/vn/tmc/hrintegration/SyncAttendanceEventTest.java` | `should_send_a_correction_batch_for_missing_or_failed_records` | NFR-08: đối soát kỳ với Attendance qua `IAttendanceQuery` và dựng lô | D-07 bản đầy đủ |
| `services/audit-admin/src/test/java/vn/tmc/auditadmin/AuditTest.java` | `should_meet_the_query_time_and_retention_requirements` | NFR-02: tra cứu trong 5 giây, lưu ít nhất 12 tháng | D-08 bản đầy đủ |
| `services/identity/src/test/java/vn/tmc/identity/AuthenticationTest.java` | `should_expire_an_idle_session` | NFR-03: phiên hết hạn sau 15 phút không hoạt động | Hết hạn phiên theo hoạt động |
| `services/identity/src/test/java/vn/tmc/identity/AuthenticationTest.java` | `should_end_open_sessions_when_a_role_changes` | FR-31: tạo, đổi vai trò, vô hiệu tài khoản; đổi vai trò kết thúc phiên đang mở | C-07 |

## 19.4 Chưa được kiểm chứng

| Hạng mục | Tình trạng | Việc tiếp theo |
| --- | --- | --- |
| 3 kịch bản demo trên stack Docker đầy đủ, service trung tâm ở profile mặc định (I-6) | **Chưa ai chạy.** Ba demo chỉ đã chạy trên stack `local` (dữ liệu trong bộ nhớ) | Chạy `make up`, khởi động 8 service ở profile mặc định, chạy 3 demo, lưu đầu ra |
| Audit từ gate-station, user-qr, identity vào kho audit bền (I-4); Bearer đầu-cuối trên profile mặc định (I-5) | Không có kết quả được ghi trong repo | Chạy và ghi kết quả vào `docs/runbook/troubleshooting.md` |
| Trang `/kiosk` với webcam và điện thoại thật (D-16) | Chưa thử | Theo [`02-chay-local.md`](../handover/02-chay-local.md) mục 5.4, 5.5 |
| `CODEOWNERS` và bảo vệ nhánh (D-02) | `CODEOWNERS` vẫn dùng handle giữ chỗ `@REPLACE-ME-team-*`, nên chưa bật "Require review from Code Owners" | Lead các team gửi tài khoản GitHub, làm theo [`release-checklist.md`](../runbook/release-checklist.md) |
| Hiệu năng NFR-06 ở tải đỉnh | A-09 chỉ đo trên stack local, Windows, phát tải tuần tự 0,45 lượt/giây ([`A-09-load-test.md`](A-09-load-test.md)) | Đo tải đồng thời trên stack Docker |
| Thông lượng ghi audit qua một advisory lock (ADR-16) | Chưa đo | Đo khi có tải audit thật |

## 19.5 Giới hạn đã biết trong mã, theo khu vực

**Reporting (D-04, D-05)**

- Chỉ có luật R4 di chuyển bất khả thi. **Chưa có** luật lặp từ chối (nhiều token hết hạn hoặc sai từ một người hay một kiosk): reporting chưa nhận sự kiện từ chối (`FraudRule` ghi rõ điều này).
- Cảnh báo gian lận được lưu và xem qua `GET /supervision/v1/alerts`, nhưng **không** phát `AuditRecorded`, nên chưa có trong nhật ký kiểm toán.

**Audit (D-08, ADR-16)**

- Chuỗi băm **phát hiện**, không ngăn: người có quyền superuser vẫn viết lại được toàn bộ chuỗi từ dòng bị sửa. Chưa neo hash định kỳ ra ngoài.
- Kiểm tra đọc lại toàn bảng; chưa kiểm tra theo đoạn khi bảng lớn.
- Volume Docker tạo trước 27/11 thiếu login `auditadmin_app`: cần `make nuke && make up`.

**HR/Payroll (D-06, D-07)**

- Không có hệ thống HR thật trong dự án chạy local: mặc định `tmc.hr.mode=simulated`, service ghi cảnh báo khi khởi động. Không được nói "đã gửi công sang HR thật".
- Cảnh báo hạn chót chỉ ghi log; chưa có người nhận.

**Identity và ứng dụng web (D-13)**

- Identity **không có tài khoản `SECURITY_GUARD`** và tài khoản không mang phạm vi cổng, nên trang bảo vệ của portal chỉ dùng được qua thanh "DEV" ở profile `local`.
- Bí mật TOTP của `admin1`, `hr1` chỉ có ở profile `local`, `inmem`.
- Không có refresh token: token sống 15 phút, hết hạn thì đăng nhập lại.

**Kiosk và biên (ADR-11, ADR-12, D-15)**

- Khi offline, kiosk chỉ biết trạng thái nhân viên, nhà máy và thu hồi thiết bị **tính đến lần đồng bộ gần nhất**. Thay đổi sau đó chỉ được phát hiện ở lần đồng bộ kế tiếp hoặc bị gắn bất thường khi giao journal (M1 mục 10.4 đã thừa nhận không thu hồi tức thời trên thiết bị mất mạng).
- Journal SQLite của `kiosk-simulator` không mã hóa; M1 mục 15.2 thiết kế mã hóa AES-256 khi triển khai thật.
- Trang kiosk web `/kiosk` không thay được `kiosk-simulator` (không có journal và chế độ offline như bản dòng lệnh); demo ADR-11 dùng bản dòng lệnh.

**Hạ tầng và triển khai**

- Ở profile `local`, `inmem`, read model, `sync_log`, kho audit và outbox của identity nằm trong bộ nhớ; chỉ profile mặc định dùng PostgreSQL.
- mTLS, TLS 1.2+ chưa cấu hình; mã hóa dữ liệu cá nhân khi lưu (AES-256, NFR-04) chưa làm.
- Edge Node B dự phòng và replication đồng bộ (M1 mục 15.2) là thiết kế triển khai, không có trong `docker/docker-compose.yml`.

## 19.6 Điều báo cáo không khẳng định

Theo [`implementation-notes.md`](../architecture/implementation-notes.md) mục 7, cập nhật theo kết quả hiện có:

| Không nói | Có thể nói (kèm bằng chứng) |
| --- | --- |
| "Hệ thống đạt p95 ≤ 1 giây ở tải đỉnh" | p95 36,82 ms cho 800 lượt tuần tự trên stack local, Windows (A-09), đúng điều kiện đã ghi |
| "Không mất bản ghi khi broker khởi động lại" cho toàn hệ thống | Outbox của attendance giữ `PENDING` khi RabbitMQ dừng và phát đúng thứ tự khi bật lại (`OutboxBrokerRestartIT`, Team B); consumer khử trùng trong cùng transaction (`MessagingInfraIT`, `PresencePersistenceIT`) |
| "Audit đăng nhập bền vững" ở mọi cấu hình | Ở profile mặc định identity ghi outbox trên PostgreSQL (`IdentityOutboxIT`); ở profile `local` outbox nằm trong bộ nhớ |
| "Có MFA" cho tài khoản quản trị ở profile mặc định | TOTP RFC 6238 đã hiện thực (`TotpMfaVerifierTest`, `TotpLoginApiTest`); bí mật mẫu chỉ có ở `local`, `inmem` |
| "Chạy trên Docker" cho 3 demo | 3 demo chạy trên stack `local`; adapter PostgreSQL, RabbitMQ, Redis được kiểm bằng test tích hợp Testcontainers |
| "Đã đồng bộ công sang HR/Payroll" | Đồng bộ với HR giả lập; adapter HTTP kiểm với máy chủ HTTP giả trong test |
