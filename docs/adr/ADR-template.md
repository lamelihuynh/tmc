# ADR-NN: <Tên quyết định, nêu bằng một câu>

| Trạng thái | Proposed / Accepted / Superseded by ADR-XX / Deprecated |
| --- | --- |
| Ngày | dd/mm/yyyy |
| Người đề xuất | <team, tên> |
| Người duyệt | <ít nhất lead của các team bị ảnh hưởng> |
| Vấn đề giải quyết | <yêu cầu, lỗi, hoặc "Issue Encountered" nào> |
| Thay thế / mở rộng | <ADR cũ nếu có> |

## Bối cảnh (Context)

Lực nào đang tác động? Sự thật nào ta biết (kèm số liệu, tham chiếu FR/NFR/UC, mục M1)? Điều gì sẽ hỏng nếu không quyết định? Viết cho người chưa dự cuộc họp.

## Quyết định (Decision)

Chọn gì, nói thẳng, ở thể chủ động ("Chúng ta sẽ ..."). Gồm quy tắc cụ thể mà mã phải tuân theo. Nếu có sơ đồ hoặc bảng, đặt ở đây.

## Phương án đã cân nhắc

| Phương án | Vì sao không chọn |
| --- | --- |
| ... | ... |
| **Phương án chọn** | **Chọn vì ...** |

## Hệ quả (Consequences)

(+) Lợi ích.
(–) Cái giá phải trả, rủi ro còn lại, việc phải làm thêm. **Mục này không được để trống:** quyết định nào cũng có đánh đổi.

## Kiểm chứng tuân thủ (Compliance)

Làm sao biết mã đang theo quyết định này? Liệt kê test, luật ArchUnit, kịch bản demo cụ thể (tên lớp test, tên lệnh). Chỗ nào chưa kiểm chứng được thì nói rõ.

## Vị trí trong kiến trúc

| View | Thay đổi |
| --- | --- |
| Module view | ... |
| Component and connector view | ... |
| Deployment view | ... |
| API / sự kiện | ... |

## Liên kết

AC-xx, ADR-xx, FR/NFR/UC liên quan, mục báo cáo, story trong `docs/team/WORK-BREAKDOWN.md`.

---

### Cách dùng

1. Sao chép tệp thành `docs/adr/ADR-NN-ten-ngan.md` (NN là số kế tiếp trong `ADR-00-index.md`).
2. Mở PR `docs(adr): propose ADR-NN ...` với trạng thái **Proposed**. Gắn nhãn `adr`.
3. Thảo luận trong PR. Chỉ chuyển sang **Accepted** khi các lead bị ảnh hưởng đã duyệt.
4. Thêm một dòng vào `ADR-00-index.md` trong cùng PR.
5. ADR đã Accepted **không sửa nội dung**; muốn đổi thì viết ADR mới và đánh dấu cái cũ là `Superseded`. (Sửa lỗi chính tả được.)
