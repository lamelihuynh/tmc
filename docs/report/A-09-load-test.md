# A-09 — Kiểm thử tải Gate Station

- Kết quả: **PASS**
- Thời điểm: 2026-11-24 01:34:11 +07:00
- Base commit: 726e068; working tree was dirty (uncommitted Team A changes were included).
- Môi trường: Windows; Java 21; profile local; HTTP thật giữa gate-station, user-qr, attendance và turnstile-simulator; không dùng Docker.
- Máy: 12th Gen Intel(R) Core(TM) i7-12700H; RAM 7.7 GiB; OS Microsoft Windows NT 10.0.26200.0.
- Kịch bản: 800 lượt POST /kiosk/v1/scans tới BD-GATE-03, luân phiên 4 nhân viên mẫu, token ES256 mới cho mỗi lượt, cổng hai chiều, 8 lượt warm-up không tính vào mẫu.
- Tốc độ đích: 0.45 lượt/giây; thời gian đo: 1776.02 giây (29.60 phút); thông lượng thực tế: 0.45 lượt/giây.

| Chỉ số | Kết quả | Tiêu chí |
| --- | ---: | ---: |
| Lượt đo thành công | 800 / 800 | 800 / 800 |
| Lượt lỗi | 0 | 0 |
| HTTP status | 200: 800 | mọi lượt 200 |
| Kết quả ứng dụng | ALLOWED: 800 | mọi lượt ALLOWED |
| Trung bình | 23.49 ms | — |
| p50 | 21.95 ms | — |
| p95 | 36.82 ms | ≤ 1,000 ms |
| p99 | 41.60 ms | ≤ 2,000 ms |

Lệnh chạy lại từ PowerShell tại thư mục gốc repo: .\scripts\perf\gate-station-a09.ps1.

Latency tính từ lúc gửi HTTP request đến khi đọc xong response; thời gian khởi tạo, đăng ký thiết bị, warm-up và sinh token không nằm trong latency. Tiến trình phát token chạy tuần tự và được pacing ở tốc độ đích; kết quả này áp dụng cho máy phát tải và stack local nêu trên, không đại diện cho tải đồng thời hoặc stack Docker đầy đủ.