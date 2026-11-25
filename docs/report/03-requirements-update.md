# Cập nhật yêu cầu và Use Case (Task 1.1)

Phần này trả lời yêu cầu **Task 1.1** về việc cập nhật các yêu cầu phi chức năng, ca sử dụng và đặc tính kiến trúc trong `docs/document.md` (M1) để đồng bộ với các quyết định ADR-11, ADR-12 và ADR-13 tại Milestone 2.

> [!NOTE]
> Tệp này gồm phần **NFR-09** và **UC-08** do **Team B** cập nhật theo `docs/adr/README.md` (mục 3). Các mục của Team C (**FR-19**, **NFR-05**, **UC-07**, **AC-03**, **AC-04**) để sẵn khung bên dưới để Team C bổ sung và Team D hợp nhất (D-12).

---

## 1. Cập nhật Yêu cầu phi chức năng — NFR-09 (Team B)

So với M1 ([`docs/document.md`](../document.md), mục 4.3), **NFR-09** được bổ sung tiêu chí đo lường cụ thể cho mất kết nối giữa chừng (ADR-11):

| ID | Phân loại | Mô tả chi tiết (M2) | Tiêu chí đo lường / Ràng buộc (M2) |
| --- | --- | --- | --- |
| **NFR-09** | Tính sẵn sàng & Dự phòng mất mạng | Hệ thống phải duy trì hoạt động liên tục và bảo toàn toàn vẹn giao dịch khi mất kết nối mạng nội bộ/WAN hoặc tiến trình gặp sự cố **ở bất kỳ bước nào** của lượt quét (từ lúc quét tại kiosk tới khi phát sự kiện về trung tâm). | **0 bản ghi chấm công bị mất hoặc tính trùng** và **0 lần mở cổng lặp** khi đứt kết nối tại bất kỳ điểm nào (P1–P7). Kiosk phải ghi **Kiosk Journal** (append-only SQLite, lưu hash token) **trước** khi gọi mạng; mọi lệnh mở cổng ghi trước trạng thái `GATE_COMMANDED` bền vững; khi timeout chuyển `GATE_UNKNOWN` để đối soát thay vì gửi lại lệnh mở; Outbox chỉ chuyển `PUBLISHED` sau khi broker xác nhận. |

---

## 2. Cập nhật Ca sử dụng — UC-08: Quét QR vào/ra (Team B)

So với M1 ([`docs/document.md`](../document.md), mục 5.8), **UC-08** được điều chỉnh theo ADR-11, ADR-12 và ADR-13 như sau:

### 2.1. Điều chỉnh Luồng chính và Luồng thay thế
- **Xóa luồng thay thế `1.2` ("Nhân viên dùng QR nhận qua email"):** Loại bỏ hoàn toàn QR tĩnh qua email theo ADR-12; mọi QR sử dụng tại cổng phải là token động ES256 ký bằng khóa riêng gắn thiết bị đã đăng ký (`kid`), thời hạn tối đa 60 giây.
- **Chuẩn hóa bước 2–6 của Luồng chính theo ADR-13 & ADR-11:**
  - Bước 2: Kiosk sinh `attempt_id` (UUID v7), ghi vào **Kiosk Journal** (`RECEIVED`, `journal_seq`) **trước** khi gọi `POST /kiosk/v1/scans` kèm header `Idempotency-Key = attempt_id`.
  - Bước 3–4: `gate-station` kiểm tra idempotency với `attendance`, gọi `user-qr` xác thực chữ ký/thiết bị/thời hạn, áp chính sách và giữ chỗ nguyên tử `jti` + anti-passback.
  - Bước 5: `attendance` ghi `RESERVED`, sau đó ghi **`GATE_COMMANDED` (write-ahead)** trước khi `gate-station` gọi Turnstile (`cmd-ts-<attempt_id>`).
  - Bước 6: Khi Turnstile trả `OPENED`, `attendance` cập nhật `GATE_OPENED → COMMITTED`, tạo `AttendanceTransaction` (`saveIfAbsent`) và ghi sự kiện `AttendanceConfirmed` vào `outbox_events` **trong cùng một transaction PostgreSQL**.

### 2.2. Viết lại các Luồng ngoại lệ `2.x`, `5.1` và `6.1` theo máy trạng thái ADR-11

| Mã ngoại lệ | Tình huống | Xử lý chuẩn hóa theo máy trạng thái (M2) |
| --- | --- | --- |
| **2.1 (Kiosk mất mạng tới Edge)** | Không kết nối được `gate-station` trước khi gửi hoặc sau số lần thử lại | Kiosk tự xác thực token động bằng khóa thiết bị và danh bạ/danh sách thu hồi từ lần đồng bộ gần nhất, kiểm tra replay cache cục bộ (2 phút), ghi journal (`OFFLINE_OPENED` hoặc `OFFLINE_DENIED`) trước khi mở cổng qua Local Gate Link. |
| **2.2 (Khôi phục mạng sau offline)** | Kiosk đẩy các dòng chưa bàn giao (`handed_over = 0`) | Kiosk `flush` theo thứ tự `journal_seq` tới `POST /kiosk/v1/scans/offline-batch` (Edge Gateway chuyển sang `POST /internal/v1/offline-batch` của `attendance` — `IngestOfflineBatchUseCase`). `attendance` bỏ qua `attempt_id` đã có (`DUPLICATE`); gắn cờ bất thường `TOKEN_REUSED_OFFLINE` hoặc `ANTI_PASSBACK_OFFLINE` (không tính công, đẩy vào hàng đợi HR `GET /hr/v1/anomalies` cho UC-09); hợp lệ thì commit `OFFLINE` + outbox. Tuyệt đối không phát lại lệnh mở cổng. |
| **2.3 (Kiosk chưa có sync hoặc journal lỗi)** | Kiosk offline nhưng chưa có khóa thiết bị hoặc không ghi được journal | Không mở cổng, hiển thị lỗi để Security Guard xử lý mở thủ công (UC-06). |
| **2.4 (Mất phản hồi giữa chừng – P1/P5)** | Kiosk gửi online nhưng rớt phản hồi hoặc Edge trả `IN_PROGRESS` | Kiosk gửi lại với **cùng `attempt_id`**: nếu Edge đã chốt kết quả cuối thì trả lại (`duplicate = true`); nếu đang xử lý (`IN_PROGRESS`) thì kiosk hỏi lại tối đa 6 lần × 300 ms, quá giới hạn ghi `PENDING` (không tự mở cổng cục bộ). |
| **5.1 (Turnstile timeout hoặc lỗi – P3/P4)** | Turnstile quá hạn chờ (0,8 giây) hoặc trả `504`/`ERROR` | Nếu Turnstile báo lỗi phần cứng (`ERROR`), chuyển `GATE_FAILED` (`GATE_HARDWARE_ERROR`) và nhả `jti` để nhân viên quét lại. Nếu `TIMEOUT`, chuyển **`GATE_UNKNOWN`**, trả kiosk `HTTP 202 PENDING` (`TURNSTILE_TIMEOUT_RETRY_SCAN`), **không gửi lại lệnh mở** và giữ khóa `jti`. Job nền `RecoverStaleAttemptsUseCase` chuyển `GATE_COMMANDED` bị treo quá 10 giây sang `GATE_UNKNOWN`, và `ReconcileUnknownUseCase` hỏi trạng thái cổng để chốt `COMMITTED` hoặc `GATE_FAILED`. |
| **6.1 (Lỗi lưu kết quả sau khi cổng đã mở – P5 / Crash)** | Cổng trả `OPENED` nhưng không lưu được vào `attendance` | `gate-station` trả `UNCONFIRMED`; bản ghi `GATE_COMMANDED` trong PostgreSQL được phục hồi sang `GATE_UNKNOWN` rồi đối soát chốt `COMMITTED` + công, bảo đảm không mất công của nhân viên. |

---

## 3. Phần chờ các team khác (D-12 hợp nhất)

- **FR-19, NFR-05, UC-07:** *[Chờ Team C điền theo `docs/adr/README.md` mục 3]*
- **AC-03, AC-04:** *[Chờ Team C / Team D bổ sung fitness functions]*

