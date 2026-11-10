# ADR-13: Chốt thành phần điều phối luồng quét tại biên (Edge Access Orchestrator)

| Trạng thái | Proposed (đề xuất cho M2, chờ nhóm duyệt) |
| --- | --- |
| Ngày | 07/11/2026 |
| Vấn đề giải quyết | Mâu thuẫn nội bộ trong M1; cần một tên và một ranh giới rõ trước khi code Task 2 |
| Thay thế / mở rộng | Làm rõ ADR-01, ADR-03, ADR-06 ở mức triển khai tại biên |

## Bối cảnh (Context)

Báo cáo M1 mô tả luồng quét QR theo hai cách khác nhau:

| Mục M1 | Thành phần điều phối | Thành phần xác thực QR |
| --- | --- | --- |
| 12.4 và 13.1 (component, sequence) | **Gate Station Service** gọi `IQRValidation`, `IAttendanceCommand`, `ITurnstileController` | User/QR Service |
| 15.2 (deployment tại nhà máy) | **Attendance Service (Edge)** gọi QR Verifier, Redis, Gate Station Service, Turnstile Adapter | QR Verifier (thành phần riêng tại Edge) |

Hai cách này không thể cùng đúng. Ngoài ra, mục 11.2 nói User/QR Service đặt tại Central, nhưng xác thực QR phải diễn ra tại Edge trong ngân sách 2 giây và khi đứt WAN. Nếu không chốt, code Task 2 sẽ không khớp với bất kỳ sơ đồ nào.

## Quyết định (Decision)

### 1. Tách rõ "quyền sở hữu nghiệp vụ" và "nơi chạy"

- **Gate Station Service** giữ nguyên trách nhiệm trong ADR-01: sở hữu danh mục trạm, cấu hình chiều cổng, chính sách ra vào, trạng thái anti-passback và Turnstile Adapter. Nó là **chủ sở hữu quyết định cho phép qua cổng**.
- **Attendance Service** giữ nguyên trách nhiệm trong ADR-03: sở hữu bản ghi chấm công chuẩn và Outbox.
- **User/QR Service** giữ nguyên trách nhiệm: sở hữu hồ sơ, vòng đời QR, Device Key Registry (ADR-12).

Cả ba đều có **Edge instance** chạy tại nhà máy (mục 14.2 M1 đã nói Attendance và Gate Station có Edge instance; ADR này bổ sung **QR Verifier là Edge instance chỉ-đọc của User/QR Service**, dùng dữ liệu kéo về qua `sync-bundle`).

### 2. Một thành phần điều phối duy nhất: `RecordScanUseCase` trong Gate Station (Edge)

Thành phần điều phối luồng quét là **use case `RecordScanUseCase` thuộc tầng Application của Gate Station Service**, chạy tại Edge. Lý do chọn Gate Station thay vì Attendance:

- Quyết định cho phép qua cổng là nghiệp vụ của Gate Station (chính sách, chiều cổng, anti-passback). Attendance chỉ **ghi nhận kết quả**, không quyết định.
- Turnstile Adapter thuộc Gate Station (ADR-06); để Attendance gọi Turnstile sẽ vi phạm ranh giới.
- Giữ đúng sequence ở mục 13.1 của M1, chỉ cần sửa mục 15.2.

Thứ tự điều phối (khớp máy trạng thái ADR-11):

```
Kiosk ──POST /kiosk/v1/scans──► Edge Gateway ──► RecordScanUseCase (Gate Station, Edge)
   1. IQRValidation.verify(token)            → QR Verifier (Edge instance của User/QR)
   2. AccessPolicy.evaluate(...)             → trong Gate Station (domain)
   3. ITokenReservation.reserve(jti)         → Redis Edge
   4. IAttendanceCommand.recordAttempt(...)  → Attendance (Edge): ghi RESERVED/GATE_COMMANDED
   5. ITurnstileControlPort.openGate(...)    → Turnstile Adapter (Gate Station)
   6. IAttendanceCommand.recordGateResult()  → Attendance (Edge): COMMITTED + Outbox, token USED
   7. trả ScanResult về kiosk
```

### 3. Ranh giới port mà code Task 2 phải tuân theo

| Port (interface) | Chủ sở hữu interface | Ai hiện thực | Dùng trong bước |
| --- | --- | --- | --- |
| `IQRValidation` | Gate Station Application | QR Verifier (User/QR Edge) | 1 |
| `IAccessPolicy` (danh sách strategy) | Gate Station Domain | Các rule: `ActiveEmployeeRule`, `FactoryPermissionRule`, `GateDirectionRule`, `AntiPassbackRule` | 2 |
| `ITokenReservation` | Gate Station Application | Redis adapter; in-memory adapter cho test | 3 |
| `IAttendanceCommand` | Attendance Application (Gate Station là client) | Attendance Edge; in-memory cho test | 4, 6 |
| `ITurnstileControlPort` | Gate Station Application | `HttpTurnstileAdapter`; `SimulatedTurnstileAdapter` | 5 |
| `IOutboxWriter` | Attendance Application | PostgreSQL; in-memory cho test | 6 |
| `IDeviceKeyRegistry`, `ITokenReplayStore`, `IClock` | theo ADR-12 | theo ADR-12 | 1, 3 |

Quy tắc phụ thuộc: Application chỉ phụ thuộc Domain và port; adapter hiện thực port; không có adapter nào gọi adapter khác trực tiếp. Đây là chỗ thể hiện SRP, OCP, LSP, ISP, DIP trong báo cáo Task 2.

### 4. Đổi tên trong tài liệu

- Mục 15.2: đổi "Attendance Service: Điều phối luồng nghiệp vụ chính..." thành "Gate Station Service (Edge): điều phối luồng quét qua `RecordScanUseCase`..." và "Attendance Service (Edge): ghi nhận lượt và kết quả, sở hữu `access_attempts`, `attendance_transactions`, `outbox_events`".
- Mục 15.2: ghi rõ "QR Verifier = Edge instance chỉ-đọc của User/QR Service".
- Mục 12.4, 13.1: giữ nguyên, bổ sung các trạng thái của ADR-11.

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Attendance (Edge) điều phối như mục 15.2 | Attendance phải biết chính sách cổng và gọi Turnstile → vi phạm ADR-03, ADR-06 |
| Gộp Gate Station và Attendance tại Edge thành một "Edge Service" | Đơn giản hơn để code, nhưng phá ranh giới sở hữu dữ liệu của ADR-01, ADR-10 và làm mất lập luận SOLID |
| Gate Station (Edge) điều phối, Attendance (Edge) ghi nhận, QR Verifier là Edge instance của User/QR | **Chọn**: khớp mục 12.4, 13.1 của M1; chỉ sửa mục 15.2 |

## Hệ quả (Consequences)

(+) Một tên, một ranh giới, một thứ tự gọi; sequence, component và code khớp nhau.
(+) Code Task 2 có cấu trúc thư mục rõ: `gate-station/{domain,application,adapters}`, `attendance/{...}`, `user-qr/{...}`.
(–) Phải sửa mục 15.2 và hình 15.2.1 trong báo cáo.
(–) Tại Edge có ba tiến trình logic (Gate Station, Attendance, QR Verifier); trong Task 2 có thể triển khai cùng một tiến trình nhưng tách module, và phải ghi rõ điều đó trong README.

## Kiểm chứng tuân thủ (Compliance)

1. Kiểm tra phụ thuộc bằng công cụ (ArchUnit hoặc tương đương): Domain không import Application, Application không import adapter, `attendance` không import `gate-station` và ngược lại ngoài qua port.
2. Thay `HttpTurnstileAdapter` bằng `SimulatedTurnstileAdapter` chỉ qua cấu hình, không sửa use case.
3. Thêm một rule mới vào `IAccessPolicy` (ví dụ `ShiftWindowRule`) chỉ bằng thêm lớp, test cũ không đổi.

## Liên kết

AC-01, AC-09 · ADR-01, ADR-03, ADR-06, ADR-10 · ADR-11, ADR-12 · mục 12.4, 13.1, 15.2 của M1
