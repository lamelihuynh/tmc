# Kế hoạch 16/11 đến 26/11/2026: bốn team chạy song song

Tài liệu này trả lời: ai làm gì, ngày nào, khi nào kiểm tích hợp, và cắt gì khi trễ. Từ 16/11, **không team nào chờ mã chưa xong của team khác**: mọi hợp đồng, fixture và hạ tầng dùng chung đã có sẵn (bước gỡ phụ thuộc, xem [`_decoupling-changelog.md`](_decoupling-changelog.md)).

## 1. Lịch và mốc

| Ngày | Thứ | Mốc (hạn 23:00) |
| --- | --- | --- |
| 16/11 | Hai | Việc 0: nhánh `chore/x/decouple-teams` vào `main` (sáng). Mọi team bắt đầu story |
| 17/11 | Ba | |
| 18/11 | Tư | |
| 19/11 | Năm | **K1 Kiểm tích hợp giữa kỳ** (tùy chọn, không chặn): I-1, I-2, I-3 |
| 20/11 | Sáu | Bản nháp báo cáo của Team A, B, C trong `docs/report/` |
| 21, 22/11 | Bảy, Chủ nhật | Không xếp việc. Dùng nếu cần đuổi kịp |
| 23/11 | Hai | **K2 Tích hợp đầy đủ**: mọi story bắt buộc đã vào `main`; I-4 đến I-7 trên stack Docker |
| 24/11 | Ba | **Đóng băng mã** lúc 23:00: chỉ sửa lỗi. Tổng duyệt demo lần 1. Bản cuối phần báo cáo của từng team |
| 25/11 | Tư | **Hợp nhất báo cáo** (D-12), xuất PDF. Tổng duyệt lần 2. Dự phòng |
| 26/11 | Năm | **Nộp bài**: PDF báo cáo có URL GitHub, gắn thẻ `m2-submission` |

**Quy ước về giờ:** mọi hạn là **23:00 của ngày được ghi** (giờ Việt Nam). "Xong" nghĩa là PR đã hợp nhất vào `main` trước 23:00. Hợp nhất sau 23:00 tính là trễ sang hôm sau, lead báo trong họp đứng kế tiếp.

Ngày làm việc: 7 ngày (16 đến 20/11, 23 và 24/11) cho code, 25/11 cho báo cáo và tổng duyệt. Biểu đồ Gantt: [03-gantt.md](03-gantt.md).

## 2. Vì sao không team nào bị chặn

1. **Hợp đồng đã chốt và có trong `docs/api`.** JWT và JWKS (`identity.openapi.yaml`), `AttendanceCorrected` (`attendance-events.asyncapi.yaml`), exchange `tmc.events` và khóa định tuyến (ADR-15), `employees` trong gói đồng bộ, last-direction, `AttemptView`. Team nào thêm endpoint của chính mình thì sửa tệp OpenAPI của mình trong cùng PR.
2. **Mỗi phía đã có bản giả và fixture.** `FakeAttendanceGateway`, `GateFixture`, `AttendanceFixture`, `PhoneSimulator`, `FakeEdge`; `TestJwtKeys`, `TestTokens` (`platform/security` testFixtures); `TmcContainers` (PostgreSQL, RabbitMQ, Redis); `docs/api/fixtures/employee-seed.json`, `qr-token-policy.json`.
3. **Hạ tầng dùng chung đã có.** `platform/messaging` (outbox, relay, publisher, dedup JDBC, `AuditOutbox`), `platform/security` (`PrincipalResolver`), database `gate_store`, database `identity`, consumer thật ở ba service trung tâm.
4. **Phần cần mã thật của team khác chỉ là kiểm tích hợp tùy chọn** (mục 4), làm vào 19/11 và 23/11. Bên kia chưa xong thì ghi "chưa kiểm" và đi tiếp; story của mình vẫn tính là xong khi đạt tiêu chí bằng bản giả.

Service không import mã của nhau. Chỗ dễ xung đột là `docs/api` và `platform/*`: PR chạm vào đó cần thêm một người duyệt từ team bị ảnh hưởng, nhưng việc duyệt không phải là chờ mã.

## 3. Ai làm gì, ngày nào

Mỗi team có hai người: **1** (lead) và **2**. Chi tiết, tiêu chí hoàn thành và fixture của từng story nằm trong tài liệu của team: [team-a.md](team-a.md), [team-b.md](team-b.md), [team-c.md](team-c.md), [team-d.md](team-d.md).

| Ngày | A1 | A2 | B1 | B2 | C1 | C2 | D1 | D2 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **16/11** Hai | A-04 | A-03 | B-06 | B-01 (sáng), B-02 (chiều) | C-08 | C-01 (sáng), C-02 (chiều) | Việc 0 (sáng), D-06 | D-05 |
| **17/11** Ba | A-04 | A-06 (sáng), A-01 (chiều) | B-06 | B-02 | C-08 | C-02 | D-06 | D-04 |
| **18/11** Tư | A-04 | A-02 | B-06 | B-03 | C-11 | C-02 | D-07 | D-04 (sáng), D-08 (chiều) |
| **19/11** Năm, K1 | A-12 | A-02 | B-07 | B-03 | C-11 | C-05 | D-02 (sáng), D-03 (chiều) | D-08 |
| **20/11** Sáu | A-11 (sáng), R-A nháp | A-05 (sáng), A-09 chuẩn bị | B-07 (sáng), R-B nháp | B-08 | C-12 | C-10, R-C nháp | D-15 | D-13 |
| 21, 22/11 | dự phòng | dự phòng | dự phòng | dự phòng | dự phòng | dự phòng | dự phòng | dự phòng |
| **23/11** Hai, K2 | A-09 | A-09 | R-B | I-6, bằng chứng R-B | R-C, I-4, I-5 | R-C | D-12, điều phối I-6 | D-13 (sáng), D-14 (chiều) |
| **24/11** Ba | Sửa lỗi, R-A cuối, tổng duyệt | Sửa lỗi, tổng duyệt | R-B cuối, tổng duyệt | Sửa lỗi, tổng duyệt | Sửa lỗi, tổng duyệt | R-C cuối, tổng duyệt | D-12, điều phối tổng duyệt | D-16, tổng duyệt |
| **25/11** Tư | Rà báo cáo | Rà báo cáo | Rà báo cáo | Rà báo cáo | Rà báo cáo | Rà báo cáo | D-12 hợp nhất, PDF | Hỗ trợ D-12, tổng duyệt lần 2 |
| **26/11** Năm | | | | | | | **Nộp** | |

Bảng gộp theo story (cùng dữ liệu với Gantt và lịch tương tác):

| Team | Story | Mức | Người | Ngày |
| --- | --- | --- | --- | --- |
| A | A-04 Kho cấu hình cổng trên `gate_store` | B | A1 | 16 đến 18/11 |
| A | A-12 Audit cổng qua outbox | B | A1 | 19/11 |
| A | A-11 JWT ở `SecurityController` | B | A1 | 20/11 sáng |
| A | A-03 Dựng lại anti-passback | B | A2 | 16/11 |
| A | A-06 Chiều khi gửi lại | N | A2 | 17/11 sáng |
| A | A-01 (còn lại) Redis khởi động lại | B | A2 | 17/11 chiều |
| A | A-02 Anti-passback sau lô offline | B | A2 | 18 đến 19/11 |
| A | A-05 Test hợp đồng turnstile | N | A2 | 20/11 sáng |
| A | A-09 Kiểm thử tải | B | A2, rồi A1 và A2 | 20/11 chiều đến 23/11 |
| A | R-A Báo cáo Team A | B | A1 | nháp 20/11, cuối 24/11 |
| B | B-06 Hiệu chỉnh chấm công | B | B1 | 16 đến 18/11 |
| B | B-07 Xác minh mở thủ công | N | B1 | 19 đến 20/11 sáng |
| B | R-B Báo cáo "mất kết nối" | B | B1, B2 | nháp 20/11, cuối 24/11 |
| B | B-01 (còn lại) Test rollback | B | B2 | 16/11 sáng |
| B | B-02 (còn lại) Dừng, bật lại RabbitMQ | B | B2 | 16/11 chiều đến 17/11 |
| B | B-03 Hàng đợi bất thường | B | B2 | 18 đến 19/11 |
| B | B-08 Chỉ số vận hành | N | B2 | 20/11 |
| C | C-08 Audit qua `AuditOutbox` | B | C1 | 16 đến 17/11 |
| C | C-11 Database cho identity | N | C1 | 18 đến 19/11 |
| C | C-12 Xoay khóa JWT | N | C1 | 20/11 |
| C | C-01 (còn lại) Đăng ký song song | B | C2 | 16/11 sáng |
| C | C-02 Nguồn HR cho danh bạ | B | C2 | 16/11 chiều đến 18/11 |
| C | C-05 TOTP | B | C2 | 19/11 |
| C | C-10 Mô hình đe dọa | B | C2 | 20/11 |
| C | R-C Báo cáo "QR tĩnh" | B | C2, C1 | nháp 20/11, cuối 24/11 |
| D | Việc 0 Hợp nhất nhánh gỡ phụ thuộc | B | D1 | 16/11 sáng |
| D | D-06 Adapter HR/Payroll, `AttendanceCorrected` | B | D1 | 16 đến 17/11 |
| D | D-07 Cảnh báo hạn chót | B | D1 | 18/11 |
| D | D-02 (còn lại) CODEOWNERS, bảo vệ nhánh | B | D1 | 19/11 sáng |
| D | D-03 (còn lại) Cảnh báo DLQ | N | D1 | 19/11 chiều |
| D | D-15 Kiosk dùng `employees` khi offline | N | D1 | 20/11 |
| D | D-12 Hợp nhất báo cáo, nộp | B | D1 | 23 đến 25/11, nộp 26/11 |
| D | D-05 Quy tắc gian lận R4 | B | D2 | 16/11 |
| D | D-04 Read model JDBC | B | D2 | 17 đến 18/11 sáng |
| D | D-08 Kho audit bền, chuỗi băm | B | D2 | 18/11 chiều đến 19/11 |
| D | D-13 Portal và app gửi Bearer | N | D2 | 20 đến 23/11 sáng |
| D | D-14 Test kiosk trên Windows | N | D2 | 23/11 chiều |
| D | D-16 Webcam và điện thoại thật | T | D2 | 24/11 |

## 4. Kiểm tích hợp tùy chọn

Thay cho bảng "bàn giao" cũ. Không có story nào chờ những mục này: mỗi story đã xong khi đạt tiêu chí bằng bản giả. Kiểm tích hợp chỉ để phát hiện lệch hợp đồng sớm; lệch thì bên sai hợp đồng sửa, ghi vào nhật ký quyết định.

| Mã | Ngày | Các team | Kiểm gì | Bên kia chưa xong thì |
| --- | --- | --- | --- | --- |
| I-1 | 19/11 | A, B | A-03 gọi last-direction thật của attendance khi Redis mất dữ liệu | Ghi "chưa kiểm", thử lại 23/11 |
| I-2 | 19/11 | C, A | C-02 đổi trạng thái nhân viên, sau tối đa 30 giây gate-station trả `EMPLOYEE_INACTIVE` | Như trên |
| I-3 | 19/11 | B, D | B-06 phát `AttendanceCorrected` qua RabbitMQ; reporting áp, hr-integration gửi (D-06) | Như trên |
| I-4 | 23/11 | A, C, D | Audit từ gate-station (A-12), user-qr, identity (C-08) vào kho audit bền (D-08), xem qua `GET /admin/v1/audit` | Ghi vào phần hạn chế |
| I-5 | 23/11 | A, C, D | Bearer đầu-cuối: app và portal (D-13) tới user-qr, gate-station (A-11), ba service trung tâm, trên profile mặc định | Ghi vào phần hạn chế; portal vẫn chạy ở `local` |
| I-6 | 23/11 | cả nhóm | 7 service ở profile mặc định và turnstile-simulator trên `make up`, chạy 3 demo (hiện **chưa kiểm chứng**) | Demo dùng stack `local` như hiện tại; ghi điều kiện vào báo cáo |
| I-7 | 23/11 | A, D | A-09 đo trên stack Docker | Dùng số đo trên stack `local`, ghi rõ điều kiện |

Ngày 24/11, trước giờ đóng băng: CI trên GitHub xanh ở commit sẽ đóng băng (D1 kiểm), rồi tổng duyệt demo.

## 5. Đã xong trong bước gỡ phụ thuộc

Chi tiết và lệnh đã chạy: [`_decoupling-changelog.md`](_decoupling-changelog.md).

| Story | Trạng thái |
| --- | --- |
| A-10, B-04, B-05, B-10, C-04, C-06 (bản user-qr), D-01 | Xong |
| A-01, B-01, B-02, C-01, D-02, D-03 | Phần lớn xong; phần còn lại ở mục 3 |
| `AttendanceCorrected` (hợp đồng, bảng `attendance_corrections`, reporting áp dụng) | Xong; còn use case và endpoint (B-06), phía HR (D-06) |

Số liệu kiểm chứng (macOS arm64, JDK 21, Docker): `./gradlew spotlessCheck test` **209 test, 10 bỏ qua, 0 lỗi**; `./gradlew integrationTest` **27 test, 0 lỗi**; app nhân viên 5 test vitest xanh. **Chưa kiểm chứng:** 3 demo trên stack Docker đầy đủ, dừng và bật lại RabbitMQ giữa lúc phát, `apps/web-portal` `npm test`, CI trên GitHub cho các thay đổi này.

## 6. Phạm vi

### 6.1. Thu nhỏ

| Story | Bản đầy đủ | Bản làm trong đợt này |
| --- | --- | --- |
| C-06 | Mọi service xác thực JWT | `platform/security` và user-qr (xong); gate-station (A-11), attendance (B-06), ba service trung tâm (D-13) tự gắn |
| D-03 | Consumer thật, DLQ, retry, theo dõi | Đã có consumer, DLQ, retry, dedup; còn cảnh báo DLQ |
| D-04 | Báo cáo theo nhóm, ca, xuất file, dashboard | Read model JDBC, áp hiệu chỉnh |
| D-06 | Adapter HTTP, tra cứu, scheduler có backoff | Adapter HTTP, tra cứu trước khi gửi lại, `AttendanceCorrected`. Không scheduler |
| D-07 | Đối soát kỳ, lô chốt, cảnh báo | Cảnh báo 08:00 và 12:00 ngày 1 |
| D-08 | PostgreSQL chỉ INSERT, chuỗi băm, truy vấn 5 giây, lưu 12 tháng | PostgreSQL chỉ INSERT, chuỗi băm |
| C-02 | Hồ sơ và vòng đời nhân viên từ HR | Bảng nhân viên trong user-qr, một endpoint HR đổi trạng thái và nhà máy |

### 6.2. Cắt khỏi đợt này

A-07 (cảnh báo trực tiếp cho bảo vệ), A-08 (callback từ cổng), B-09 (tham số quản trị), C-03 (vòng đời nhân viên), C-07 (quản lý tài khoản), C-09 (lệnh thu hồi trong device-simulator), D-09 (kịch bản sự cố tự động), D-10 (portal đăng nhập thật qua màn hình riêng), D-11 (Dockerfile từng service); hết hạn phiên theo hoạt động (`should_expire_an_idle_session`).

Mọi mục bị cắt hoặc thu nhỏ phải có trong phần "Hạn chế và việc tiếp theo" của báo cáo. D1 gom danh sách này ở D-12.

### 6.3. Thứ tự cắt khi trễ

Lead báo trễ trong họp đứng; quyết định cắt ở K1 (19/11) hoặc K2 (23/11), ghi vào [`MEETING-CADENCE.md`](../team/MEETING-CADENCE.md). Cắt theo thứ tự:

D-16 → C-12 → B-07 → A-05 → D-14 → B-08 → A-06 → D-03 → C-11 → D-15 → D-13

Story mức B chỉ cắt khi cả bốn lead đồng ý. **Không cắt:** A-02, A-03 (lỗ hổng anti-passback thật), A-04, B-06, C-02, C-08, D-04, D-06, D-08, A-09 (được đo trên stack `local` nếu Docker không kịp), D-12.

## 7. Báo cáo

Mỗi team viết phần của mình bằng Markdown trong `docs/report/` (khung ở [`docs/report/README.md`](../report/README.md)). A, B, C nộp bản nháp trước 23:00 ngày 20/11 và bản cuối trước 23:00 ngày 24/11. D1 viết phần của Team D và hợp nhất từ 23/11, xuất PDF ngày 25/11.

| Phần | Team viết | Team bổ sung bằng chứng |
| --- | --- | --- |
| Issues Encountered: mất kết nối (ADR-11, 7 điểm đứt) | B | A, D |
| Issues Encountered: QR tĩnh (ADR-12, mô hình đe dọa) | C | A, D |
| Cập nhật yêu cầu và use case theo `docs/adr/README.md` mục 3 | C (NFR-05, UC-07), B (NFR-09, UC-08) | |
| Sơ đồ component, sequence, deployment | A (quét QR), B (outbox), C (bảo mật), D (HR) | |
| Áp dụng SOLID, gắn với lớp thật | Cả bốn, mỗi team phần mã của mình | |
| Phản ánh | Cả bốn, mỗi team một đoạn | |
| Task 2: hiện thực, cách chạy, ảnh kết quả | D | |
| Hạn chế và việc tiếp theo | D gom từ mục 6 | Cả bốn rà |

Mọi khẳng định về hành vi phải trỏ tới một test hoặc một kịch bản demo. Không ghi "đạt p95 dưới 1 giây" nếu A-09 chưa có số đo; không ghi "chạy trên Docker" cho phần chưa qua I-6.

## 8. Nhịp làm việc

Chi tiết: [`MEETING-CADENCE.md`](../team/MEETING-CADENCE.md).

| Việc | Khi nào | Ai |
| --- | --- | --- |
| Họp đứng 15 phút: hôm qua xong gì, hôm nay làm gì, có gì lệch hợp đồng | Mỗi sáng ngày làm việc, 16 đến 25/11 | Cả 8 người |
| Kiểm tích hợp K1 | 19/11, chiều | Người phụ trách story liên quan; D1 điều phối |
| Kiểm tích hợp K2 | 23/11, cả ngày | Cả nhóm; D1 điều phối |
| Tổng duyệt demo lần 1 | 24/11, trước 23:00, trên mã đóng băng | Cả nhóm |
| Tổng duyệt lần 2, rà báo cáo | 25/11 | Cả nhóm |
| Nộp | 26/11, trước 23:00 | D1 |

Quy trình một thay đổi ([`CONTRIBUTING.md`](../../CONTRIBUTING.md)): nhánh từ `main`, mã kèm test, `./gradlew spotlessApply test`, PR, người cùng team duyệt (thêm một người team khác nếu chạm `docs/api` hoặc `platform`), CI xanh, squash merge. PR nhỏ, hợp nhất trong ngày; PR mở quá hai ngày thì chia nhỏ.

## 9. Định nghĩa "xong" cho một story

1. Tiêu chí hoàn thành của story được chứng minh trong mô tả PR (lệnh đã chạy và kết quả).
2. Có test đi kèm; test `@Disabled` tương ứng đã được bật.
3. `./gradlew spotlessCheck test` xanh, `ArchitectureTest` xanh; story có test tích hợp thì `integrationTest` của module đó xanh.
4. Hợp đồng thay đổi thì `docs/api` đổi trong cùng PR.
5. Quyết định kiến trúc mới thì có ADR.
6. Đã hợp nhất vào `main`. Không cần kiểm tích hợp với team khác để tính là xong.
