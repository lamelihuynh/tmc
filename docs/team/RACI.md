# Ma trận trách nhiệm (RACI)

**R** = Responsible (làm), **A** = Accountable (chịu trách nhiệm cuối, duyệt), **C** = Consulted (được hỏi trước), **I** = Informed (được báo sau).

| Hoạt động | Team A | Team B | Team C | Team D |
| --- | --- | --- | --- | --- |
| Mã `gate-station`, `turnstile-simulator` | **A, R** | C | C | I |
| Mã `attendance` | C | **A, R** | I | C |
| Mã `user-qr`, `identity`, `device-simulator` | C | I | **A, R** | I |
| Mã `kiosk-simulator`, `web-portal`, `reporting`, `hr-integration`, `audit-admin` | I | C | C | **A, R** |
| Hợp đồng API (`docs/api`) | R/C | R/C | R/C | R/C (bên cung cấp là A) |
| ADR mới | R/C | R/C | R/C | R/C (người đề xuất là R, cả bốn lead là A) |
| Thư viện `platform/*` | C | C | C | C (người sửa là R, cả bốn lead duyệt) |
| CI, `docker`, `scripts`, `.github` | I | I | I | **A, R** |
| Cấu hình nhánh bảo vệ, `CODEOWNERS` | C | C | C | **A, R** |
| Phần báo cáo của mình | **A, R** | **A, R** | **A, R** | **A, R** (D hợp nhất, là A cho bản cuối) |
| Hợp nhất và nộp báo cáo | C | C | C | **A, R** |
| Quyết định cắt phạm vi khi trễ | R | R | R | R (đồng thuận bốn lead, `MEETING-CADENCE.md`) |
| Trả lời giảng viên | C | C | C | **A, R** (một đầu mối) |

Một người **không bao giờ** vừa là tác giả vừa là người duy nhất duyệt một PR.
