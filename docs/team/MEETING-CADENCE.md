# Nhịp làm việc và họp

## Lịch

| Họp | Khi nào | Ai | Thời lượng | Mục đích |
| --- | --- | --- | --- | --- |
| Đồng bộ nhanh (stand-up) | Mỗi ngày làm việc, cố định một giờ | Cả 8 người, hoặc trên kênh chat nếu không họp được | 10 phút | Mỗi người trả lời 3 câu: hôm qua xong gì, hôm nay làm gì, đang bị chặn bởi ai |
| Họp lead | 2 lần mỗi tuần | 4 lead | 30 phút | Phụ thuộc giữa team, cắt phạm vi, xung đột hợp đồng |
| Demo nội bộ | Cuối mỗi tuần | Cả nhóm | 30 phút | Chạy `make demo-*` trên nhánh `main`; mỗi team trình bày thứ vừa xong |
| Rà soát báo cáo | W3 giữa và W4 đầu | Cả nhóm | 60 phút | Đọc chéo phần báo cáo của nhau |
| Hồi cứu (retro) | Cuối W2 và W4 | Cả nhóm | 30 phút | Giữ gì, bỏ gì, thử gì |

## Quy tắc giao tiếp

1. **Bị chặn quá nửa ngày thì báo ngay**, không chờ stand-up. Ghi rõ: chặn bởi story nào, cần gì, từ ai, trước lúc nào.
2. Mọi thảo luận ảnh hưởng hợp đồng phải để lại dấu vết ở issue hoặc PR, không chỉ trong chat.
3. Quyết định kiến trúc đi qua ADR, không đi qua chat.
4. Mỗi issue có đúng một người nhận (assignee). Việc hai người cùng làm thì tách thành hai issue.

## Bảng theo dõi

GitHub Projects với các cột: `Backlog` → `Ready` → `In progress` → `In review` → `Done`.
Mỗi story trong `WORK-BREAKDOWN.md` tạo thành một issue bằng mẫu `story.md`, gắn nhãn `team:A|B|C|D`, `level:B|N|T`, `week:W1..W4`.

Giới hạn việc dở dang: mỗi người tối đa **2 issue** ở `In progress` cùng lúc.

## Nhật ký quyết định

Ghi mỗi quyết định ở dạng một dòng: ngày, quyết định, lý do, ai đồng ý.

| Ngày | Quyết định | Lý do | Đồng thuận |
| --- | --- | --- | --- |
| 08/11/2026 | Chọn Java 21, Spring Boot 3, Gradle đa module, monorepo | Kiểm tra kiến trúc bằng ArchUnit (AC-09), Testcontainers, quen thuộc với môn học | Người ra yêu cầu scaffold |
| 08/11/2026 | Chia team theo bounded context | Giảm xung đột mã, mỗi thư mục một chủ | Người ra yêu cầu scaffold |
| `<ngày>` | `<điền khi họp>` | | |

## Khi một team trễ

1. Lead của team báo trong họp lead, nói rõ story nào, trễ bao lâu, ảnh hưởng ai.
2. Cắt theo thứ tự: mức `T`, rồi mức `N`, và **không bao giờ cắt mức `B`** mà không có ý kiến của cả bốn lead.
3. Team khác còn thời gian nhận việc theo thứ tự đường găng ở `WORK-BREAKDOWN.md` mục 6.
4. Ghi quyết định vào bảng trên.
