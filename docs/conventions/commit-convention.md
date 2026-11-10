# Quy ước commit, nhánh và hợp nhất

## Nhánh

- `main` là nhánh duy nhất sống lâu. **Luôn xanh**, luôn chạy được `make local-up` và 3 demo. Không ai đẩy thẳng lên `main`.
- Nhánh làm việc ngắn (tối đa vài ngày), đặt tên: `<loại>/<team>/<id-story>-<mô-tả-ngắn>`

```
feat/a/A-02-sync-antipassback-after-offline
fix/b/B-01-outbox-publish-order
docs/d/D-12-update-nfr-05
chore/d/D-02-ci-integration-job
```

`<team>` là `a`, `b`, `c` hoặc `d`. `<id-story>` lấy từ `docs/team/WORK-BREAKDOWN.md`. Việc không thuộc story dùng `x`.

## Commit: Conventional Commits

```
<type>(<scope>): <mô tả ngắn, tiếng Anh, thể mệnh lệnh, không dấu chấm cuối>

[nội dung: vì sao, không chỉ là cái gì. Có thể tiếng Việt hoặc tiếng Anh.]

[Refs: A-02, ADR-11]
```

**Dòng đầu tối đa 72 ký tự.** Commit message viết bằng tiếng Anh để công cụ và người ngoài nhóm đọc được; thân commit và mô tả PR có thể tiếng Việt.

### `type`

| type | Dùng khi | Ví dụ |
| --- | --- | --- |
| `feat` | thêm hành vi mới | `feat(gate-station): refresh anti-passback after offline ingest` |
| `fix` | sửa lỗi | `fix(attendance): keep outbox order when a publish fails` |
| `refactor` | đổi cấu trúc, không đổi hành vi | `refactor(user-qr): extract token time policy` |
| `test` | chỉ thêm hoặc sửa test | `test(kiosk-sim): cover in-progress polling` |
| `docs` | chỉ tài liệu | `docs(adr): add ADR-14 for audit hash chain` |
| `build` | Gradle, phụ thuộc | `build(deps): bump spring boot to 3.3.6` |
| `ci` | GitHub Actions | `ci: run integration tests on a separate job` |
| `chore` | việc vặt không đổi mã sản xuất | `chore: ignore local runtime files` |
| `perf` | cải thiện hiệu năng | `perf(gate-station): reuse http client` |

### `scope` (chọn một)

`gate-station`, `attendance`, `user-qr`, `identity`, `reporting`, `hr-integration`, `audit-admin`, `turnstile-sim`, `kiosk-sim`, `device-sim`, `web-portal`, `platform`, `docker`, `scripts`, `adr`, `api`, `team`, `report`.

Thay đổi chạm nhiều scope: dùng scope của phần **chính**, hoặc bỏ scope nếu thật sự xuyên suốt. Nếu cần ghi nhiều scope, bạn đang commit quá to: tách ra.

### Thay đổi phá vỡ hợp đồng

Thêm `!` sau scope và đoạn `BREAKING CHANGE:` ở thân:

```
feat(api)!: rename reasonCode to reason_code in internal attempts

BREAKING CHANGE: attendance and gate-station must be deployed together.
Refs: ADR-15
```

Mọi thay đổi như vậy cần ADR và reviewer của bên dùng hợp đồng.

## Quy tắc commit

1. **Một commit, một ý.** Mã và test cho cùng ý nằm trong cùng commit.
2. Mỗi commit phải build và test được (để `git bisect` có ích).
3. **Không commit:** bí mật, mật khẩu thật, khóa riêng, dữ liệu cá nhân thật, tệp `.db`, `build/`, `node_modules/`. Hồ sơ của `device-simulator` chỉ chứa khóa **giả lập**.
4. Không commit mã bị comment, `System.out.println` gỡ lỗi, `@Disabled` không có `TODO Team X` kèm tiêu chí.
5. Không sửa lịch sử của nhánh đã đẩy và người khác đang dùng.

## Hợp nhất

- **Squash and merge** vào `main`. **Tiêu đề PR là thông điệp commit cuối**, nên PR title cũng phải theo Conventional Commits.
- Xóa nhánh sau khi hợp nhất.
- Cập nhật nhánh bằng `git rebase origin/main` (không tạo commit merge ngược vào nhánh làm việc).
- Hai người cùng sửa một tệp: nói với nhau trước, hoặc tách việc để không đụng tệp.

## Thẻ phát hành

`v0.<tuần>.<n>` khi qua mỗi mốc M-1 đến M-5 trong `WORK-BREAKDOWN.md`. Bản nộp bài gắn thẻ `m2-submission`.
