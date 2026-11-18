# Lịch và biểu đồ Gantt: 16/11 đến 26/11/2026

## Mở bản tương tác

### [▶ Mở lịch tương tác](https://htmlpreview.github.io/?https://github.com/lamelihuynh/tmc/blob/main/docs/handover/lich-ban-giao.html)

Trang này vẽ lịch của 8 người: bốn làn song song (Team A, B, C, D, mỗi team hai người) và một làn mốc chung. Bấm vào một story để xem mục tiêu, người làm, ngày, fixture để code theo và tiêu chí xong. Bên dưới lịch là bảng 7 lần kiểm tích hợp **tùy chọn** (I-1 đến I-7): không story nào chờ chúng.

[![Biểu đồ Gantt của lịch 16/11 đến 26/11, bấm để mở bản tương tác](gantt.png)](https://htmlpreview.github.io/?https://github.com/lamelihuynh/tmc/blob/main/docs/handover/lich-ban-giao.html)

**Mọi hạn là 23:00 của ngày cuối cùng trên thanh.** Thanh đậm là story bắt buộc; thanh nhạt là nên có hoặc làm nếu còn thời gian (cắt trước khi trễ, thứ tự ở [01-ke-hoach.md](01-ke-hoach.md) mục 6.3). Hình thoi là mốc chung, đặt đúng 23:00 của ngày đó. Thứ Bảy 21 và Chủ nhật 22/11 không xếp việc. Không có mũi tên phụ thuộc giữa các team: mỗi làn chạy độc lập.

### Vì sao không bấm thẳng vào tệp HTML

GitHub chỉ hiện mã nguồn khi bạn bấm vào một tệp `.html`, không chạy nó. Có ba cách xem bản tương tác:

| Cách | Đường dẫn | Ghi chú |
| --- | --- | --- |
| Xem trước qua htmlpreview | [đường dẫn ở trên](https://htmlpreview.github.io/?https://github.com/lamelihuynh/tmc/blob/main/docs/handover/lich-ban-giao.html) | Dùng được ngay. Dịch vụ bên thứ ba, đọc tệp công khai từ nhánh `main` |
| GitHub Pages | <https://lamelihuynh.github.io/tmc/> | Đường dẫn chính thức, chỉ có sau khi người quản trị repo bật Pages (cách bật ở đầu `.github/workflows/pages.yml`) |
| Mở từ máy | Mở `docs/handover/lich-ban-giao.html` bằng trình duyệt sau khi `git pull` | Không cần mạng, trừ phông chữ |

Muốn xem lịch của một nhánh khác, thay `main` trong đường dẫn htmlpreview bằng tên nhánh.

## Mốc chung

| Ngày (23:00) | Mốc |
| --- | --- |
| 16/11 (sáng) | Việc 0: nhánh `chore/x/decouple-teams` vào `main` |
| 19/11 | K1 Kiểm tích hợp giữa kỳ: I-1, I-2, I-3 (tùy chọn) |
| 20/11 | Bản nháp báo cáo của Team A, B, C |
| 23/11 | K2 Tích hợp đầy đủ: mọi story bắt buộc đã vào `main`; I-4 đến I-7 (tùy chọn) |
| 24/11 | Đóng băng mã; tổng duyệt demo lần 1; bản cuối phần báo cáo của từng team |
| 25/11 | Hợp nhất báo cáo (D-12), xuất PDF; tổng duyệt lần 2; dự phòng |
| 26/11 | Nộp bài |

## Hạn theo ngày

| Hạn (23:00) | Phải hợp nhất xong |
| --- | --- |
| 16/11 | Việc 0, A-03, B-01 (còn lại), C-01 (còn lại), D-05 |
| 17/11 | A-06, A-01 (còn lại), B-02 (còn lại), C-08, D-06 |
| 18/11 | A-04, B-06, C-02, D-04, D-07 |
| 19/11 | A-02, A-12, B-03, C-05, C-11, D-02 (còn lại), D-03 (còn lại), D-08 |
| 20/11 | A-05, A-11, B-07, B-08, C-10, C-12, D-15; nháp R-A, R-B, R-C |
| 23/11 | A-09, D-13, D-14; mọi story bắt buộc đã vào `main` |
| 24/11 | Đóng băng mã; D-16 (trong tổng duyệt); bản cuối R-A, R-B, R-C |
| 25/11 | D-12: PDF báo cáo |
| 26/11 | Nộp |

## Nguồn của ảnh Gantt

Ảnh `gantt.png` được vẽ từ khối Mermaid dưới đây bằng `scripts/docs/render-gantt.mjs` (`cd scripts/docs && npm ci && node render-gantt.mjs`; cần Chrome và mạng để tải Mermaid). Khi đổi lịch, sửa cả bốn nơi cho khớp: khối này, dữ liệu trong `lich-ban-giao.html`, bảng ở [01-ke-hoach.md](01-ke-hoach.md) mục 3, và bảng việc trong tài liệu của team; rồi chạy lại script.

<details>
<summary>Mã Mermaid (GitHub cũng vẽ được khối này)</summary>

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'taskTextColor': '#10202e', 'taskTextDarkColor': '#10202e', 'taskTextOutsideColor': '#10202e', 'taskTextLightColor': '#10202e', 'taskBkgColor': '#9dbbdc', 'taskBorderColor': '#1f4e79', 'activeTaskBkgColor': '#f1f5fa', 'activeTaskBorderColor': '#1f4e79', 'sectionBkgColor': '#eef2f6', 'altSectionBkgColor': '#ffffff', 'sectionBkgColor2': '#eef2f6', 'excludeBkgColor': '#e3e6ea', 'gridColor': '#b9c2cc', 'titleColor': '#10202e', 'milestoneBkgColor': '#0f6b52', 'milestoneBorderColor': '#0f6b52'}}}%%
gantt
    title Lịch 4 team chạy song song, 16 đến 26/11/2026 (hạn 23h00)
    dateFormat YYYY-MM-DD HH:mm
    axisFormat %d/%m
    tickInterval 1day
    excludes weekends
    todayMarker off

    section Mốc chung
    Việc 0 nhánh vào main                   :milestone, m0, 2026-11-16 12:00, 0d
    K1 Kiểm tích hợp giữa kỳ                :milestone, k1, 2026-11-19 23:00, 0d
    Bản nháp báo cáo A B C                   :milestone, rp, 2026-11-20 23:00, 0d
    K2 Tích hợp đầy đủ                       :milestone, k2, 2026-11-23 23:00, 0d
    Đóng băng mã                             :milestone, fz, 2026-11-24 23:00, 0d
    Hợp nhất báo cáo                         :milestone, rm, 2026-11-25 23:00, 0d
    Nộp bài                                  :milestone, sb, 2026-11-26 23:00, 0d

    section Team A  Gate Station
    A-04 Kho cấu hình cổng (A1)              :a04, 2026-11-16 00:00, 3d
    A-12 Audit qua outbox (A1)               :a12, 2026-11-19 00:00, 1d
    A-11 JWT ở SecurityController (A1)       :a11, 2026-11-20 00:00, 12h
    R-A Báo cáo, nháp (A1)                   :ra1, 2026-11-20 12:00, 12h
    R-A Báo cáo, bản cuối (A1)               :ra2, 2026-11-24 00:00, 1d
    A-03 Dựng lại anti-passback (A2)         :a03, 2026-11-16 00:00, 1d
    A-06 Chiều khi gửi lại (A2)              :active, a06, 2026-11-17 00:00, 12h
    A-01 Redis khởi động lại (A2)            :a01, 2026-11-17 12:00, 12h
    A-02 Anti-passback sau lô offline (A2)   :a02, 2026-11-18 00:00, 2d
    A-05 Test hợp đồng turnstile (A2)        :active, a05, 2026-11-20 00:00, 12h
    A-09 Kiểm thử tải, chuẩn bị (A2)         :a09, 2026-11-20 12:00, 12h
    A-09 Kiểm thử tải, đo (A1, A2)           :a09b, 2026-11-23 00:00, 1d

    section Team B  Attendance
    B-06 Hiệu chỉnh chấm công (B1)           :b06, 2026-11-16 00:00, 3d
    B-07 Xác minh mở thủ công (B1)           :active, b07, 2026-11-19 00:00, 36h
    R-B Báo cáo, nháp (B1)                   :rb1, 2026-11-20 12:00, 12h
    R-B Báo cáo, hoàn thiện (B1, B2)         :rb2, 2026-11-23 00:00, 2d
    B-01 Test rollback (B2)                  :b01, 2026-11-16 00:00, 12h
    B-02 Dừng, bật lại RabbitMQ (B2)         :b02, 2026-11-16 12:00, 36h
    B-03 Hàng đợi bất thường (B2)            :b03, 2026-11-18 00:00, 2d
    B-08 Chỉ số vận hành (B2)                :active, b08, 2026-11-20 00:00, 1d

    section Team C  QR và Identity
    C-08 Audit qua AuditOutbox (C1)          :c08, 2026-11-16 00:00, 2d
    C-11 Database cho identity (C1)          :active, c11, 2026-11-18 00:00, 2d
    C-12 Xoay khóa JWT (C1)                  :active, c12, 2026-11-20 00:00, 1d
    R-C Báo cáo, hoàn thiện (C1, C2)         :rc2, 2026-11-23 00:00, 2d
    C-01 Đăng ký song song (C2)              :c01, 2026-11-16 00:00, 12h
    C-02 Nguồn HR cho danh bạ (C2)           :c02, 2026-11-16 12:00, 60h
    C-05 TOTP (C2)                           :c05, 2026-11-19 00:00, 1d
    C-10 Mô hình đe dọa, R-C nháp (C2)       :c10, 2026-11-20 00:00, 1d

    section Team D  Kiosk, Portal, Central
    D-06 Adapter HR/Payroll (D1)             :d06, 2026-11-16 00:00, 2d
    D-07 Cảnh báo hạn chót (D1)              :d07, 2026-11-18 00:00, 1d
    D-02 Bảo vệ nhánh (D1)                   :d02, 2026-11-19 00:00, 12h
    D-03 Cảnh báo DLQ (D1)                   :active, d03, 2026-11-19 12:00, 12h
    D-15 Kiosk dùng employees offline (D1)   :active, d15, 2026-11-20 00:00, 1d
    D-12 Hợp nhất báo cáo (D1)               :d12, 2026-11-23 00:00, 3d
    D-05 Quy tắc gian lận R4 (D2)            :d05, 2026-11-16 00:00, 1d
    D-04 Read model JDBC (D2)                :d04, 2026-11-17 00:00, 36h
    D-08 Kho audit, chuỗi băm (D2)           :d08, 2026-11-18 12:00, 36h
    D-13 Portal, app gửi Bearer (D2)         :active, d13, 2026-11-20 00:00, 1d
    D-13 tiếp (D2)                           :active, d13b, 2026-11-23 00:00, 12h
    D-14 Test kiosk Windows (D2)             :active, d14, 2026-11-23 12:00, 12h
    D-16 Webcam, điện thoại thật (D2)        :active, d16, 2026-11-24 00:00, 1d
```

</details>
