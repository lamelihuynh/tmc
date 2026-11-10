# ADR-11: Offline-first store-and-forward với máy trạng thái giao dịch tại cổng

| Trạng thái | Proposed (đề xuất cho M2, chờ nhóm duyệt) |
| --- | --- |
| Ngày | 06/11/2026 |
| Vấn đề giải quyết | I-1: Hệ thống mất kết nối trong lúc đang xử lý giao dịch chấm công |
| Thay thế / mở rộng | Mở rộng ADR-04 (Outbox), ADR-05 (Idempotency); cụ thể hóa mục 14.5 của M1 |

## Bối cảnh (Context)

M1 đã xử lý trường hợp kiosk **đã mất mạng từ trước** khi quét (chế độ offline, hàng đợi SQLite) và trường hợp **đứt WAN** giữa nhà máy và Central Site (Outbox đẩy lại sau). Đề bài M2 nêu rõ hơn: kết nối có thể mất **giữa chừng** một giao dịch. Một lượt quét đi qua nhiều bước và nhiều đường mạng, mỗi điểm đứt tạo ra một tình huống khác nhau:

| Điểm đứt | Tình huống | Rủi ro nếu không xử lý |
| --- | --- | --- |
| P1 | Kiosk gửi `POST /kiosk/v1/scans` nhưng không nhận được phản hồi từ Edge | Kiosk không biết Edge đã xử lý chưa; quét lại có thể tạo 2 bản ghi hoặc mở cổng 2 lần |
| P2 | Edge đã xác thực QR, đã giữ chỗ token trong Redis, rồi mất Redis hoặc crash | Token bị kẹt ở RESERVED, nhân viên không quét lại được |
| P3 | Edge gửi lệnh mở cổng tới Turnstile, không nhận được phản hồi | Không biết cổng đã mở hay chưa; nếu gửi lại thì mở 2 lần; nếu không ghi thì mất công |
| P4 | Turnstile đã mở, Edge commit DB thất bại hoặc crash trước commit | Người đã đi qua nhưng không có bản ghi: mất dữ liệu chấm công |
| P5 | DB đã commit, Edge trả kết quả về kiosk nhưng kiosk không nhận được | Kiosk hiển thị lỗi dù đã thành công; nhân viên quét lại |
| P6 | Đứt LAN giữa kiosk và phòng server trước khi quét | Đã có ở M1 (lớp 3, mục 14.5) |
| P7 | Đứt WAN giữa Edge và Central Site | Đã có ở M1 (lớp 1, mục 14.5) |

M1 chỉ mô tả P6, P7 và nói chung về trạng thái `UNKNOWN` cho P3. Các điểm P1, P2, P4, P5 chưa có quy tắc phục hồi rõ ràng, và chưa có bằng chứng (test) cho tiêu chí "không mất, không trùng".

## Quyết định (Decision)

Áp dụng mô hình **offline-first, store-and-forward** với một **máy trạng thái tường minh** cho mỗi lượt quét, và một **khóa idempotency duy nhất** đi xuyên suốt mọi thành phần.

### 1. Một khóa duy nhất cho cả lượt quét

Kiosk sinh `attempt_id` (UUID v7) ngay khi đọc được QR. Khóa này được dùng làm:

- `Idempotency-Key` của `POST /kiosk/v1/scans` (kiosk → Edge),
- `event_id` của bản ghi attendance và outbox (Edge DB),
- `Idempotency-Key` của `POST /gates/{id}/open` (Edge → Turnstile),
- `eventId` trong AttendanceEvents (Broker → consumer).

Mọi thành phần khi nhận lại cùng khóa phải trả về **kết quả đã có**, không xử lý lại.

### 2. Kiosk ghi journal trước khi gọi mạng (write-ahead)

Kiosk App thay "Offline Queue" của M1 bằng **Kiosk Journal**: một nhật ký append-only (SQLite mã hóa) ghi **mọi** lượt quét, kể cả khi đang online, **trước** khi gọi Edge. Mỗi bản ghi journal có `attempt_id`, `journal_seq` tăng dần, token QR đã băm, `gate_id`, `scanned_at`, và `local_status`.

Quy tắc: kiosk chỉ coi một lượt là "đã bàn giao" khi Edge trả về kết quả cuối cùng cho `attempt_id` đó. Mọi lượt chưa bàn giao được gửi lại qua `POST /kiosk/v1/scans/offline-batch` khi kết nối phục hồi, theo thứ tự `journal_seq`.

### 3. Máy trạng thái của một lượt quét (AccessAttempt)

```
RECEIVED ──► VALIDATED ──► RESERVED ──► GATE_COMMANDED ──┬─► GATE_OPENED ──► COMMITTED ──► PUBLISHED
    │             │             │                        ├─► GATE_FAILED  ──► COMMITTED ──► PUBLISHED
    │             │             │                        └─► GATE_UNKNOWN ─► (reconcile) ─► GATE_OPENED | GATE_FAILED
    └─► DENIED ◄──┴─────────────┘
```

| Trạng thái | Ai ghi | Bền vững ở đâu | Ý nghĩa |
| --- | --- | --- | --- |
| RECEIVED | Kiosk | Kiosk Journal | Đã đọc QR, đã sinh `attempt_id` |
| VALIDATED | Edge | Edge DB (bảng `access_attempts`) | Chữ ký, hạn, thu hồi, trạng thái nhân viên, chính sách cổng đều hợp lệ |
| RESERVED | Edge | Redis (`SET NX EX`) + Edge DB | Token `jti` đã được giữ chỗ; anti-passback đã được giữ chỗ |
| GATE_COMMANDED | Edge | Edge DB | Đã gửi lệnh mở cổng (ghi **trước** khi gửi) |
| GATE_OPENED / GATE_FAILED / GATE_UNKNOWN | Edge | Edge DB | Kết quả chuẩn hóa từ Turnstile Adapter |
| COMMITTED | Edge | Edge DB + Outbox (cùng transaction) | Bản ghi chấm công chính thức, token chuyển USED |
| PUBLISHED | Outbox Publisher | Edge DB (publish_status) | Broker đã ACK |
| DENIED | Edge | Edge DB | Từ chối, kèm `reason_code` |

Nguyên tắc: **ghi trạng thái trước khi thực hiện hành động có tác động ra ngoài** (gửi lệnh cổng, gửi broker). Nhờ đó khi crash, hệ thống luôn biết mình đang ở bước nào.

### 4. Quy tắc phục hồi tại từng điểm đứt

| Điểm đứt | Quy tắc phục hồi | Kết quả bảo đảm |
| --- | --- | --- |
| P1 | Kiosk gửi lại cùng `attempt_id`. Edge tra `access_attempts`: nếu đã có kết quả cuối thì trả lại, nếu đang xử lý thì trả `IN_PROGRESS` và kiosk chờ, nếu chưa có thì xử lý mới | Không trùng bản ghi, không mở cổng 2 lần |
| P2 | Khóa RESERVED trong Redis có TTL ngắn (đề xuất 10 giây). Job phục hồi của Edge quét các bản ghi ở RESERVED quá TTL và chuyển về DENIED với `reason_code = SYSTEM_RETRY`; token được giải phóng để quét lại | Token không bị kẹt |
| P3 | Bản ghi đã ở GATE_COMMANDED. Adapter trả timeout → ghi GATE_UNKNOWN, **không gửi lại lệnh mở**. Job reconcile gọi `GET /gates/{id}/status` hoặc nhận webhook `PASS_COMPLETED`; nếu xác nhận đã mở → GATE_OPENED → COMMITTED; nếu xác nhận chưa mở → GATE_FAILED, giải phóng token | Không mở cổng 2 lần, không mất công của người đã đi qua |
| P4 | Vì GATE_COMMANDED đã bền vững trước khi gửi lệnh, sau khi Edge khởi động lại, job phục hồi coi mọi bản ghi GATE_COMMANDED không có kết quả như GATE_UNKNOWN và chạy reconcile như P3 | Không mất bản ghi |
| P5 | Kiosk còn lượt chưa bàn giao trong journal → gửi lại `attempt_id` → Edge trả COMMITTED đã có → kiosk đóng lượt | Kiosk hiển thị đúng, không tạo bản ghi mới |
| P6 | Giữ như M1: Offline Token Verifier + cache thu hồi + mở cổng qua Local Gate Link, ghi journal với `local_status = OFFLINE_OPENED`. Bổ sung: kiosk giữ **replay cache cục bộ** (`jti` đã thấy trong 2 phút gần nhất) theo ADR-12 | Không mất bản ghi khi LAN đứt |
| P7 | Giữ như M1: Outbox tại Edge DB, Publisher đẩy lại khi WAN phục hồi, consumer khử trùng theo `messageId` | Không mất event |

### 5. Khi Edge nhận batch offline

`IngestOfflineBatchUseCase` xử lý từng bản ghi theo `journal_seq`:

1. Nếu `attempt_id` đã tồn tại → bỏ qua, trả `DUPLICATE`.
2. Nếu token `jti` đã được dùng ở kiosk khác → ghi bản ghi với cờ `anomaly = TOKEN_REUSED_OFFLINE`, không tính công, cảnh báo Security Guard.
3. Kiểm tra lại anti-passback theo thời điểm `scanned_at`; nếu vi phạm → ghi cờ `anomaly = ANTI_PASSBACK_OFFLINE`, chuyển HR Staff xử lý ở UC-09.
4. Hợp lệ → ghi COMMITTED với `source = OFFLINE`, outbox trong cùng transaction.

Edge **không bao giờ** gửi lệnh mở cổng khi nhập batch offline (cổng đã mở lúc đó rồi).

## Phương án đã cân nhắc

| Phương án | Lý do không chọn |
| --- | --- |
| Giữ nguyên M1 (chỉ offline queue khi kiosk đã mất mạng) | Không xử lý P1 đến P5; không có bằng chứng không mất, không trùng |
| Hai pha (2PC) giữa Edge DB, Redis và Turnstile | Turnstile là hệ thống ngoài không hỗ trợ; tăng độ trễ, vi phạm ngân sách 2 giây |
| Saga với bù trừ (đóng cổng lại khi commit thất bại) | Không thể "bù" một người đã đi qua cổng; sai về nghiệp vụ |
| Kiosk tự quyết định hoàn toàn (không cần Edge khi online) | Mất kiểm soát anti-passback và replay giữa các kiosk; chỉ dùng làm đường lùi (P6) |
| Máy trạng thái + write-ahead + idempotency xuyên suốt + reconcile | **Chọn**: phù hợp với ADR-04, ADR-05 đã có, không cần transaction phân tán |

## Hệ quả (Consequences)

(+) Mỗi điểm đứt có quy tắc phục hồi rõ, có thể kiểm thử tự động.
(+) Không mất bản ghi: mọi lượt được ghi bền vững ở kiosk trước khi rời kiosk, và ở Edge trước khi gửi lệnh ra ngoài.
(+) Không trùng bản ghi: một khóa duy nhất, mọi thành phần idempotent.
(+) Không mở cổng hai lần: không bao giờ gửi lại lệnh mở khi kết quả chưa xác định.
(–) Thêm bảng `access_attempts`, job phục hồi và job reconcile ở Edge.
(–) Trạng thái GATE_UNKNOWN có thể tồn tại vài giây đến vài phút; báo cáo phải hiển thị "chưa đối soát".
(–) Phụ thuộc Turnstile có API `status` hoặc webhook để reconcile; nếu không có, GATE_UNKNOWN chuyển cho bảo vệ xác minh (UC-06).

## Kiểm chứng tuân thủ (Compliance)

Các test bắt buộc trong Task 2 (dùng `SimulatedTurnstileAdapter` và fault injection):

1. Gửi cùng `attempt_id` hai lần → đúng 1 bản ghi, đúng 1 lệnh mở cổng.
2. Turnstile timeout → bản ghi GATE_UNKNOWN, **0** lệnh mở lại; reconcile trả OPENED → COMMITTED.
3. Crash giả lập sau GATE_COMMANDED, khởi động lại → job phục hồi đưa về COMMITTED hoặc GATE_FAILED, không mất bản ghi.
4. Broker không khả dụng sau commit → outbox giữ PENDING, publish lại thành công sau khi broker phục hồi.
5. Batch offline có bản ghi trùng và token dùng ở hai kiosk → mỗi `attempt_id` một bản ghi, bản ghi trùng token được gắn cờ anomaly.
6. Khóa RESERVED quá TTL → được giải phóng, token quét lại được.

## Vị trí trong kiến trúc

| View | Thay đổi |
| --- | --- |
| Module view | Thêm `AccessAttempt` (domain, máy trạng thái), `RecordScanUseCase`, `IngestOfflineBatchUseCase`, `ReconcileUnknownUseCase`; port `IAttemptRepository`, `ITokenReservation`, `ITurnstileControlPort`, `IOutboxWriter` |
| C&C view | Kiosk Journal thay Offline Queue; thêm Recovery Job và Reconcile Job bên trong Edge; `ITurnstileControlPort` thêm `checkGateHealth` dùng cho reconcile |
| Deployment view | Kiosk Journal trên SQLite mã hóa tại kiosk; `access_attempts` trên PostgreSQL Edge primary (sync replica sang Node B); Redis giữ RESERVED với TTL |
| API | `POST /kiosk/v1/scans` thêm `journal_seq`; response thêm trạng thái `IN_PROGRESS`; `offline-batch` trả kết quả từng bản ghi (`ACCEPTED`, `DUPLICATE`, `ANOMALY`) |

## Liên kết

AC-02, AC-03, AC-10 · ADR-04, ADR-05, ADR-06 · NFR-07, NFR-09 · UC-08, UC-06 · ADR-12 (replay cache cục bộ), ADR-13 (thành phần điều phối)
