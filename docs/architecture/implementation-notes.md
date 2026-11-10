# Ghi chú hiện thực: chỗ mã khác ADR và các khoảng trống đã biết

Tài liệu này trả lời câu hỏi "mã đang làm gì khác với ADR và báo cáo M1, và vì sao". **Báo cáo cuối phải khớp với mã, nên mọi chỗ khác biệt ở đây hoặc phải sửa mã, hoặc phải sửa ADR/báo cáo.** Mỗi mục có người nhận việc trong `docs/team/WORK-BREAKDOWN.md`.

## 1. Khác biệt có chủ đích so với ADR

| # | ADR nói | Mã làm | Lý do | Cần làm với báo cáo |
| --- | --- | --- | --- | --- |
| 1 | ADR-11: `PUBLISHED` là trạng thái của lượt quét | `PUBLISHED` là `publish_status` của hàng outbox, **không** phải `AttemptStatus` | Publisher chạy nền không nên ghi vào aggregate; M1 mục 12.7 cũng tách `PENDING_GATE` khỏi `PENDING`/`PUBLISHED` của outbox | Sửa sơ đồ trạng thái ADR-11 cho khớp (`state-machine.md` đã vẽ đúng theo mã) |
| 2 | ADR-11: `GATE_FAILED → COMMITTED → PUBLISHED` | `GATE_FAILED` là trạng thái **cuối**; không có `COMMITTED` cho lượt lỗi cổng. Sự kiện `GateResultRecorded` vẫn được phát | M1 bảng 16.3.2: lỗi cổng "không tính là đã vào/ra"; gọi nó `COMMITTED` dễ nhầm với công đã ghi | Sửa sơ đồ ADR-11 |
| 3 | ADR-12 và ADR-13: `ITokenReplayStore` thuộc User/QR | Kiểm tra replay một lần dùng nằm ở `ITokenReservation` của **Gate Station** (Redis `SET NX`); `VerifyQrTokenUseCase` chỉ đọc, không đánh dấu đã dùng | Cần nhả token khi cổng lỗi (M1 bảng 16.3.2). Nếu verify đánh dấu "đã dùng" thì không nhả được | Sửa bảng port ở ADR-13, bỏ `ITokenReplayStore` khỏi `user-qr` |
| 4 | ADR-12 test 2: token của thiết bị bị thu hồi trả `QR_REVOKED` | Trả `DEVICE_REVOKED` | ADR-12 mục API đã thêm `DEVICE_REVOKED`; hai chỗ trong ADR mâu thuẫn nhau. `QR_REVOKED` dành cho QR bị thu hồi theo nhân viên | Sửa ADR-12 test 2 (đã sửa trong repo) |
| 5 | ADR-12: token tối đa theo cấu hình, không nêu giới hạn | `TokenVerificationPolicy` từ chối token có `exp - iat > 60 s` (`MALFORMED_TOKEN`) | Chặn token sống lâu giống QR tĩnh, kể cả khi app bị sửa. Kịch bản demo `--long-lived` | Thêm vào ADR-12 mục "Quyết định" |
| 6 | ADR-13: QR Verifier là instance chỉ-đọc của User/QR đặt tại Edge | Hiện là endpoint `/internal/v1/qr/verify` của chính service `user-qr` | Chưa có triển khai Edge riêng cho `user-qr`. Hợp đồng không đổi nên chuyển sang Edge sau này không ảnh hưởng Gate Station | Ghi rõ trong mục triển khai: "mô hình hóa như instance Edge, chạy cùng tiến trình khi demo" |
| 7 | M1 mục 12.6: mở thủ công không tự thành công hợp lệ | `GateResultApplier` commit lượt `MANUAL` nhưng **không** tạo `AttendanceTransaction` và không phát `AttendanceConfirmed`; chỉ phát `AuditRecorded` | Khớp M1. Việc xác minh bởi HR là story B-07 | Giữ nguyên, mô tả luồng xác minh |

## 2. Quyết định kỹ thuật bổ sung (chưa có ADR riêng)

| Chủ đề | Quyết định | Nơi trong mã |
| --- | --- | --- |
| Không lưu token thô | Journal kiosk chỉ lưu SHA-256 của token. Khi gửi lại lượt chưa có kết quả, kiosk gửi **hash** thay token. Nếu Edge đã biết `attempt_id` thì trả kết quả đã lưu, nếu chưa thì từ chối `MALFORMED_TOKEN` và nhân viên quét lại | `KioskApp.flush`, `RecordScanUseCase` (bước tra `find` đầu tiên) |
| `Idempotency-Key` header = `attempt_id` | Cùng một UUID v7 đi suốt: kiosk, Edge, Turnstile (`cmd-ts-<attempt_id>`), Outbox | `KioskController`, `HttpTurnstileAdapter` |
| Thời hạn giữ chỗ | Token: giữ 10 giây khi đang gửi lệnh; sau `OPENED` hoặc `TIMEOUT` chặn đến `exp + 60 s`. Anti-passback pending: 60 giây | `ScanSettings` |
| Lỗi kết nối sớm với Turnstile | Retry **một lần** cùng khóa idempotency rồi coi là `TIMEOUT`. Chỉ lỗi kết nối được retry, chậm trả lời thì không | `HttpTurnstileAdapter` |
| Khi Attendance không lưu được trước lệnh cổng | Nhả token và anti-passback, ném lỗi, **không** gửi lệnh mở. Hàng `RESERVED` còn lại được `RecoverStaleAttemptsUseCase` giải phóng | `RecordScanUseCase` bước 4 |
| Khi cổng mở nhưng không ghi được kết quả | Trả `UNCONFIRMED`; hàng ở `GATE_COMMANDED` sẽ được phục hồi sang `GATE_UNKNOWN` rồi đối soát | `RecordScanUseCase.onOpened` |
| Kiosk hỏi lại | Khi Edge trả `IN_PROGRESS`, kiosk hỏi lại cùng `attempt_id` tối đa 6 lần, mỗi lần chờ 300 ms, rồi mới kết luận `PENDING`. **Không** mở cổng cục bộ khi Edge đang giữ lượt | `KioskApp.scan` |
| Thứ tự xác thực QR | Cấu trúc và PII, thiết bị, trạng thái thiết bị, chủ khóa phải là chủ token, chữ ký, rồi mới đến thời gian | `VerifyQrTokenUseCase` |

## 3. Khoảng trống: anti-passback sau khi nhập offline  (Team A: A-02, Team B: B-03)

`IngestOfflineBatchUseCase` ghi lượt vào Attendance và kiểm tra anti-passback **trong Attendance**, nhưng **không cập nhật `IAntiPassbackStore` của Gate Station**. Hệ quả: nhân viên vào offline rồi vào lại online ngay sau khi flush sẽ không bị chặn ở lượt thứ hai. Đây là lỗ hổng thật, không phải nhược điểm lý thuyết.

Hướng giải quyết đề xuất: sau khi `ingestOffline` trả `ACCEPTED`, Gate Station cập nhật `last` cho từng nhân viên theo `scannedAt` mới nhất của lô.

## 4. Khoảng trống: trạng thái lạnh và danh bạ nhân viên  (Team A: A-03, Team C: C-02)

- **Redis mất dữ liệu** thì `last` của anti-passback biến mất, nhân viên đã vào có thể vào lần nữa. Cần dựng lại từ `IAttendanceQuery.lastDirection` (B-05, A-03). Compose đã bật AOF để giảm khả năng này, không loại bỏ nó.
- **`IEmployeeDirectory`** đọc từ cấu hình (`application.yml`). Thực tế phải đến từ gói đồng bộ của User/QR (C-02). Trong lúc đó, `ActiveEmployeeRule` chỉ đúng với dữ liệu seed.
- Cache nhân viên đủ quyền **không có ở kiosk**: kiểm tra offline chỉ dựa vào khóa thiết bị, chữ ký, thời gian và replay. Nhân viên đã nghỉ việc nhưng thiết bị chưa bị thu hồi vẫn qua được khi kiosk offline, đến lúc đồng bộ sẽ bị gắn bất thường. M1 mục 10.4 đã thừa nhận giới hạn "không cam kết thu hồi tức thời trên thiết bị mất mạng"; phải nêu rõ trong báo cáo.

## 5. Khoảng trống nhỏ  (Team A: A-06, Team B: B-04)

- Khi trả lời một lượt **gửi lại**, `ScanResult.direction` là `null` vì `AttemptView` chưa mang chiều. Kiosk vẫn đúng nhưng không hiển thị chiều. Sửa bằng B-04 và A-06.
- `ReasonCode.GATE_DIRECTION_MISMATCH` chưa có rule nào dùng; chỉ dùng trong test mở rộng chính sách.

## 6. Những gì chưa được kiểm chứng

| Hạng mục | Vì sao | Cách kiểm chứng | Story |
| --- | --- | --- | --- |
| Adapter JDBC, Flyway, Outbox trên PostgreSQL | Máy tạo scaffold không chạy được Docker | `make up && make integration` | D-01, B-01, C-01 |
| `AmqpEventPublisher` và các `@RabbitListener` | Cần RabbitMQ | Test tích hợp AMQP | B-02, D-03 |
| `RedisTokenReservation`, `RedisAntiPassbackStore` | Cần Redis | Test tích hợp | A-01 |
| Mọi kiểm thử hiệu năng (NFR-06) | Chưa có thiết lập đo | Kiểm thử tải | A-09 |
| mTLS, TLS 1.2+ | Chưa cấu hình | Tài liệu và cấu hình triển khai | D-11 |

**Đã kiểm chứng:** toàn bộ test đơn vị và kiến trúc (xem bảng ở `WORK-BREAKDOWN.md` mục 2) và 3 kịch bản demo chạy trên 4 tiến trình thật qua HTTP (profile `local`): đường thành công, từ chối replay, token giống QR tĩnh, token chứa tên, anti-passback, offline, và timeout cổng có phục hồi.

## 7. Điều báo cáo không được khẳng định

- Không nói "hệ thống đạt p95 ≤ 1 giây" cho tới khi A-09 có số đo.
- Không nói "không mất bản ghi" trên hạ tầng thật cho tới khi B-01 và B-02 xanh. Có thể nói: "mô hình và các test đơn vị cho thấy mỗi điểm đứt có quy tắc phục hồi".
- Không nói "có MFA" cho tới khi C-05 có TOTP thật.
