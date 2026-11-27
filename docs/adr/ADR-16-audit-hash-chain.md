# ADR-16: Nhật ký audit chỉ thêm, có chuỗi băm, ghi bằng login chỉ INSERT

| Trạng thái | Accepted |
| --- | --- |
| Ngày | 27/11/2026 |
| Người đề xuất | Team D |
| Người duyệt | Lead Team C (identity và audit nguồn), lead Team D |
| Vấn đề giải quyết | NFR-02, AC-05: audit không được sửa hay xóa, và phải phát hiện được nếu bị sửa (story D-08) |
| Thay thế / mở rộng | Mở rộng ADR-08 (Identity tập trung, RBAC, audit) và ADR-10 (database riêng mỗi service) |

## Bối cảnh (Context)

`audit-admin` nhận `*.AuditRecorded` từ `tmc.events` và trước D-08 giữ nhật ký trong bộ nhớ: khởi động lại là mất, và không có gì chứng minh dữ liệu chưa bị sửa. NFR-02 đòi nhật ký chỉ thêm; AC-05 đòi phát hiện được sửa đổi.

Hai sự thật làm khó việc này:

1. Mỗi service đăng nhập database bằng chính role sở hữu bảng (`docker/init/central/01-databases.sql`). Chủ sở hữu luôn UPDATE được, nên "chỉ INSERT" chỉ là lời hứa của mã.
2. Có hai luồng ghi đồng thời: listener RabbitMQ và REST (`AUDIT_QUERIED`, `AUDIT_CHAIN_VERIFIED`). Chuỗi băm ghi song song sẽ rẽ nhánh nếu không tuần tự hóa.

## Quyết định (Decision)

Chúng ta sẽ:

1. **Tách hai login.** Flyway chạy bằng chủ sở hữu `auditadmin`. Service chạy bằng `auditadmin_app`, được `GRANT SELECT, INSERT` trên `audit_entries` (và `processed_messages`), không gì khác (`V2__audit_entries.sql`).
2. **Chặn ở database.** Trigger `BEFORE UPDATE OR DELETE` và `BEFORE TRUNCATE` báo lỗi cho mọi role, kể cả chủ sở hữu.
3. **Chuỗi băm.** Mỗi dòng lưu `prev_hash` và `hash = SHA-256(prev_hash || canonical(entry))` (`AuditHashChain`). Dạng chuẩn ghi từng trường dưới dạng `<độ dài>:<giá trị>`, khóa `details` sắp xếp, thời gian cắt về micro giây. Dòng đầu có `prev_hash` là 64 số 0.
4. **Tuần tự hóa việc ghi** bằng `pg_advisory_xact_lock` (không cần quyền trên bảng). `UNIQUE(prev_hash)` là lớp chặn cuối nếu có ai ghi không qua khóa. Khóa tham gia transaction của consumer, nên dòng audit commit cùng marker chống trùng.
5. **Kiểm tra được.** `GET /admin/v1/audit/verification` (SYSTEM_ADMIN) tính lại toàn chuỗi và trả `seq` của dòng hỏng đầu tiên. Bản thân việc kiểm tra cũng được ghi audit.

## Phương án đã cân nhắc

| Phương án | Vì sao không chọn |
| --- | --- |
| Chỉ dựa vào port không có hàm sửa/xóa | Không chặn được ai có quyền vào database |
| `SELECT ... FOR UPDATE` hoặc `LOCK TABLE` để tuần tự hóa | Cần quyền UPDATE/DELETE, trái với login chỉ INSERT |
| Ký từng dòng bằng khóa riêng | Cần quản lý khóa; chuỗi băm đủ cho yêu cầu "phát hiện" của AC-05 |
| Lưu audit vào hệ thống WORM bên ngoài | Ngoài phạm vi chạy local của dự án |
| **Hai login + trigger + chuỗi băm + advisory lock** | **Chọn vì chặn được ở database, phát hiện được khi bị qua mặt, và không cần hạ tầng thêm** |

## Hệ quả (Consequences)

(+) Login của service không sửa, xóa hay TRUNCATE được lịch sử; IT chứng minh bằng PostgreSQL thật.
(+) Superuser tắt trigger để sửa một dòng vẫn bị `verification` chỉ ra đúng dòng đó.
(–) Mọi lần ghi audit đi qua một khóa: thông lượng ghi audit là tuần tự. Với lưu lượng audit của hai nhà máy thì chấp nhận được; chưa đo.
(–) Chuỗi băm phát hiện, không ngăn: ai có quyền superuser vẫn có thể viết lại **toàn bộ** chuỗi từ dòng bị sửa. Muốn chống việc đó cần neo hash định kỳ ra ngoài (ví dụ gửi hash cuối ngày cho HR), chưa làm.
(–) Volume Docker tạo trước 27/11 không có login `auditadmin_app`: phải `make nuke && make up` (xem runbook).
(–) Kiểm tra đọc lại toàn bộ bảng; khi bảng lớn cần kiểm tra theo đoạn. Yêu cầu tra cứu 5 giây và lưu 12 tháng (NFR-02) đã cắt khỏi M2.

## Kiểm chứng tuân thủ (Compliance)

- `AuditTest.should_detect_a_modified_entry_through_the_hash_chain`, `should_have_no_way_to_change_or_remove_history_through_the_store_port`, `should_let_only_the_system_admin_verify_the_chain_and_audit_the_check`.
- `AuditStoreIT` (PostgreSQL thật): login ứng dụng bị từ chối UPDATE, DELETE, TRUNCATE; chủ sở hữu bị trigger chặn; 20 lần ghi song song kèm ghi trùng giữ một chuỗi hợp lệ; dòng bị superuser sửa được chỉ ra đúng `seq`.
- `AuditEventListenerIT`: luồng RabbitMQ đến bảng vẫn chạy.

## Vị trí trong kiến trúc

| View | Thay đổi |
| --- | --- |
| Module view | `audit-admin`: thêm `domain/AuditHashChain`, `adapter/out/persistence/JdbcAuditStore`; `IAuditStore.verifyChain()` |
| Component and connector view | Endpoint mới `GET /admin/v1/audit/verification` |
| Deployment view | Database `audit_admin` có thêm login `auditadmin_app`; Flyway dùng login chủ sở hữu |
| API | `central.openapi.yaml`: thêm `/admin/v1/audit/verification` |
