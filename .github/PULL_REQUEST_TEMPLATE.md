<!-- Tiêu đề PR = thông điệp commit cuối (squash). Ví dụ: feat(gate-station): refresh anti-passback after offline ingest -->

## Vì sao

<!-- Vấn đề hoặc yêu cầu nào? 1 đến 3 câu. -->

## Liên kết

- Story / issue: <!-- ví dụ A-02, closes #12 -->
- ADR / yêu cầu liên quan: <!-- ví dụ ADR-11, FR-18, NFR-09 -->

## Thay đổi gì

<!-- Các điểm chính, theo ý chứ không theo từng tệp. -->

## Ảnh hưởng

- [ ] Đổi hợp đồng (API, sự kiện, port): đã sửa `docs/api` trong PR này và thêm reviewer của bên dùng
- [ ] Đổi hành vi so với ADR hoặc báo cáo: đã sửa `docs/architecture/implementation-notes.md`
- [ ] Cần ADR mới: <!-- link, hoặc "không" -->
- [ ] View kiến trúc bị ảnh hưởng: module / component / deployment / không

## Đã kiểm tra

<!-- Dán lệnh và kết quả thật, không ghi "đã test". -->

```
make format && make test && make arch-test
```

- [ ] Đã chạy `make demo-online` / `demo-offline` / `demo-timeout` (nếu đụng luồng quét, phục hồi, xác thực QR): <!-- kết quả -->
- [ ] Test mới: <!-- tên lớp test; với lỗi: test đỏ trước khi sửa -->
- [ ] Chưa kiểm chứng được: <!-- ví dụ cần Docker. Ghi rõ, đừng bỏ trống -->

## Checklist tác giả

- [ ] Theo `docs/conventions/coding-standards.md`; `ArchitectureTest` xanh
- [ ] Không có dữ liệu cá nhân, token thô, `jti`, bí mật trong mã, log, sự kiện
- [ ] Fail-secure: không có đường nào mở cổng khi không chắc chắn; không gửi lại lệnh vật lý khi chưa rõ kết quả
- [ ] PR nhỏ, một ý, không kèm sửa định dạng không liên quan

<!-- Người duyệt: dùng docs/conventions/pr-review-checklist.md -->
