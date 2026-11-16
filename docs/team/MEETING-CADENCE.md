# Nhịp làm việc và họp

Áp dụng cho lịch 16/11 đến 26/11/2026 ([`docs/handover/01-ke-hoach.md`](../handover/01-ke-hoach.md)). Mọi hạn là 23:00 của ngày được ghi.

## Lịch

| Họp | Khi nào | Ai | Thời lượng | Mục đích |
| --- | --- | --- | --- | --- |
| Họp đứng | Mỗi sáng ngày làm việc, 16 đến 25/11, cố định một giờ | Cả 8 người (hoặc trên kênh chat nếu không họp được) | 15 phút | Mỗi người: hôm qua hợp nhất gì, hôm nay làm gì, có gì lệch hợp đồng hoặc trễ. Lead báo story có nguy cơ cắt |
| Kiểm tích hợp K1 | 19/11, buổi chiều | Người phụ trách story của I-1, I-2, I-3; D1 điều phối | 60 phút | Thử tùy chọn I-1 đến I-3 trên `main`; ghi kết quả; quyết định cắt nếu cần |
| Kiểm tích hợp K2 | 23/11, cả ngày (họp chốt 30 phút cuối ngày) | Cả nhóm; D1 điều phối | | Mọi story mức B đã vào `main`; thử I-4 đến I-7 trên stack Docker; quyết định cắt cuối cùng |
| Tổng duyệt demo lần 1 | 24/11, xong trước 23:00 (giờ đóng băng mã) | Cả nhóm | 60 phút | 3 demo, app nhân viên, portal trên commit sẽ đóng băng; ghi lỗi chặn nộp |
| Rà báo cáo và tổng duyệt lần 2 | 25/11 | Cả nhóm | 90 phút | Đọc chéo bản hợp nhất (D-12); chạy lại demo trên commit đã đóng băng; dự phòng sửa lỗi chặn nộp |
| Nộp | 26/11, trước 23:00 | D1 | | Theo `docs/runbook/release-checklist.md` |

Không có họp lead riêng: lead trao đổi trong họp đứng và ở K1, K2. Không xếp họp vào 21 và 22/11.

## Quy tắc giao tiếp

1. **Không ai chờ team khác.** Story xong được bằng hợp đồng và bản giả; phần cần mã thật của nhau là kiểm tích hợp tùy chọn. Thấy mình đang chờ thì báo ngay, không chờ họp đứng: thường là thiếu fixture hoặc hợp đồng chưa rõ, và lead của bên cung cấp hợp đồng sửa `docs/api` trong ngày.
2. Mọi thảo luận ảnh hưởng hợp đồng phải để lại dấu vết ở issue hoặc PR, không chỉ trong chat.
3. Quyết định kiến trúc đi qua ADR, không đi qua chat.
4. Mỗi issue có đúng một người nhận (assignee). Việc hai người cùng làm thì tách thành hai issue.

## Bảng theo dõi

GitHub Projects với các cột: `Backlog` → `Ready` → `In progress` → `In review` → `Done`.
Mỗi story trong `WORK-BREAKDOWN.md` mục 4 là một issue (mẫu `story.md`), gắn nhãn `team:A|B|C|D`, `level:B|N|T`. Hạn của issue là ngày ở [`01-ke-hoach.md`](../handover/01-ke-hoach.md) mục 3 (nhãn `week:W1..W4` cũ không còn dùng).

Giới hạn việc dở dang: mỗi người tối đa **2 issue** ở `In progress` cùng lúc.

## Nhật ký quyết định

Ghi mỗi quyết định ở dạng một dòng: ngày, quyết định, lý do, ai đồng ý.

| Ngày | Quyết định | Lý do | Đồng thuận |
| --- | --- | --- | --- |
| 08/11/2026 | Chọn Java 21, Spring Boot 3, Gradle đa module, monorepo | Kiểm tra kiến trúc bằng ArchUnit (AC-09), Testcontainers, quen thuộc với môn học | Người ra yêu cầu scaffold |
| 08/11/2026 | Chia team theo bounded context | Giảm xung đột mã, mỗi thư mục một chủ | Người ra yêu cầu scaffold |
| 16/11/2026 | Lịch mới 16 đến 26/11: đóng băng mã 24/11, nộp 26/11; bốn team độc lập, kiểm tích hợp tùy chọn 19/11 và 23/11; ADR-11 đến 15 Accepted | Bước gỡ phụ thuộc đã xong mọi phần dùng chung | `<bốn lead xác nhận>` |
| `<ngày>` | `<điền khi họp>` | | |

## Khi một team trễ

1. Lead báo trong họp đứng: story nào, trễ bao lâu. Vì không team nào chờ ai, trễ chỉ ảnh hưởng chính team đó và báo cáo.
2. Cắt theo thứ tự ở [`01-ke-hoach.md`](../handover/01-ke-hoach.md) mục 6.3: mức `T`, rồi `N`. **Không cắt mức `B`** mà không có ý kiến của cả bốn lead; quyết định ở K1 (19/11) hoặc K2 (23/11).
3. Người trong cùng team còn thời gian nhận việc trước; người team khác chỉ hỗ trợ bằng review hoặc viết test, không sửa thư mục của team khác khi chưa có PR được chủ sở hữu duyệt.
4. Ghi quyết định vào bảng trên.
