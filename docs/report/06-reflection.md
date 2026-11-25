# Phản ánh (Task 1.4)

Phần này trả lời yêu cầu **Task 1.4** (mỗi team đóng góp một đoạn phản ánh về quá trình chuyển đổi kiến trúc và hiện thực từ M1 sang M2; Team D hợp nhất ở D-12).

---

## Phản ánh của Team B (`services/attendance`)

Khi bắt tay hiện thực module sổ cái `attendance` theo ADR-11 và ADR-15, bài học lớn nhất của Team B là **tính nguyên tử và thứ tự ghi quan trọng hơn mọi cơ chế thử lại ở tầng mạng**. Ở bản M1, "lưu local rồi đẩy lại" được hiểu chung ở mức ý tưởng, nhưng khi viết test mô phỏng crash sau lệnh cổng và dừng/bật lại RabbitMQ thật, chúng tôi thấy rõ chỉ một kẽ hở nhỏ — như ghi `GATE_COMMANDED` sau khi gọi Turnstile, hoặc đánh dấu `PUBLISHED` trước khi nhận publisher confirm — đều có thể tạo ra lỗi mất công hoặc mở cổng hai lần không thể sửa được bằng retry. Việc đưa toàn bộ chuyển trạng thái vào aggregate `AccessAttempt`, tách `GateResultApplier` dùng chung cho cả luồng online lẫn luồng đối soát nền, và ép mọi adapter (`InMemory` lẫn `JDBC`/`RabbitMQ`) chạy qua cùng hợp đồng đã giúp phát hiện sớm các sai lệch giữa giả lập trong bộ nhớ và hạ tầng thật (ví dụ hành vi rollback transaction PostgreSQL trong `PersistenceAndBrokerIT` và ngắt kết nối RabbitMQ giữa chừng trong `OutboxBrokerRestartIT`). Đổi lại, kiến trúc tách port và hai bộ adapter đòi hỏi kỷ luật cập nhật đồng bộ cả `InMemory` và `JDBC` mỗi khi thêm truy vấn mới (như hàng đợi bất thường B-03 hay chỉ số đếm B-08) và luôn phải giữ tài liệu/sơ đồ đồng bộ với mã thực tế thay vì để hai bên trôi xa nhau.

---

## Phần chờ các team khác (D-12 hợp nhất)

- **Phản ánh của Team A:** *[Chờ Team A]*
- **Phản ánh của Team C:** *[Chờ Team C]*
- **Phản ánh của Team D:** *[Chờ Team D]*

