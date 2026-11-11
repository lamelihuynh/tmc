# Biểu đồ Gantt: 12/11 đến 25/11/2026

Biểu đồ này vẽ cùng một lịch với [01-ke-hoach.md](01-ke-hoach.md) mục 3. GitHub, GitLab và IntelliJ (có plugin Mermaid) hiển thị được trực tiếp. Nếu trình xem của bạn không vẽ Mermaid, xem ảnh [gantt.png](gantt.png) (tạo lại bằng `scripts/docs/render-gantt.mjs`). Bản tương tác, bấm vào từng story để xem chi tiết: mở [lich-ban-giao.html](lich-ban-giao.html) bằng trình duyệt.

**Mọi hạn là 23:00 của ngày cuối cùng trên thanh.** Thanh màu đỏ là đường găng: chậm ở đó thì chậm cả phần trung tâm. Hình thoi là mốc, đặt đúng 23:00 của ngày đó. Thứ Bảy và Chủ nhật không xếp việc.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'taskTextColor': '#10202e', 'taskTextDarkColor': '#10202e', 'taskTextOutsideColor': '#10202e', 'taskBkgColor': '#c6d8ee', 'taskBorderColor': '#1f4e79', 'activeTaskBkgColor': '#c6d8ee', 'critBkgColor': '#f3aaa0', 'critBorderColor': '#a3231c', 'sectionBkgColor': '#eef2f6', 'altSectionBkgColor': '#ffffff', 'sectionBkgColor2': '#eef2f6', 'excludeBkgColor': '#e3e6ea', 'gridColor': '#b9c2cc', 'titleColor': '#10202e'}}}%%
gantt
    title Lịch M2 của 4 team (hạn 23h00 mỗi ngày)
    dateFormat YYYY-MM-DD HH:mm
    axisFormat %d/%m
    tickInterval 1day
    excludes weekends
    todayMarker off

    section Mốc
    M0 Khởi động                             :milestone, m0, 2026-11-12 23:00, 0d
    M1 Đóng băng hợp đồng                    :milestone, m1, 2026-11-13 23:00, 0d
    M2 Hạ tầng thật                          :milestone, m2, 2026-11-18 23:00, 0d
    Nộp bản nháp báo cáo                     :milestone, m25, 2026-11-20 23:00, 0d
    M3 Đủ tính năng                          :milestone, m3, 2026-11-23 23:00, 0d
    M4 Đóng băng mã                          :milestone, m4, 2026-11-24 23:00, 0d
    M5 Nộp bài                               :milestone, m5, 2026-11-25 23:00, 0d

    section Team A  Gate Station
    A-05 Test hợp đồng turnstile (A2)        :a05, 2026-11-12 00:00, 1d
    A-04 Kho cấu hình cổng (A1)              :a04, 2026-11-13 00:00, 5d
    A-01 Kiểm chứng Redis (A2)               :a01, 2026-11-13 00:00, 3d
    A-03 Dựng lại anti-passback (A2)         :a03, 2026-11-18 00:00, 2d
    A-02 Đồng bộ sau offline (A2)            :a02, 2026-11-19 00:00, 2d
    A-10 Danh bạ từ gói đồng bộ (A1)         :a10, 2026-11-20 00:00, 1d
    A-06 Chiều khi gửi lại (A1)              :a06, 2026-11-20 00:00, 1d
    A-09 Kiểm thử tải (cả hai)               :a09, 2026-11-23 00:00, 1d

    section Team B  Attendance
    B-05 API last-direction (B1)             :b05, 2026-11-12 00:00, 2d
    B-04 AttemptView thêm trường (B2)        :b04, 2026-11-12 00:00, 2d
    B-01 Kiểm chứng JDBC (B1)                :crit, b01, 2026-11-16 00:00, 2d
    B-02 Outbox với RabbitMQ (B2)            :crit, b02, 2026-11-16 00:00, 3d
    B-10 ProcessedMessageStore (B2)          :b10, 2026-11-18 00:00, 1d
    B-06 Hiệu chỉnh chấm công (B1)           :b06, 2026-11-18 00:00, 3d
    B-03 Hàng đợi bất thường (B2)            :b03, 2026-11-19 00:00, 2d
    B-08 Chỉ số vận hành (B2)                :b08, 2026-11-23 00:00, 1d

    section Team C  QR và Identity
    Hợp đồng JWT (C1)                        :cjwt, 2026-11-12 00:00, 2d
    C-10 Mô hình đe dọa (C2, xen kẽ)         :c10, 2026-11-12 00:00, 7d
    Hợp đồng employees (C2)                  :cemp, 2026-11-13 00:00, 1d
    C-04 Cấp JWT (C1)                        :c04, 2026-11-13 00:00, 4d
    C-01 Kiểm chứng registry JDBC (C2)       :c01, 2026-11-16 00:00, 2d
    C-02 Employees trong gói đồng bộ (C2)    :c02, 2026-11-17 00:00, 3d
    C-06 Xác thực JWT ở user-qr (C1)         :c06, 2026-11-19 00:00, 2d
    C-05 TOTP (C2)                           :c05, 2026-11-20 00:00, 1d
    C-08 Audit qua outbox (C1)               :c08, 2026-11-23 00:00, 1d

    section Team D  Kiosk, Portal, Central
    D-01 Kiểm chứng compose (D1)             :d01, 2026-11-12 00:00, 1d
    D-14 Sửa test kiosk Windows (D2)         :d14, 2026-11-12 00:00, 1d
    D-02 CI (D2)                             :d02, 2026-11-12 00:00, 3d
    D-06 Adapter HR/Payroll (D1)             :d06, 2026-11-13 00:00, 4d
    D-05 Quy tắc gian lận R4 (D2)            :d05, 2026-11-16 00:00, 2d
    D-08 Chuỗi băm audit (D2)                :d08, 2026-11-18 00:00, 1d
    D-03 Consumer thật (cả hai)              :crit, d03, 2026-11-19 00:00, 2d
    D-07 Cảnh báo hạn chót (D1)              :d07, 2026-11-20 00:00, 1d
    D-04 Read model hiệu chỉnh (D2)          :d04, 2026-11-23 00:00, 1d
    D-13 App nhân viên dùng JWT (D2, C2)     :d13, 2026-11-23 00:00, 1d
    D-12 Hợp nhất báo cáo và nộp (D1)        :d12, 2026-11-23 00:00, 3d
```

![Biểu đồ Gantt của lịch M2](gantt.png)

## Đọc biểu đồ

| Ký hiệu | Nghĩa |
| --- | --- |
| Thanh | Một story; đầu trái là ngày bắt đầu, đầu phải là hết ngày cuối (hạn 23:00) |
| Thanh đỏ | Đường găng: B-01, B-02, D-03 |
| Hình thoi | Mốc của cả nhóm, lúc 23:00 |
| (A1), (B2), ... | Người làm trong team: 1 là lead, 2 là người còn lại |

Thứ tự phụ thuộc giữa các thanh (thanh nào phải xong trước thanh nào) và phương án khi bị trễ nằm ở [01-ke-hoach.md](01-ke-hoach.md) mục 4.

## Hạn theo ngày

| Hạn (23:00) | Phải hợp nhất xong |
| --- | --- |
| 12/11 | D-01, D-14, A-05; mọi người chạy được stack local |
| 13/11 | B-04, B-05; hợp đồng JWT; hợp đồng `employees`; lược đồ `AttendanceCorrected`; thiết kế A-04 trong OpenAPI |
| 16/11 | D-02 (CI) |
| 17/11 | A-01, B-01, C-01, D-05 |
| 18/11 | B-02, B-10, C-04, D-06, D-08 |
| 19/11 | A-03, A-04, C-02 |
| 20/11 | A-02, A-06, A-10, B-03, B-06, C-05, C-06, C-10, D-03, D-07; bản nháp báo cáo của 4 team |
| 23/11 | A-09, B-08, C-08, D-04, D-13; mọi story bắt buộc đã vào `main` |
| 24/11 | Đóng băng mã; tổng duyệt demo; báo cáo đủ nội dung |
| 25/11 | Nộp PDF báo cáo, URL GitHub, gắn tag |
