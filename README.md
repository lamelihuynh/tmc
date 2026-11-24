# TMC: hệ thống kiểm soát ra vào và chấm công bằng mã QR

Hệ thống cho TMC Manufacturing (hai nhà máy Bình Dương và Long An, khoảng 1.400 nhân viên): nhân viên đưa mã QR trên điện thoại vào đầu quét, hệ thống kiểm tra, mở cổng xoay, và ghi một giao dịch chấm công. Dữ liệu được chuyển sang HR/Payroll để đối soát trước 12:00 ngày 1 hằng tháng. Đây là bài tập môn Kiến trúc phần mềm (Assignment M2): thiết kế kiến trúc, áp dụng SOLID, và hiện thực module chính.

M2 bổ sung hai vấn đề phải giải quyết: **mất kết nối giữa chừng một giao dịch** và **QR tĩnh bị lộ**. Lời giải nằm ở [ADR-11](docs/adr/ADR-11-offline-first-store-and-forward.md) và [ADR-12](docs/adr/ADR-12-dynamic-qr-device-binding.md).

## Kiến trúc trong một hình

```mermaid
flowchart LR
  phone([Điện thoại<br/>QR 30 giây, ký bằng khóa thiết bị]) --> kiosk[Kiosk<br/>journal ghi trước, xác thực offline]
  kiosk --> gate[gate-station<br/>điều phối]
  gate --> qr[user-qr<br/>xác thực QR, thiết bị]
  gate --> att[attendance<br/>nguồn dữ liệu chấm công]
  gate --> turn[Turnstile hiện hữu]
  att -- sự kiện qua Outbox --> mq[[RabbitMQ]]
  mq --> rep[reporting]
  mq --> hr[hr-integration] --> hrp[HR/Payroll hiện hữu]
  mq --> aud[audit-admin]
```

Microservices + Event-Driven (ADR-01, 02): đường quét QR chạy **đồng bộ tại biên** (nhà máy) để đáp ứng 2 giây; báo cáo, kiểm toán và đồng bộ HR chạy **bất đồng bộ** ở trung tâm. Mỗi service theo kiến trúc lục giác. Sơ đồ chi tiết: [docs/architecture/c4-edge.md](docs/architecture/c4-edge.md).

## Chạy thử trong 3 phút (không cần Docker)

Yêu cầu: JDK 21. Chi tiết: [docs/runbook/local-setup.md](docs/runbook/local-setup.md).

```bash
make build           # biên dịch và chạy test đơn vị và kiến trúc
make local-up        # khởi động turnstile giả lập, user-qr, attendance, gate-station
make demo-online     # quét, replay bị chặn, token giống QR tĩnh bị chặn, anti-passback
make local-restart && make demo-offline   # kiosk mất mạng, tự xác thực, giao journal sau
make local-restart && make demo-timeout   # cổng mở nhưng phản hồi mất: không mở hai lần, đối soát sau
make local-down
```

`make help` liệt kê mọi lệnh. Kịch bản demo giải thích từng bước: [docs/runbook/demo-scenarios.md](docs/runbook/demo-scenarios.md).

## Sản phẩm khi chạy

Ảnh và GIF dưới đây được chụp tự động từ ứng dụng thật chạy với server thật (stack local), không phải bản vẽ. Tạo lại bằng `scripts/docs/capture-screenshots.mjs`.

<p align="center">
  <img src="docs/screenshots/employee-app-demo.gif" width="260" alt="Luồng dùng app nhân viên: đăng nhập, đăng ký thiết bị, hiện QR, cổng mở, dùng lại QR bị từ chối">
</p>

### App nhân viên (`apps/employee-app`)

| Đăng nhập | Đăng ký thiết bị | Mã QR đổi mỗi 20 giây | Cổng mở |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/01-dang-nhap.png" width="200" alt="Màn hình đăng nhập"> | <img src="docs/screenshots/02-dang-ky-thiet-bi.png" width="200" alt="Đăng ký thiết bị"> | <img src="docs/screenshots/03-ma-qr.png" width="200" alt="Mã QR"> | <img src="docs/screenshots/04-cong-mo.png" width="200" alt="Cổng mở"> |
| Tài khoản mẫu `nv001` đến `nv005` | Khóa riêng tạo trên máy, không rời thiết bị | Mỗi mã sống 30 giây, dùng một lần | gate-station trả `ALLOWED`, chiều vào |

| Dùng lại mã (ảnh chụp màn hình) | Mã chứa họ tên | Nội dung token |
| :---: | :---: | :---: |
| <img src="docs/screenshots/05-dung-lai-bi-tu-choi.png" width="200" alt="Dùng lại QR bị từ chối"> | <img src="docs/screenshots/06-qr-chua-ten-bi-tu-choi.png" width="200" alt="QR chứa tên bị từ chối"> | <img src="docs/screenshots/07-noi-dung-token.png" width="200" alt="Nội dung token"> |
| `QR_ALREADY_USED` | `MALFORMED_TOKEN` | Chỉ 6 claim, không có dữ liệu cá nhân |

### Trang kiosk tại cổng (`/kiosk`)

<p align="center">
  <img src="docs/screenshots/08-kiosk.png" width="420" alt="Trang kiosk: cổng mở sau khi nhận mã QR">
</p>

### Web portal cho các vai trò khác (`apps/web-portal`)

Ba ảnh dưới đây chụp khi cả 8 service chạy trên PostgreSQL, Redis và RabbitMQ thật (Docker), vì trang của quản đốc và HR chỉ có dữ liệu khi sự kiện đi qua RabbitMQ. Tạo lại bằng `scripts/docs/capture-portal.mjs`.

| Vai trò | Màn hình |
| --- | --- |
| **Bảo vệ** (`SECURITY_GUARD`): xem danh mục cổng, mở cổng thủ công với lý do bắt buộc. Chỉ mở được cổng mình được phân công | <img src="docs/screenshots/09-bao-ve-mo-cong-thu-cong.png" width="560" alt="Bảo vệ mở cổng thủ công"> |
| **Quản đốc** (`PRODUCTION_SUPERVISOR`): ai đang ở trong nhà máy, chỉ trong phạm vi nhà máy của mình | <img src="docs/screenshots/10-quan-doc-hien-dien.png" width="560" alt="Quản đốc xem hiện diện"> |
| **Quản trị hệ thống** (`SYSTEM_ADMIN`): theo dõi công đã đồng bộ sang HR/Payroll | <img src="docs/screenshots/11-hr-dong-bo.png" width="560" alt="Theo dõi đồng bộ HR"> |

Chưa có màn hình cho: Ban giám đốc (`EXECUTIVE`, dashboard thuộc D-04 đã thu nhỏ), HR hiệu chỉnh công (B-06 chưa làm), và trang tra cứu nhật ký kiểm toán (API `GET /admin/v1/audit` đã có, chưa có trang). Thanh "DEV" màu vàng là cách chọn danh tính tạm thời khi phát triển, sẽ thay bằng đăng nhập thật.

Lịch làm việc của 4 team: [mở lịch tương tác](https://htmlpreview.github.io/?https://github.com/lamelihuynh/tmc/blob/main/docs/handover/lich-ban-giao.html), hoặc xem [biểu đồ Gantt](docs/handover/03-gantt.md).

### Chạy trong 4 bước

```bash
# 1. Build (lần đầu vài phút). Trên Windows thêm: -x :simulators:kiosk-simulator:test
./gradlew build

# 2. Dựng 5 service local, không cần Docker
pwsh scripts/dev/local-stack.ps1 start        # Windows
make local-up && ./gradlew :services:identity:bootRun   # macOS, Linux

# 3. Chạy app nhân viên, rồi mở http://localhost:5174 (kiosk: http://localhost:5174/kiosk)
cd apps/employee-app && npm install && npm run dev

# 4. Chạy kịch bản demo dòng lệnh
bash scripts/demo/online.sh
```

Kết quả của bước 2 và bước 4 khi mọi thứ chạy đúng:

```text
turnstile     :8090  UP
user-qr       :8083  UP
attendance    :8082  UP
gate-station  :8081  UP
identity      :8087  UP

== 3. Scan a fresh 30 s token at the entry gate
   ALLOWED attempt=01a1c834-d81f-7eeb-8964-f97c8dcf0b3d reason=null direction=ENTRY mode=ONLINE
   OK   got "ALLOWED"
== 4. The same QR used again (a screenshot) is refused, the gate stays closed
   OK   got "QR_ALREADY_USED"
== 5. A token that looks like a static QR (24 h) is refused
   OK   got "MALFORMED_TOKEN"
== 7. Two entries in a row are refused by anti-passback
   OK   got "ANTI_PASSBACK_VIOLATION"
== online demo passed
```

Hướng dẫn đầy đủ, kể cả dùng điện thoại thật và xử lý sự cố: [docs/handover/02-chay-local.md](docs/handover/02-chay-local.md).

## Bản đồ kho mã

| Thư mục | Nội dung | Team |
| --- | --- | --- |
| `services/gate-station` | Điều phối luồng quét, chính sách ra vào, anti-passback, ACL tới Turnstile, mở thủ công | A |
| `services/attendance` | Nguồn dữ liệu chấm công, máy trạng thái lượt quét, Outbox, nhập offline, phục hồi, đối soát | B |
| `services/user-qr` | Xác thực QR theo khóa thiết bị, đăng ký và thu hồi thiết bị, gói đồng bộ | C |
| `services/identity` | Tài khoản nhân sự, khóa tài khoản, MFA (chưa cấp JWT) | C |
| `services/reporting`, `hr-integration`, `audit-admin` | Consumer phía trung tâm | D |
| `simulators/turnstile-simulator` | Turnstile giả có chèn lỗi | A |
| `simulators/device-simulator` | Điện thoại giả: sinh khóa, đăng ký, ký token QR | C |
| `simulators/kiosk-simulator` | Kiosk giả: journal SQLite, xác thực offline, giao batch | D |
| `apps/web-portal` | Guard Console và báo cáo (React) | D |
| `apps/employee-app` | App nhân viên (đăng nhập, QR động) và trang kiosk quét bằng webcam (React) | D, C phối hợp |
| `platform/*` | Thư viện dùng chung, **không chứa nghiệp vụ** | cả nhóm |
| `docker/`, `scripts/`, `.github/` | Hạ tầng, demo, CI | D |
| `docs/` | Báo cáo M1, ADR, kiến trúc, API, runbook, quy ước, kế hoạch team | cả nhóm |

## Tài liệu

| Bạn muốn | Đọc |
| --- | --- |
| **Bắt đầu ở đây:** bàn giao, lịch 12/11 đến 25/11, việc của từng team, runbook | [docs/handover/README.md](docs/handover/README.md) |
| Backlog gốc và tiêu chí hoàn thành của từng story | [docs/team/WORK-BREAKDOWN.md](docs/team/WORK-BREAKDOWN.md) |
| Đóng góp mã | [CONTRIBUTING.md](CONTRIBUTING.md), [docs/conventions/](docs/conventions/) |
| Hiểu quyết định kiến trúc | [docs/adr/](docs/adr/ADR-00-index.md) |
| Tìm lớp nào hiện thực SOLID | [docs/architecture/solid-map.md](docs/architecture/solid-map.md) |
| Biết mã khác ADR chỗ nào, còn thiếu gì | [docs/architecture/implementation-notes.md](docs/architecture/implementation-notes.md) |
| Máy trạng thái và 7 điểm đứt | [docs/architecture/state-machine.md](docs/architecture/state-machine.md) |
| Hợp đồng API và sự kiện | [docs/api/](docs/api/README.md) |
| Chạy, vận hành, xử lý sự cố | [docs/runbook/](docs/runbook/local-setup.md) |
| Nộp bài | [docs/runbook/release-checklist.md](docs/runbook/release-checklist.md) |
| Báo cáo M1 và đề M2 | [docs/document.md](docs/document.md), [docs/SA_CC_hk261_Assignment_M2.pdf](docs/SA_CC_hk261_Assignment_M2.pdf) |

## Tình trạng hiện tại (trung thực)

**Đã kiểm chứng:** build và toàn bộ test đơn vị, test kiến trúc của mọi module; ba kịch bản demo chạy trên bốn tiến trình thật qua HTTP (đường thành công và các từ chối, kiosk offline, timeout cổng có đối soát).

**Chưa kiểm chứng (cần Docker):** adapter PostgreSQL, Redis, RabbitMQ và migration; kiểm thử hiệu năng (2 giây ở tải đỉnh); mTLS. Việc kiểm chứng đầu tiên là story D-01.

**Đã biết còn thiếu và có người nhận:** [docs/architecture/implementation-notes.md](docs/architecture/implementation-notes.md) mục 3 đến 7. Đáng chú ý: anti-passback chưa cập nhật sau khi nhập offline (A-02), danh bạ nhân viên đang đọc từ cấu hình (C-02), identity chưa cấp JWT (C-04).

## Công nghệ

Java 21, Spring Boot 3.3, Gradle (Kotlin DSL) đa module, PostgreSQL 16, Redis 7, RabbitMQ 3.13, Nimbus JOSE (ES256), Flyway, ArchUnit, Testcontainers; picocli và SQLite cho công cụ mô phỏng; React 18, Vite, TypeScript cho portal.

## Nhóm thực hiện

4 team, mỗi team 2 người; danh sách và phân công ở [docs/team/WORK-BREAKDOWN.md](docs/team/WORK-BREAKDOWN.md).
