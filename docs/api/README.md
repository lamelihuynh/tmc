# Hợp đồng API và sự kiện

Nguồn sự thật cho giao tiếp giữa các thành phần. **Đổi hợp đồng thì sửa tệp ở đây trong cùng PR với mã**, và thêm reviewer của bên dùng (`docs/team/WORK-BREAKDOWN.md` mục 5). Quy ước chung: `docs/conventions/api-design-guidelines.md`.

| Tệp | Hợp đồng | Bên cung cấp | Bên dùng | Khớp với mã? |
| --- | --- | --- | --- | --- |
| [kiosk-edge.openapi.yaml](kiosk-edge.openapi.yaml) | Kiosk API, mở thủ công, quan sát cổng | gate-station | kiosk, Guard Console, attendance | Có (`KioskController`, `SecurityController`, `InternalController`) |
| [internal-attendance.openapi.yaml](internal-attendance.openapi.yaml) | Ghi nhận lượt quét, kết quả cổng, nhập offline | attendance | gate-station | Có (`InternalAttendanceController`) |
| [user-qr.openapi.yaml](user-qr.openapi.yaml) | Thiết bị, xác thực QR, gói đồng bộ | user-qr | điện thoại, HR, gate-station, kiosk | Có (`DeviceController`, `QrVerifyController`) |
| [turnstile.openapi.yaml](turnstile.openapi.yaml) | API Turnstile hiện hữu (M1 mục 16.3) và quản trị của bản giả lập | Turnstile Control System | gate-station | Có với `turnstile-simulator`; **phần "trạng thái lệnh cuối" là giả định** về API thật, xem tệp |
| [central.openapi.yaml](central.openapi.yaml) | Báo cáo hiện diện, đồng bộ HR, audit, đăng nhập | reporting, hr-integration, audit-admin, identity | Portal | Có, mức khung |
| [hr-payroll.openapi.yaml](hr-payroll.openapi.yaml) | API HR/Payroll hiện hữu (M1 mục 16.4), **đã sửa header** | HR/Payroll | hr-integration | **Chưa hiện thực** (story D-06); thân yêu cầu cần đối chiếu M1 |
| [attendance-events.asyncapi.yaml](attendance-events.asyncapi.yaml) | `AttendanceEvents` trên RabbitMQ | attendance | reporting, hr-integration, audit-admin | Có (`AttendanceEventFactory`, `AmqpEventPublisher`); AMQP thật chưa kiểm chứng |

## Cách kiểm tra cú pháp

```bash
# cú pháp YAML (không thay thế việc kiểm tra OpenAPI đầy đủ)
for f in docs/api/*.yaml; do ruby -ryaml -e 'YAML.load_file(ARGV[0]); puts "ok #{ARGV[0]}"' "$f"; done
# hoặc, nếu có PyYAML:  python3 -c "import sys,yaml; [yaml.safe_load(open(f)) for f in sys.argv[1:]]" docs/api/*.yaml
```

Hoặc dán vào https://editor.swagger.io (OpenAPI) và https://studio.asyncapi.com (AsyncAPI).

## Điều chưa có

- Hợp đồng chạy thử tự động (contract test) giữa các bên: chưa có. Hiện tính đúng đắn dựa vào test của từng bên cùng một bản fake (`FakeAttendanceGateway`, `FakeEdge`). Đề xuất thêm vào D-02.
- Các API quản trị còn lại (FR-11 đến FR-16, FR-31, FR-32, FR-35) chưa được định nghĩa vì chưa có mã.
