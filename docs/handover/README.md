# Bộ tài liệu bàn giao M2

Bộ tài liệu này đủ để một team làm việc độc lập: biết mình làm gì, ở đâu, khi nào xong, chạy và kiểm thử ra sao, code theo hợp đồng và fixture nào. Từ 16/11 **không team nào chờ mã chưa xong của team khác**; phần cần mã thật của nhau chỉ là kiểm tích hợp tùy chọn vào 19/11 và 23/11.

**Thời gian:** Thứ Hai 16/11/2026 đến Thứ Năm 26/11/2026. **Đóng băng mã 23:00 ngày 24/11**, hợp nhất báo cáo 25/11, **nộp 26/11**. Mọi hạn là 23:00 của ngày được ghi.

## Đọc theo thứ tự này

| Bước | Tài liệu | Ai đọc | Thời gian đọc |
| --- | --- | --- | --- |
| 1 | [00-he-thong.md](00-he-thong.md): hệ thống làm gì, gồm những phần nào, một lượt quét đi qua đâu | Mọi người | 15 phút |
| 2 | [02-chay-local.md](02-chay-local.md): cài máy, dựng server local, Docker, chạy test, chạy demo, dùng app nhân viên | Mọi người, làm theo ngay 16/11 | 30 phút, kể cả thời gian chạy |
| 3 | [01-ke-hoach.md](01-ke-hoach.md): lịch từng người từng ngày, kiểm tích hợp tùy chọn, phạm vi bị cắt | Mọi người, lead đọc kỹ | 15 phút |
| 3b | [03-gantt.md](03-gantt.md): biểu đồ Gantt, và [lịch tương tác](https://htmlpreview.github.io/?https://github.com/lamelihuynh/tmc/blob/main/docs/handover/lich-ban-giao.html) (bấm vào từng story để xem chi tiết) | Mọi người | 5 phút |
| 4 | Tài liệu của team mình: [team-a.md](team-a.md), [team-b.md](team-b.md), [team-c.md](team-c.md), [team-d.md](team-d.md). Mỗi tệp mở đầu bằng hộp "Bạn có thể bắt đầu ngay" | Team tương ứng | 20 phút |
| 5 | [_decoupling-changelog.md](_decoupling-changelog.md): những gì bước gỡ phụ thuộc đã làm, đã chạy, còn chưa kiểm chứng | Lead | 10 phút |

## Bốn team

| Team | Phụ trách | Mã nguồn sở hữu |
| --- | --- | --- |
| **A. Gate Station** | Điều phối một lượt quét tại cổng, điều khiển cổng xoay, kho cấu hình cổng | `services/gate-station`, `simulators/turnstile-simulator` |
| **B. Attendance** | Sổ ghi lượt quét, máy trạng thái, hiệu chỉnh, outbox phát sự kiện | `services/attendance` |
| **C. User/QR và Identity** | Khóa thiết bị, xác thực QR, danh bạ nhân viên, đăng nhập, JWT | `services/user-qr`, `services/identity`, `simulators/device-simulator`, `platform/security` |
| **D. Kiosk, Portal và Central** | Kiosk, hai ứng dụng web, báo cáo, tích hợp HR, kiểm toán, CI, hạ tầng, báo cáo cuối | `simulators/kiosk-simulator`, `apps/*`, `services/reporting`, `services/hr-integration`, `services/audit-admin`, `docker`, `scripts`, `.github` |

## Việc đầu tiên sáng 16/11

Mọi thứ "đã có sẵn" trong bộ tài liệu này nằm trên nhánh `chore/x/decouple-teams`. Lead Team D mở PR nhánh đó vào `main`, bốn lead duyệt, CI xanh, hợp nhất (Việc 0 trong [team-d.md](team-d.md)). Sau đó mọi người:

```bash
git switch main && git pull
./gradlew build                 # 209 test đơn vị, 10 bỏ qua có chủ đích
make up                         # cần Docker; container cũ thiếu database gate_store thì: make nuke && make up
./gradlew integrationTest       # 27 test tích hợp, cần Docker
```

## Quan hệ với tài liệu có sẵn

- Bộ này **thay thế** phần lịch, mốc và phụ thuộc của [`docs/team/WORK-BREAKDOWN.md`](../team/WORK-BREAKDOWN.md). Mã story (A-01, B-02, ...) giữ nguyên; story mới (A-11, A-12, C-11, C-12, D-15, D-16) và báo cáo từng team (R-A, R-B, R-C) được thêm vào backlog ở đó.
- Quy ước viết mã, commit, review, kiểm thử ở [`docs/conventions/`](../conventions/) và [`CONTRIBUTING.md`](../../CONTRIBUTING.md).
- Quyết định kiến trúc ở [`docs/adr/`](../adr/) (ADR-11 đến ADR-15 đã Accepted); hợp đồng API ở [`docs/api/`](../api/), fixture dùng chung ở [`docs/api/fixtures/`](../api/fixtures/).
- Bản trực quan cũ của lịch: <https://claude.ai/artifact/HJQqZixiUDbWPr9JQcR3Kp> (trang riêng tư, lịch 12/11 đến 25/11 cũ; dùng [lich-ban-giao.html](lich-ban-giao.html) thay thế).

## Hiện trạng ngày 16/11/2026

Đã kiểm chứng trên macOS arm64, JDK 21, Docker đang chạy (chi tiết: [`_decoupling-changelog.md`](_decoupling-changelog.md) mục 3):

| Hạng mục | Kết quả |
| --- | --- |
| `./gradlew spotlessCheck test` toàn repo | **209 test, 10 bỏ qua** (`@Disabled("TODO Team X")`), **0 lỗi** |
| `./gradlew integrationTest --rerun` toàn repo | **27 test, 0 lỗi** trên container thật (messaging 5, attendance 8, gate-station 7, user-qr 3, hr-integration 2, reporting 1, audit-admin 1) |
| D-01: 7 jar ở profile mặc định trên hạ tầng `make up` | Cả 7 khởi động; đăng nhập trả JWT; user-qr nhận Bearer (201), từ chối header dev (403); sync bundle có 5 nhân viên; sự kiện audit đi qua RabbitMQ tới audit-admin |
| Stack `local` (5 tiến trình, có identity) và 3 demo | `online`, `offline`, `timeout` đều qua |
| App nhân viên | `npm test` (5 test), lint, build xanh |

Chưa kiểm chứng (các team không bị chặn bởi những mục này; xem kiểm tích hợp I-6 và story tương ứng):

| Hạng mục | Story hoặc kiểm tích hợp |
| --- | --- |
| 3 demo trên stack Docker đầy đủ (mới chạy trên `local`) | I-6 |
| Dừng và bật lại RabbitMQ giữa lúc phát | B-02 |
| ~~Outbox của identity bền; read model reporting, log HR, kho audit trong bộ nhớ~~ Đã xong 27/11: `attendance_projection`, `sync_log`, `audit_entries` trên PostgreSQL (kèm IT) | C-11, D-04, D-06, D-08 |
| ~~JWT mới gắn ở user-qr~~ Đã xong 27/11 cho reporting, hr-integration, audit-admin, web-portal, employee-app (`BearerTokenApiTest`, vitest) | A-11, B-06, D-13 |
| `apps/web-portal` `npm test`; CI trên GitHub cho các thay đổi của nhánh | Việc 0 |
| Trang `/kiosk` quét bằng webcam thật | D-16 |
| `device-simulator` chưa nằm trong contract test chính sách token | Ghi vào phần hạn chế |
