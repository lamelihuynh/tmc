# Báo cáo M2: khung và theo dõi

Báo cáo nộp = **`docs/document.md` (M1) đã sửa + phần M2**, xuất thành PDF, kèm URL GitHub ở trang đầu (đề bài M2, Submission Instructions). Thư mục này chứa phần **M2 mới** do từng team viết bằng Markdown; Team D hợp nhất (story D-12).

## Quy ước viết

1. Một tệp cho mỗi phần ở bảng dưới, đặt tên đúng như cột "Tệp". Tiếng Việt, thuật ngữ kỹ thuật giữ nguyên tiếng Anh như trong M1.
2. **Mọi khẳng định về hành vi trỏ tới một test hoặc một kịch bản demo** (tên lớp test, hoặc `make demo-*`). Điều mã chưa làm thì đưa vào "Hạn chế", không viết như đã có.
3. Không khẳng định điều chưa đo: xem `docs/architecture/implementation-notes.md` mục 7.
4. Hình dùng Mermaid trong tệp nguồn; Team D xuất ảnh khi hợp nhất. Đánh số hình theo dạng `Hình 12.4.1` như M1.
5. Mỗi phần mở đầu bằng một câu "Phần này trả lời yêu cầu nào của đề".

## Các phần

| Tệp | Phần | Trả lời yêu cầu đề | Team | Căn cứ trong repo | Trạng thái |
| --- | --- | --- | --- | --- | --- |
| `01-issues-connectivity.md` | Issues Encountered: mất kết nối trong lúc xử lý | "Propose architectural enhancements ... explain how it mitigates ... how it should be incorporated" | B (viết), A, D | ADR-11, `state-machine.md`, `Adr11ResilienceTest`, `make demo-timeout`, `make demo-offline` | Chưa viết |
| `02-issues-static-qr.md` | Issues Encountered: QR tĩnh bị lộ | như trên | C (viết), A, D | ADR-12, `Adr12TokenVerificationTest`, `make demo-online`, `privacy-notes.md` | Chưa viết |
| `03-requirements-update.md` | Cập nhật FR-19, NFR-05, NFR-09, UC-07, UC-08, AC-03, AC-04 | Task 1.1 | C (NFR-05, UC-07), B (NFR-09, UC-08) | `docs/adr/README.md` mục 3 | Chưa viết |
| `04-views-update.md` | Component, sequence, deployment đã sửa | Task 1.2 (module, C&C, allocation) | A (quét QR, 15.2), B (outbox), C (bảo mật), D (HR) | `c4-edge.md`, `module-map.md` | Chưa viết |
| `05-solid.md` | Áp dụng SOLID | Task 1.3 | cả bốn | `solid-map.md` | Chưa viết |
| `06-reflection.md` | Phản ánh | Task 1.4 | cả bốn (mỗi team một đoạn), D hợp nhất | | Chưa viết |
| `07-implementation.md` | Hiện thực module đã chọn, cách chạy, kết quả | Task 2 | D | `README.md`, `release-checklist.md` | Chưa viết |
| `08-limitations.md` | Hạn chế và việc tiếp theo | trung thực | D | `implementation-notes.md` mục 3 đến 7 | Chưa viết |

Cập nhật cột "Trạng thái" (Chưa viết, Nháp, Đã duyệt chéo, Đã hợp nhất) khi làm.

## Dàn ý gợi ý cho hai phần "Issues Encountered"

Mỗi phần theo đúng thứ tự, vì đề bài hỏi đúng ba điều: giảm thiểu thế nào, đưa vào kiến trúc ra sao, và nêu rõ.

1. **Vấn đề** (cụ thể hóa bằng kịch bản, ví dụ bảng 7 điểm đứt P1 đến P7).
2. **Vì sao M1 chưa đủ** (nêu thẳng cái đã có và cái còn thiếu, như `docs/adr/README.md` mục 1).
3. **Giải pháp** (cơ chế, không chỉ tên mẫu thiết kế).
4. **Cách giảm thiểu từng kịch bản** (bảng: kịch bản, quy tắc, bằng chứng test).
5. **Đưa vào kiến trúc:** thay đổi ở module view, component và connector view, deployment view, API. Dùng các bảng "Vị trí trong kiến trúc" ở cuối ADR.
6. **Đánh đổi và rủi ro còn lại** (mục "Hệ quả" của ADR, cộng với `implementation-notes.md` mục 3 và 4).
7. **Bằng chứng:** test và demo đã chạy, kèm đầu ra.
