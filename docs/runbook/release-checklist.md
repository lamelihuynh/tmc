# Checklist nộp bài M2

Người chịu trách nhiệm: **lead Team D**. Làm theo thứ tự, đánh dấu từng mục, **không đánh dấu mục chưa kiểm tra thật**.

## T-5 ngày: đóng băng mã (mốc M-4)

- [ ] Mọi story mức `B` trong `docs/team/WORK-BREAKDOWN.md` xong, hoặc có quyết định cắt được ghi trong `MEETING-CADENCE.md`
- [ ] `@Disabled` còn lại đều thuộc mức `N` hoặc `T`, và được liệt kê trong báo cáo ở mục "Hạn chế"
- [ ] Từ giờ chỉ nhận PR sửa lỗi và tài liệu

## T-3 ngày: kiểm chứng trên máy sạch

Làm trên một máy (hoặc thư mục) **chưa từng build repo**, theo đúng `docs/runbook/local-setup.md`.

- [ ] `git clone` repo, `make build` xanh
- [ ] `make local-up`, `make demo-online`, `make local-restart`, `make demo-offline`, `make local-restart`, `make demo-timeout` đều in `... demo passed`
- [ ] `make up` rồi `make integration` xanh (cần Docker)
- [ ] Chạy 3 demo lại với stack Docker đầy đủ
- [ ] `cd apps/web-portal && npm ci && npm run lint && npm test && npm run build` xanh
- [ ] CI trên GitHub xanh ở commit sẽ gắn thẻ
- [ ] Số liệu thật: số test (tổng và bỏ qua), kết quả A-09 (p95, p99, số lượt vượt 2 giây) nếu đã đo, độ phủ nếu đã đo
- [ ] Chụp ảnh hoặc lưu đầu ra: `make build`, 3 demo, `make integration`

## T-2 ngày: báo cáo

- [ ] Có đủ các phần theo `docs/team/WORK-BREAKDOWN.md` mục 8
- [ ] **Task 1:** mô tả bối cảnh, yêu cầu chức năng và phi chức năng, đặc tính kiến trúc, so sánh và chọn kiểu kiến trúc, các bước tạo kiến trúc logic và vật lý, view module, view C&C, view phân bổ, quyết định kiến trúc, nguyên lý thiết kế
- [ ] **Mục "Issues Encountered":** một tiểu mục cho mỗi vấn đề (mất kết nối trong lúc xử lý, QR tĩnh bị lộ). Mỗi tiểu mục có: vấn đề, kịch bản lỗi, giải pháp, **cách giảm thiểu**, **cách đưa vào kiến trúc** (view nào đổi), đánh đổi, bằng chứng
- [ ] **SOLID:** mỗi nguyên tắc có ví dụ từ mã thật và test (từ `docs/architecture/solid-map.md`)
- [ ] **Phản ánh:** SOLID cải thiện thiết kế thế nào, khó khăn gì, tính module, bảo trì, mở rộng
- [ ] **Task 2:** nêu module đã hiện thực (Attendance Event Recording tại biên cùng Gate Station và User/QR), cách chạy, kết quả test
- [ ] Mọi khẳng định về hành vi trỏ tới **một test hoặc một kịch bản demo**
- [ ] `docs/document.md` đã sửa theo `docs/adr/README.md` mục 3 (FR-19, NFR-05, NFR-09, UC-07, UC-08, AC-03, AC-04, component, sequence, deployment, API kiosk, ma trận truy vết)
- [ ] Hai lỗi của M1 đã sửa: header API HR (đang dùng nhầm token và ghi chú của Turnstile) và mô tả dòng `manual-open` (đang trùng mô tả heartbeat), thành phần điều phối (mục 15.2 so với mục 13)
- [ ] ADR-11, 12, 13 đã chuyển sang **Accepted** hoặc ghi rõ lý do còn Proposed; chỗ lệch giữa ADR và mã đã quyết định xong (`implementation-notes.md` mục 1)
- [ ] Mục "Hạn chế và việc tiếp theo" đúng với `implementation-notes.md` mục 3 đến 7, **không** khẳng định điều chưa đo (mục 7)
- [ ] Hình có số và chú thích, danh mục hình, tài liệu tham khảo; kiểm tra chính tả; xuất PDF; không còn dấu `TODO` hay `<điền>`

## T-1 ngày: kho mã

- [ ] `README.md` ở gốc cho người lạ: mục tiêu, kiến trúc, cách chạy, URL tài liệu
- [ ] Không có bí mật, khóa thật, dữ liệu cá nhân thật, tệp `.db`, `.local/`, `node_modules/`, `build/` trong repo (`git ls-files | grep -E '\.db$|\.pem$|\.env$'` rỗng)
- [ ] Giấy phép và danh sách thành viên (nếu giảng viên yêu cầu)
- [ ] Repo ở chế độ cho giảng viên truy cập được (công khai hoặc đã mời)
- [ ] Gắn thẻ `m2-submission` vào commit đã kiểm chứng

## Ngày nộp

- [ ] Mở URL GitHub trong cửa sổ ẩn danh (hoặc tài khoản khác) để chắc nó truy cập được
- [ ] Nộp trên LMS: **báo cáo chi tiết (PDF) chứa URL GitHub** (đề bài M2, mục Submission Instructions)
- [ ] Tải lại tệp vừa nộp và mở thử
- [ ] Ghi vào `MEETING-CADENCE.md` nhật ký quyết định: ngày giờ nộp, commit đã nộp
