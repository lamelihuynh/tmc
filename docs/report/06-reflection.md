# Phản ánh và Bài học Kiến trúc (Task 1.4)

> Phần này trả lời yêu cầu **Task 1.4** của đề bài M2: đúc kết những bài học thực tế, đánh đổi và thay đổi nhận thức kiến trúc của các thành viên trong quá trình chuyển từ thiết kế M1 sang hiện thực và kiểm chứng ở M2. Mỗi team viết một đoạn; Team D hợp nhất tại D-12.

---

## Phản ánh của Team C (User/QR và Identity)

Khi bước từ tài liệu kiến trúc M1 sang hiện thực và kiểm thử hệ thống bảo mật QR động + định danh ở M2, bài học lớn nhất của Team C là **một "đường lùi tiện lợi" tưởng chừng vô hại trong đặc tả có thể phá vỡ toàn bộ mô hình bảo mật của hệ thống**. Ở M1, chúng tôi đã thiết kế JWS `ES256` 30 giây và chống replay bằng Redis, nhưng vẫn giữ luồng thay thế 1.1 trong UC-07 cho phép gửi QR tĩnh qua email có hạn cả ngày để phòng trường hợp điện thoại mất mạng. Khi đặt toàn bộ luồng lên bàn cân mô hình đe dọa (ADR-12, C-10), chúng tôi nhận ra chính đường lùi đó biến mọi biện pháp token ngắn hạn thành vô nghĩa. Giải pháp triệt để không phải là thêm rào cản tại kiosk, mà là đảo ngược quyền ký: đưa khóa riêng bất đối xứng xuống điện thoại để ứng dụng tự ký QR ngay cả khi ngoại tuyến, xóa bỏ QR qua email và chỉ giữ mở cổng thủ công có audit của bảo vệ (UC-06) làm phương án dự phòng.

Bài học thứ hai đến từ **ranh giới trách nhiệm giữa các service khi xử lý lỗi thực tế**. Ban đầu ADR-12 đặt việc lưu vết token đã dùng (`ITokenReplayStore`) tại `user-qr`. Khi triển khai nối với luồng cổng của `gate-station`, chúng tôi phát hiện nếu `user-qr` đánh dấu token "đã dùng" ngay lúc xác thực chữ ký thì khi cổng xoay bị kẹt hoặc mất kết nối sau đó, nhân viên sẽ bị kẹt lại với thông báo lỗi sai rằng QR đã dùng rồi. Tách `VerifyQrTokenUseCase` thành hàm xác thực thuần túy chỉ-đọc và trao quyền giữ chỗ nguyên tử `jti` cho `ITokenReservation` của `gate-station` (chỉ khóa vĩnh viễn khi lệnh mở cổng đã đi, và nhả ngay khi phần cứng lỗi trước lúc mở) là điểm chỉnh sửa kiến trúc quan trọng giúp vừa giữ được tính an toàn, vừa không làm hỏng trải nghiệm qua cổng.

Cuối cùng, việc áp dụng **kiến trúc lục giác kèm kiểm thử hợp đồng và fixture dùng chung** (`qr-token-policy.json`, `employee-seed.json`, `TestJwtKeys`, `ArchUnit`) cho phép hai thành viên C1 và C2 làm việc song song hiệu quả mà không xung đột hay phải chờ các team khác. C1 có thể hoàn thiện cấp phát/xoay khóa JWT và outbox bền cho `identity` & `user-qr`, trong khi C2 triển khai độc lập xử lý tranh chấp đăng ký thiết bị song song `409`, danh bạ HR bền trên PostgreSQL và bộ xác thực TOTP RFC 6238 mà vẫn giữ nguyên `AuthenticateUseCase` và mọi hợp đồng HTTP/AsyncAPI với Team A và Team D. Qua đó, chúng tôi cũng nhìn thấy rõ ranh giới giữa cái đã chứng minh bằng test thật (xác thực ngoại tuyến của token, xoay khóa JWKS, transaction rollback khi xung đột) với những phần còn phụ thuộc hạ tầng triển khai thực tế như TLS/mTLS, mã hóa lưu trữ AES-256 và thu hồi phiên JWT tức thời.

---

## Phản ánh của Team B (`services/attendance`)

Khi bắt tay hiện thực module sổ cái `attendance` theo ADR-11 và ADR-15, bài học lớn nhất của Team B là **tính nguyên tử và thứ tự ghi quan trọng hơn mọi cơ chế thử lại ở tầng mạng**. Ở bản M1, "lưu local rồi đẩy lại" được hiểu chung ở mức ý tưởng, nhưng khi viết test mô phỏng crash sau lệnh cổng và dừng/bật lại RabbitMQ thật, chúng tôi thấy rõ chỉ một kẽ hở nhỏ — như ghi `GATE_COMMANDED` sau khi gọi Turnstile, hoặc đánh dấu `PUBLISHED` trước khi nhận publisher confirm — đều có thể tạo ra lỗi mất công hoặc mở cổng hai lần không thể sửa được bằng retry. Việc đưa toàn bộ chuyển trạng thái vào aggregate `AccessAttempt`, tách `GateResultApplier` dùng chung cho cả luồng online lẫn luồng đối soát nền, và ép mọi adapter (`InMemory` lẫn `JDBC`/`RabbitMQ`) chạy qua cùng hợp đồng đã giúp phát hiện sớm các sai lệch giữa giả lập trong bộ nhớ và hạ tầng thật (ví dụ hành vi rollback transaction PostgreSQL trong `PersistenceAndBrokerIT` và ngắt kết nối RabbitMQ giữa chừng trong `OutboxBrokerRestartIT`). Đổi lại, kiến trúc tách port và hai bộ adapter đòi hỏi kỷ luật cập nhật đồng bộ cả `InMemory` và `JDBC` mỗi khi thêm truy vấn mới (như hàng đợi bất thường B-03 hay chỉ số đếm B-08) và luôn phải giữ tài liệu/sơ đồ đồng bộ với mã thực tế thay vì để hai bên trôi xa nhau.

---

## Phần chờ các team khác (D-12 hợp nhất)

- **Phản ánh của Team A:** *[Chờ Team A]*
- **Phản ánh của Team D:** *[Chờ Team D]*
