# Checklist duyệt Pull Request

## Cho tác giả (trước khi bấm "Ready for review")

- [ ] Tiêu đề PR theo Conventional Commits (`docs/conventions/commit-convention.md`); PR được squash nên tiêu đề là thông điệp cuối
- [ ] Mô tả nêu **vì sao**, liên kết story (ví dụ `A-02`) và ADR liên quan
- [ ] Đã chạy và ghi lại trong mô tả: `make format`, `make test`, và `make arch-test` (và `make demo-*` nếu đụng luồng quét)
- [ ] Test mới đi cùng mã; test hồi quy cho lỗi đã sửa
- [ ] Nếu đổi hợp đồng: `docs/api/*.yaml` đổi trong **cùng PR**, đã thêm reviewer của bên dùng
- [ ] Nếu có quyết định kiến trúc mới: có ADR
- [ ] Nếu đổi hành vi so với ADR hoặc báo cáo: đã sửa `docs/architecture/implementation-notes.md`
- [ ] PR nhỏ (dưới khoảng 400 dòng thay đổi, không tính tệp sinh ra). Lớn hơn thì tách
- [ ] Không có bí mật, dữ liệu cá nhân thật, tệp sinh ra, mã bị comment

## Cho người duyệt

Cần **2 người duyệt**: một người cùng team (không phải tác giả) và một người **khác team** nếu PR chạm hợp đồng (API, sự kiện, port) hoặc thư mục dùng chung. Chủ sở hữu thư mục trong `CODEOWNERS` được yêu cầu tự động.

### Đúng đắn và an toàn

- [ ] Mã làm đúng điều mô tả và tiêu chí của story
- [ ] Mọi nhánh lỗi được xử lý: cái gì xảy ra khi mạng đứt, timeout, trùng lặp, thứ tự đảo ngược?
- [ ] **Không có đường nào mở cổng khi không chắc chắn** (fail-secure). Không gửi lại lệnh vật lý khi chưa rõ kết quả
- [ ] Lệnh ghi idempotent theo khóa ổn định
- [ ] Quyền được kiểm trong use case (`Rbac`), phạm vi dữ liệu (nhà máy, cổng, nhóm) được kiểm, không chỉ vai trò
- [ ] Không có dữ liệu cá nhân, token thô, `jti` trong log, sự kiện, thông báo lỗi
- [ ] Ngoại lệ không bị nuốt âm thầm; chỗ cố ý nuốt có chú thích vì sao là an toàn

### Kiến trúc

- [ ] `ArchitectureTest` vẫn xanh; không thêm phụ thuộc ngược chiều
- [ ] Không có chú thích Spring trong `domain` hoặc `application`
- [ ] Port do tầng dùng nó sở hữu; adapter đặt đúng thư mục
- [ ] Không import mã hoặc đọc dữ liệu của service khác
- [ ] Việc cấu hình nằm ở `bootstrap`, không rải trong adapter
- [ ] Mở rộng bằng cách thêm lớp (OCP), không thêm nhánh `if` theo loại vào lớp cũ

### Chất lượng

- [ ] Test đọc như yêu cầu, tên dạng `should_..._when_...`, khẳng định cả điều không được xảy ra
- [ ] Không có `Thread.sleep`, giờ hệ thống, thứ tự phụ thuộc trong test
- [ ] `@Disabled` mới (nếu có) đúng dạng `TODO Team X (tham chiếu): tiêu chí`
- [ ] Tên rõ nghĩa, chú thích nói vì sao, không có mã chết
- [ ] Tài liệu liên quan đã cập nhật (runbook, API, README của thư mục)

### Khi duyệt

- Nhận xét nói rõ: **chặn** (phải sửa), **gợi ý** (có thể bỏ qua), hay **câu hỏi**. Dùng tiền tố `blocking:`, `nit:`, `question:`
- Duyệt trong vòng **một ngày làm việc**. Nếu không kịp, nói rõ để PR được chuyển người
- Tác giả không tự hợp nhất khi còn nhận xét `blocking:` chưa giải quyết
- Người duyệt chạy thử mã khi PR chạm luồng quét hoặc phục hồi (`git fetch && git checkout`, rồi `make demo-*`)

## Tiêu chí hợp nhất

1. CI xanh (build, test, kiến trúc, `spotlessCheck`, portal).
2. Đủ 2 người duyệt như trên, không còn nhận xét `blocking:`.
3. Nhánh đã rebase lên `main` mới nhất, không xung đột.
4. Tác giả hợp nhất bằng **Squash and merge**.
