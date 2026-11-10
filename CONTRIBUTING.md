# Đóng góp vào dự án

Tài liệu này là bản tóm tắt để bắt đầu. Chi tiết từng chủ đề nằm trong `docs/conventions/`. **Khi tài liệu này và mã mâu thuẫn, mở issue**: một trong hai sai.

## Bắt đầu trong 15 phút

1. Cài JDK 21 (xem `docs/runbook/local-setup.md`).
2. `make build` phải xanh trên máy của bạn.
3. `make local-up && make demo-online` để thấy hệ thống chạy.
4. Nhận một issue từ GitHub Projects, hoặc từ `docs/team/WORK-BREAKDOWN.md` (mỗi story có ID, ví dụ `A-02`).

## Quy trình một thay đổi

```
issue ─► nhánh ─► mã + test ─► make format/test/arch-test ─► PR ─► duyệt ─► CI xanh ─► squash merge
```

1. **Nhánh** từ `main` mới nhất: `feat/<team>/<id>-<mô-tả>` (ví dụ `feat/a/A-02-sync-antipassback`). Xem `docs/conventions/commit-convention.md`.
2. **Viết test cùng lúc với mã.** Với lỗi: test đỏ trước, sửa sau.
3. **Chạy cục bộ trước khi đẩy:**
   ```bash
   make format && make test && make arch-test
   make demo-online      # nếu bạn đụng luồng quét, phục hồi hoặc xác thực QR
   ```
4. **Commit** theo Conventional Commits, tiếng Anh, dòng đầu ≤ 72 ký tự: `feat(gate-station): refresh anti-passback after offline ingest`.
5. **Mở PR** bằng mẫu có sẵn. Điền đủ: vì sao, story, ADR, đã chạy gì, ảnh hưởng view nào.
6. **Duyệt:** 2 người (một cùng team, không phải tác giả; thêm một người khác team nếu chạm hợp đồng hoặc thư mục dùng chung). Checklist: `docs/conventions/pr-review-checklist.md`. Trả lời trong một ngày làm việc.
7. **Hợp nhất** bằng *Squash and merge*, khi CI xanh và hết nhận xét `blocking:`. Xóa nhánh.

## Năm điều không được vi phạm

1. **Không phá quy tắc phụ thuộc.** `domain` ← `application` ← `adapter`; không import giữa các service. `ArchitectureTest` đỏ là bạn sai, không phải test sai.
2. **Không mở cổng khi không chắc chắn, không gửi lại lệnh vật lý khi chưa rõ kết quả.** Đây là điều quan trọng nhất của hệ thống (ADR-11).
3. **Không đưa dữ liệu cá nhân, token thô hay `jti` vào log, sự kiện, thông báo lỗi.**
4. **Đổi hợp đồng thì đổi `docs/api` trong cùng PR** và có reviewer của bên dùng. Đổi không tương thích cần ADR.
5. **Mỗi thư mục có một chủ sở hữu** (`CODEOWNERS`). Sửa thư mục của team khác thì mở PR và để họ duyệt.

## Khi nào cần ADR

Khi quyết định (a) khó đảo ngược, (b) ảnh hưởng nhiều hơn một team, hoặc (c) lệch khỏi một ADR đã có. Dùng `docs/adr/ADR-template.md`, mở PR với nhãn `adr`, trạng thái **Proposed**. Thông tin thêm ở `docs/adr/ADR-00-index.md`.

## Định nghĩa "xong"

Xem `docs/team/WORK-BREAKDOWN.md` mục 9. Tóm tắt: mã đúng quy ước, có test, `ArchitectureTest` xanh, hợp đồng và ADR đã cập nhật, PR được duyệt đủ người, tiêu chí của story được chứng minh trong mô tả PR.

## Cấu hình repo (Team D thiết lập một lần)

Trong GitHub, Settings → Branches → rule cho `main`:

- Require a pull request before merging; **2 approvals**; dismiss stale approvals khi có commit mới
- Require review from Code Owners (sau khi đã điền handle thật trong `CODEOWNERS`)
- Require status checks: `build`, `portal` (thêm `integration` sau khi D-01 xong)
- Require branches to be up to date; require conversation resolution
- Chỉ cho phép *Squash merge*; không cho đẩy thẳng, không cho force push
- Tự động xóa nhánh sau khi hợp nhất

Nhãn nên có: `team:A`, `team:B`, `team:C`, `team:D`, `level:B`, `level:N`, `level:T`, `week:W1` đến `week:W4`, `adr`, `bug`, `blocked`.

## Bảo mật

Không commit bí mật hay dữ liệu thật. Khóa trong `device-simulator` là khóa **giả lập** cho phát triển. Phát hiện lỗ hổng: báo riêng cho lead của team sở hữu, không mở issue công khai.
