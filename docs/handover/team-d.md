# Team D: Kiosk, Portal và Central

Đọc [00-he-thong.md](00-he-thong.md) và làm xong [02-chay-local.md](02-chay-local.md) trước khi đọc tài liệu này.

## 1. Nhiệm vụ của team

Team D giữ **hai đầu của hệ thống** và phần gắn kết cả nhóm: thiết bị tại cổng (kiosk, hai ứng dụng web), phần trung tâm nghe sự kiện (báo cáo, HR, kiểm toán), cùng CI, hạ tầng và bản báo cáo cuối.

| Sở hữu | Nội dung |
| --- | --- |
| `simulators/kiosk-simulator` | Kiosk dòng lệnh: journal SQLite, xác thực offline, gửi bù |
| `apps/employee-app` (cổng 5174) | App nhân viên và trang kiosk web (Team C phối hợp phần token) |
| `apps/web-portal` (cổng 5173) | Cổng thông tin cho bảo vệ, quản đốc, HR |
| `services/reporting` (8084) | Read model hiện diện, quy tắc gian lận |
| `services/hr-integration` (8085) | Đồng bộ công sang HR/Payroll |
| `services/audit-admin` (8086) | Nhật ký kiểm toán chỉ thêm |
| `docker`, `scripts`, `.github` | Hạ tầng, script, CI |
| Tài liệu | Phần HR và triển khai, Task 2, hợp nhất báo cáo cuối |

Team D có nhiều module nhất nhưng mỗi module nhỏ. Hai người nên chia theo module ngay từ đầu: D1 nhận hr-integration, hạ tầng và báo cáo; D2 nhận CI, reporting, audit-admin và hai ứng dụng web.

## 2. Bản đồ mã

Ba service trung tâm cùng một khuôn: một listener nhận sự kiện, bọc trong `IdempotentConsumer` để không xử lý trùng, gọi một use case, và một controller chỉ đọc.

| Service | Use case nhận sự kiện | Use case truy vấn | Port ra | Test |
| --- | --- | --- | --- | --- |
| `reporting` | `ProjectAttendanceEventUseCase` | `ReportingQueryService` | `IPresenceRepository` | `ProjectionTest` (3 test `@Disabled`) |
| `hr-integration` | `SyncAttendanceEventUseCase` | `SyncMonitoringService` | `IHrPayrollPort`, `ISyncLogRepository` | `SyncAttendanceEventTest` (3 test `@Disabled`) |
| `audit-admin` | `RecordAuditUseCase` | `AuditQueryService` | `IAuditStore` (không có hàm sửa hay xóa) | `AuditTest` (2 test `@Disabled`) |

Các test `@Disabled` chính là đặc tả của việc phải làm: mỗi test ghi rõ `TODO Team D` và yêu cầu tương ứng.

Các phần còn lại:

| Module | Tệp chính | Việc |
| --- | --- | --- |
| `kiosk-simulator` | `KioskApp`, `Journal`, `OfflineVerifier`, `HttpEdgeClient`, `KioskSimulatorCli` | Ghi journal trước khi gọi mạng; gửi lại cùng `attempt_id`; tự xác thực khi mất mạng |
| `apps/employee-app` | `src/lib/token.ts`, `deviceKey.ts`, `api.ts`; `src/pages/Login.tsx`, `Home.tsx`, `Kiosk.tsx` | Ký token bằng WebCrypto; khóa riêng không xuất được, lưu trong IndexedDB; trang `/kiosk` quét bằng `jsqr` |
| `apps/web-portal` | `src/App.tsx`, `src/pages/*`, `src/lib/api.ts`, `identity.ts` | Ba trang; thanh danh tính dùng cho phát triển |
| `docker` | `docker-compose.yml` | PostgreSQL edge (5432), PostgreSQL central (5433), Redis (6379), RabbitMQ (5672, 15672) |
| `scripts` | `dev/local-stack.sh`, `dev/local-stack.ps1`, `demo/*.sh` | Dựng stack local; 3 kịch bản demo |

## 3. Chạy và kiểm thử phần của team

```bash
./gradlew :services:reporting:test :services:hr-integration:test :services:audit-admin:test
./gradlew :simulators:kiosk-simulator:test          # hiện đỏ trên Windows, xem D-14
(cd apps/employee-app && npm test && npm run lint && npm run build)
(cd apps/web-portal && npm test && npm run lint && npm run build)
```

Chạy một service trung tâm đơn lẻ (không cần broker):

```bash
SPRING_PROFILES_ACTIVE=inmem ./gradlew :services:reporting:bootRun
curl -s 'http://localhost:8084/supervision/v1/presence?factory=BD' \
  -H 'X-Subject: 0192a1c0-0000-7000-8000-00000000a001' -H 'X-Roles: PRODUCTION_SUPERVISOR' -H 'X-Scope-Factories: BD'
```

Ở profile `inmem`, sự kiện từ attendance **không tới** được service trung tâm. Muốn thấy dữ liệu chạy xuyên suốt thì cần RabbitMQ và profile mặc định (sau B-02 và D-03).

Chạy hai ứng dụng web: `npm run dev` trong từng thư mục. Hướng dẫn dùng app nhân viên: runbook mục 5.

## 4. Việc phải làm

**Mọi hạn là 23:00 của ngày được ghi**, tính theo thời điểm PR hợp nhất vào `main`. Khi một story ghi khoảng ngày (ví dụ "16 đến 17/11"), hạn là 23:00 của ngày cuối. Toàn cảnh lịch: [03-gantt.md](03-gantt.md).

### D-01. Kiểm chứng hạ tầng Docker (bắt buộc, S, 12/11, D1). **Cả nhóm đang chờ.**

- **Việc:** chạy `docker compose -f docker/docker-compose.yml up -d` trên macOS và Windows; kiểm cả bốn container khỏe; thử kết nối bằng thông số trong `application.yml` của attendance và user-qr.
- **Xong khi:** ghi kết quả ("chạy được" hoặc danh sách lỗi và cách sửa) vào `docs/runbook/troubleshooting.md` và báo trong họp đứng ngày 13/11.
- **Thêm trong đợt này:** database hoặc schema riêng cho Gate Store của Team A (A-04) khi Team A yêu cầu.

### D-02. Hoàn thiện CI (bắt buộc, S đến M, 12 đến 16/11, D2). **Cả nhóm đang chờ.**

CI đã có sẵn ở `.github/workflows/ci.yml` và chạy trên **mọi lần đẩy lên mọi nhánh** cùng mọi PR vào `main`. Dự án chỉ chạy local nên không có bước triển khai.

| Job | Kiểm gì | Trạng thái |
| --- | --- | --- |
| `build` | `./gradlew spotlessCheck build`: định dạng, biên dịch, test đơn vị, luật kiến trúc | Bắt buộc |
| `employee-app` | `npm ci`, kiểm định dạng, lint, test, build | Bắt buộc |
| `portal` | Như trên cho `apps/web-portal` | Bắt buộc |
| `integration` | `./gradlew integrationTest` (Testcontainers) | Chưa chặn hợp nhất |
| `demo` | Dựng stack local trên runner rồi chạy 3 kịch bản demo | Chưa chặn hợp nhất, **chưa từng chạy trên GitHub** |

- **Việc còn lại:** xem lần chạy đầu tiên trên GitHub và sửa cho tới khi `build`, `employee-app`, `portal` xanh; sửa job `demo` nếu runner thiếu công cụ (script dùng `lsof` và `curl`); bảo vệ nhánh `main` theo `CONTRIBUTING.md` với ba job bắt buộc là required status checks; thay các `REPLACE-ME-*` trong `CODEOWNERS` bằng tài khoản thật.
- **Ngày 18/11:** khi A-01, B-01, B-02, C-01 đã có test tích hợp xanh, xóa `continue-on-error` của job `integration` và `demo`, đưa chúng vào required status checks.
- **Xong khi:** một PR thử cố ý làm đỏ test thì không hợp nhất được; đẩy thẳng lên `main` bị từ chối.

### D-14. Sửa test kiosk trên Windows (nên có, S, 12/11, D2)

20 test của `KioskAppTest` lỗi trên Windows với `Failed to delete temp directory ... kiosk.db`: kết nối SQLite chưa đóng khi JUnit dọn thư mục tạm.

- **Sửa ở:** `simulators/kiosk-simulator`: bảo đảm `Journal` đóng kết nối (hiện thực `AutoCloseable`, đóng trong `@AfterEach`).
- **Xong khi:** `./gradlew :simulators:kiosk-simulator:test` xanh trên Windows và macOS; bỏ được `-x :simulators:kiosk-simulator:test` khỏi runbook.

### D-05. Quy tắc gian lận R4 (bắt buộc, M, 16 đến 17/11, D2)

Lớp chống dùng lại thứ tư của ADR-12. Hiện chỉ có interface `FraudRule`.

- **Sửa ở:** `services/reporting/.../domain/`: `ImpossibleTravelRule` (vào BD rồi vào LA trong khoảng thời gian không thể di chuyển kịp) và `RepeatedDenialRule` (nhiều lần bị từ chối liên tiếp); gọi từ `ProjectAttendanceEventUseCase`; lưu và truy vấn cảnh báo.
- **Xong khi:** bật được test `should_flag_two_entries_in_different_factories_ten_minutes_apart`.
- **Không phụ thuộc ai:** quy tắc là mã thuần trong `domain`, test bằng sự kiện tự tạo. Làm được từ ngày đầu.

### D-06. Adapter HR/Payroll (bắt buộc, M, 13 đến 18/11, D1). Bản thu nhỏ.

- **Sửa ở:** `services/hr-integration/.../adapter/out/http/HttpHrPayrollAdapter.java` theo `docs/api/hr-payroll.openapi.yaml`; mở rộng `IHrPayrollPort` với thao tác tra cứu kết quả; use case xử lý bản ghi `UNKNOWN`.
- **Xong khi:** bật được test `should_look_up_the_result_before_resending_an_unknown_event`: HR không trả lời thì bản ghi là `UNKNOWN`; lần xử lý sau **tra cứu trước**, chỉ gửi lại khi HR xác nhận chưa nhận; không bao giờ gửi trùng.
- **Cách test:** dựng một HR giả bằng máy chủ HTTP trong test, giống cách `HttpTurnstileAdapterTest` của gate-station làm.
- **Không làm trong đợt này:** scheduler thử lại có backoff.

### D-08. Chuỗi băm cho nhật ký kiểm toán (bắt buộc, S đến M, 18/11, D2). Bản thu nhỏ.

- **Sửa ở:** `services/audit-admin`: mỗi `AuditEntry` mang băm của nội dung nối với băm của bản ghi trước; thêm thao tác kiểm tra toàn chuỗi.
- **Xong khi:** bật được test `should_detect_a_modified_entry_through_the_hash_chain`; test `should_have_no_way_to_change_or_remove_history_through_the_store_port` vẫn xanh (không thêm hàm sửa hay xóa vào `IAuditStore`).
- **Không làm trong đợt này:** kho PostgreSQL chỉ INSERT và lưu 12 tháng.

### D-03. Consumer thật (bắt buộc, M, 19 đến 20/11, cả hai). Bản thu nhỏ.

- **Phụ thuộc:** H8 (B-02) và H9 (B-10), hạn 18/11.
- **Việc:** với cả ba consumer: dùng `JdbcProcessedMessageStore` của Team B thay bản bộ nhớ; thêm hàng đợi thư chết (DLQ) và số lần thử lại có giới hạn; kiểm chứng các listener `@RabbitListener` với RabbitMQ thật.
- **Sửa ở:** `adapter/in/messaging/*` và `bootstrap/*Config` của ba service; test tích hợp dùng Testcontainers RabbitMQ.
- **Xong khi** có test chứng minh: gửi cùng một thông điệp hai lần thì kết quả không nhân đôi; thông điệp làm use case ném lỗi thì sau số lần thử quy định sẽ vào DLQ và không chặn thông điệp sau; consumer khởi động lại không xử lý lại thông điệp cũ.
- **Nếu B-02 trễ:** không cần publisher của Team B. Test tự đẩy `EventEnvelope` lên exchange `tmc.attendance.events` bằng `RabbitTemplate`. Thứ thật sự cần là hợp đồng trong AsyncAPI, đã chốt từ 13/11.
- **Nếu B-10 trễ:** làm DLQ và thử lại trước với `InMemoryProcessedMessageStore`, nối bản JDBC sau.
- **Chia việc:** D1 làm hr-integration, D2 làm reporting và audit-admin. Làm reporting trước làm mẫu, hai service còn lại sao theo.

### D-07. Cảnh báo hạn chót ngày 1 (bắt buộc, M, 20/11, D1). Bản thu nhỏ.

- **Sửa ở:** `services/hr-integration`: use case kiểm các bản ghi chưa `SYNCED` của kỳ vừa qua, phát cảnh báo lúc 08:00 ngày 1 và báo động nếu còn sót lúc 12:00; scheduler gọi use case.
- **Xong khi:** bật được test `should_raise_a_deadline_alert_before_noon_on_the_first_day_of_the_month`, dùng `IClock` giả để tua thời gian.
- **Không làm trong đợt này:** đối soát kỳ với attendance và lô chốt (test `should_send_a_correction_batch_...` vẫn `@Disabled`).

### D-04. Hiệu chỉnh trên read model (bắt buộc, M, 23/11, D2). Bản thu nhỏ.

- **Phụ thuộc:** H4 (lược đồ, 13/11) và H12 (sự kiện thật, 20/11).
- **Sửa ở:** `ProjectAttendanceEventUseCase` xử lý `AttendanceCorrected`.
- **Xong khi:** bật được test `should_apply_a_correction_event_on_top_of_the_original`; quản đốc chỉ thấy nhà máy trong phạm vi của mình (đã có, giữ nguyên).
- **Nếu B-06 trễ:** viết theo lược đồ trong AsyncAPI, test bằng sự kiện tự tạo.
- **Không làm trong đợt này:** báo cáo theo nhóm và ca, xuất file, dashboard.

### D-13. App nhân viên dùng JWT (nên có, S đến M, 23/11, D2 cùng C2)

- **Phụ thuộc:** H10 (C-04) và C-06.
- **Sửa ở:** `apps/employee-app/src/lib/api.ts`: lưu token từ câu trả lời đăng nhập, gửi `Authorization: Bearer` thay cho `X-Subject`.
- **Xong khi:** đăng ký thiết bị chạy bằng JWT; token hết hạn thì app đưa về màn hình đăng nhập.
- **Việc đi kèm:** thử trang `/kiosk` bằng webcam thật và điện thoại thật qua `npm run dev:lan` (chưa được kiểm chứng tại thời điểm bàn giao); ghi kết quả vào runbook.
- **Nếu C-04 trễ:** cắt phần JWT, vẫn làm phần thử webcam.

### D-12. Hợp nhất báo cáo (bắt buộc, L, 23 đến 25/11, D1)

- **Đầu vào:** bản nháp của bốn team trong `docs/report/`, hạn 20/11 (H13); số đo A-09 ngày 23/11.
- **Việc:** cập nhật `docs/document.md` theo `docs/adr/README.md` mục 3; hợp nhất về một giọng văn; đánh số hình; viết phần Task 2 (module đã hiện thực, cách chạy, ảnh kết quả test và demo, URL GitHub); gom phần "Hạn chế và việc tiếp theo" từ [01-ke-hoach.md](01-ke-hoach.md) mục 5.
- **Xong khi:** qua hết `docs/runbook/release-checklist.md`; PDF nộp ngày 25/11.
- **Kiểm chéo:** mỗi khẳng định về hành vi trong báo cáo trỏ tới một test hoặc kịch bản demo có thật. Chạy lại 3 demo và chụp ảnh ở ngày 24/11, trên mã đã đóng băng.

## 5. Team D cần gì và nợ gì

| Chiều | Hạn (23:00) | Nội dung |
| --- | --- | --- |
| Giao cho A, B, C | 12/11 | D-01: compose chạy được |
| Giao cho A, B, C | 16/11 | D-02: CI |
| Nhận từ B | 13/11 | Lược đồ `AttendanceCorrected` |
| Nhận từ C | 13/11 | Hợp đồng JWT |
| Nhận từ B | 18/11 | B-02 (RabbitMQ thật), B-10 (`JdbcProcessedMessageStore`) |
| Nhận từ C | 18/11 | `identity` cấp JWT |
| Nhận từ B | 20/11 | Sự kiện `AttendanceCorrected` thật |
| Nhận từ A, B, C | 20/11 | Bản nháp báo cáo |
| Nhận từ A | 23/11 | Số đo kiểm thử tải |
| Nhận từ A | luôn luôn | API kiosk `/kiosk/v1/**` ổn định |
| Nhận từ C | luôn luôn | Định dạng token và API đăng ký thiết bị ổn định |

Team D là **bên dùng** của gần như mọi hợp đồng. Khi A, B hoặc C mở PR đổi `docs/api`, một người của Team D phải duyệt.

## 6. Bẫy cần tránh

- **Consumer phải lũy đẳng.** Broker có thể giao một thông điệp hai lần. Mọi xử lý đi qua `IdempotentConsumer`; việc ghi nhớ "đã xử lý" và việc cập nhật dữ liệu phải cùng một transaction.
- **Thông điệp đến sai thứ tự.** `Presence.supersededBy` so `aggregateVersion` để thông điệp cũ không đè trạng thái mới. Giữ cách này khi thêm loại sự kiện.
- **HR không trả lời nghĩa là "chưa biết", không phải "thất bại".** Tra cứu trước khi gửi lại. Đây là cùng một nguyên tắc với cổng xoay ở Team A.
- **Nhật ký kiểm toán không có hàm sửa hay xóa.** Có test kiểm điều này bằng reflection; đừng lách.
- **Bộ xác thực offline của kiosk phải cùng quy tắc với `user-qr`.** Team C đổi quy tắc thì `OfflineVerifier` đổi theo, và ngược lại.
- **Trang kiosk web không thay được `kiosk-simulator`.** Nó không có journal và không chịu được mất mạng. Demo ADR-11 phải dùng bản dòng lệnh.
- **Hai ứng dụng web không tự đặt danh tính trong môi trường thật.** Header `X-Subject` là tạm; đừng trình bày nó như bảo mật.
- **Sửa `docker`, `scripts`, CI là sửa cho cả nhóm.** Thử trên cả macOS và Windows trước khi hợp nhất; `local-stack.sh` và `local-stack.ps1` phải khởi động cùng một bộ service.

## 7. Phần báo cáo của Team D

Ngoài việc hợp nhất (D-12), Team D tự viết và nộp bản nháp ngày 20/11:

- Sơ đồ và mô tả tích hợp HR, triển khai.
- Bằng chứng phía kiosk cho ADR-11 (P6: mất mạng, gửi bù, không gửi trùng) và ADR-12 (R2: bộ nhớ đệm chống dùng lại tại kiosk; R4: quy tắc gian lận).
- Áp dụng SOLID trên mã của team, ví dụ: `IHrPayrollPort` với bản giả lập và bản HTTP (thay thế được), `IAuditStore` chỉ có thao tác thêm (interface thể hiện đúng ràng buộc), `FraudRule` (thêm quy tắc không sửa mã cũ).
- Task 2 và một đoạn phản ánh.
