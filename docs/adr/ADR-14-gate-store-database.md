# ADR-14: Gate Station có cơ sở dữ liệu riêng `gate_store` trên PostgreSQL của Edge

| Trạng thái | Accepted (16/11/2026) |
| --- | --- |
| Ngày | 16/11/2026 |
| Người đề xuất | Team D (tích hợp), theo yêu cầu gỡ phụ thuộc giữa các team |
| Người duyệt | Lead Team A, lead Team D |
| Vấn đề giải quyết | A-04 (danh mục cổng FR-11 đến FR-16) và C-08 (audit tại cổng) cần chỗ lưu bền vững cho Gate Station; trước đây cổng và nhân viên chỉ là seed trong `application.yml` |
| Thay thế / mở rộng | Cụ thể hóa ADR-10 (database per service) cho Gate Station; liên quan ADR-13, ADR-15 |

## Bối cảnh (Context)

Gate Station là bên điều phối tại biên (ADR-13). Danh mục cổng (chế độ ENTRY_ONLY/EXIT_ONLY/BIDIRECTIONAL, bảo trì) đang là
seed cấu hình, không sửa được lúc chạy. Audit "mở thủ công và lý do" (NFR-02) đang chỉ ghi log. ADR-10 cấm Gate Station dùng
bảng của Attendance. Trên máy chủ PostgreSQL của Edge mới chỉ có `attendance_edge`.

Nếu không quyết định, Team A (A-04) và Team C (C-08, phần audit tại cổng) sẽ chờ nhau về chỗ đặt bảng, và mỗi team có thể tự
tạo một nơi lưu khác nhau.

## Quyết định (Decision)

1. Tạo cơ sở dữ liệu `gate_store`, login `gatestation`, trên **cùng máy chủ PostgreSQL của Edge** (`docker/init/edge/02-gate-store.sql`).
   Chỉ Gate Station truy cập.
2. Gate Station chỉ bật kết nối khi chạy profile **`gate-store`** (`GateStoreConfig`, `application-gate-store.yml`,
   biến `GATE_STORE_DB_URL`, `GATE_STORE_DB_USER`, `GATE_STORE_DB_PASSWORD`). Các profile `inmem` và `local` không cần cơ sở dữ liệu;
   auto-configuration DataSource bị tắt ở mọi profile để tránh khởi động nhầm.
3. Migration Flyway riêng ở `classpath:db/gate-store` (`V1__gate_store.sql`): bảng `gates` (ràng buộc chế độ hợp lệ, một
   `hardware_gate_id` một trạm) và bảng `outbox_events` cùng khuôn với `platform/messaging` (ADR-15).
4. Trạng thái một-lần-dùng và anti-passback **vẫn ở Redis** (ADR-12, ADR-13); `gate_store` không thay Redis.

## Phương án đã cân nhắc

| Phương án | Vì sao không chọn |
| --- | --- |
| Giữ seed cấu hình | Không sửa được cổng lúc chạy (FR-11 đến FR-16), không có chỗ cho outbox audit |
| Dùng chung `attendance_edge` | Vi phạm ADR-10, hai service khóa nhau khi đổi schema |
| Máy chủ PostgreSQL thứ hai cho Gate Station | Thêm vận hành ở mỗi nhà máy mà không thêm cô lập đáng kể so với một database riêng |
| **Database riêng `gate_store` trên PostgreSQL của Edge, bật bằng profile** | **Chọn:** cô lập theo ADR-10, không thêm máy chủ, không phá chế độ chạy không Docker |

## Hệ quả (Consequences)

(+) A-04 chỉ cần viết `JdbcGateConfigRepository` trên bảng `gates`; C-08 phần cổng chỉ cần `JdbcOutboxStore` trên `gate_store`.
(+) Demo không Docker (`local`) không đổi.
(–) Thêm một database cần sao lưu và theo dõi ở mỗi Edge.
(–) Container PostgreSQL đã tạo **trước** khi có `02-gate-store.sql` sẽ không tự có database này: phải `make nuke && make up`
hoặc tạo tay (`docs/runbook/troubleshooting.md`).
(–) Chưa có adapter JDBC cho danh mục cổng: cho tới A-04, cổng vẫn đọc từ seed kể cả khi bật `gate-store`.

## Kiểm chứng tuân thủ (Compliance)

- `GateStoreIT` (Testcontainers): service khởi động với profile `inmem,gate-store`, Flyway áp V1, bảng `gates` từ chối chế độ lạ
  và `hardware_gate_id` trùng, `outbox_events` nhận sự kiện audit.
- `ArchitectureTest` của gate-station: không import service khác.

## Vị trí trong kiến trúc

| View | Thay đổi |
| --- | --- |
| Module view | `services/gate-station/bootstrap/GateStoreConfig`, `resources/db/gate-store` |
| Component and connector view | Gate Station có kết nối JDBC tới `gate_store` |
| Deployment view | Edge PostgreSQL chứa `attendance_edge` và `gate_store` |
| API / sự kiện | Không đổi API; audit cổng đi qua outbox (ADR-15) với khóa `gate-station.AuditRecorded` |

## Liên kết

ADR-10, ADR-13, ADR-15; FR-11 đến FR-16, NFR-02; story A-04, C-08.
