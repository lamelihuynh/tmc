```text
THIẾT KẾ KIẾN TRÚC HỆ THỐNG KIỂM SOÁT RA VÀO VÀKHOA KHOA HỌC VÀ KỸ THUẬT MÁY TÍNHCHẤM CÔNG NHÂN VIÊN BẰNG MÃ QRĐẠI HỌC QUỐC GIA THÀNH PHỐ HỒ CHÍ MINHKIẾN TRÚC PHẦN MỀM (CO3017)TRƯỜNG ĐẠI HỌC BÁCH KHOAGiảng viên hướng dẫnSinh viên thực hiệnThành phố Hồ Chí Minh, tháng 10 - 2026:: Trương Văn Thảo Nhi - 2312514Dương Khả Vân - 2313866Hoàng Hữu Nhân - 2312419Trương Nguyễn Gia Bảo - 2310271Ngô Minh Thư - 2313384Nguyễn Chu Nguyên Chương - 2310365Võ Trần Minh Khoa - 2211664Huỳnh Nhật Linh - 2311847Lê Đình Thuận
```

<img src="images/page-001-image-01.png" alt="Page 1 image" width="164" />

### Danh sách thành viên

<img src="images/page-002-image-01.png" alt="Page 2 image" width="33" />

| STT | MSSV | Họ và tên | Nhiệm vụ | Đóng góp |
| --- | --- | --- | --- | --- |
| 1 | 2313866 | Dương Khả Vân | Phân tích bối cảnh, mục tiêu, phạm vi hệ thống và xác định các tác nhân (Actors). | 100% |
| 2 | 2313384 | Ngô Minh Thư | Phân tích và đặc tả yêu cầu chức năng (FR) và phi chức năng (NFR) của hệ thống. | 100% |
| 3 | 2312514 | Trương Văn Thảo Nhi | Xác định các đặc tính kiến trúc (Architecture Characteristics) và xây dựng các quyết định kiến trúc (ADR). | 100% |
| 4 | 2310365 | Nguyễn Chu Nguyên Chương | Phân tích và áp dụng các nguyên lý SOLID và nguyên tắc thiết kế vào kiến trúc hệ thống. | 100% |
| 5 | 2211664 | Võ Trần Minh Khoa | So sánh, lựa chọn phong cách kiến trúc (Architecture Styles) và xây dựng sơ đồ thành phần (Component Diagram). | 100% |
| 6 | 2312419 | Hoàng Hữu Nhân | Thiết kế sơ đồ tuần tự (Sequence Diagram) cho quy trình quét QR mở cổng và đồng bộ dữ liệu HR. | 100% |
| 7 | 2311847 | Huỳnh Nhật Linh | Thiết kế sơ đồ triển khai (Deployment Diagram) cho hai nhà máy và đặc tả API Turnstile. | 100% |
| 8 | 2310271 | Trương Nguyễn Gia Bảo | Thiết kế API tích hợp HR, tổng hợp nội dung, chuẩn hóa định dạng và hoàn thiện báo cáo. | 100% |

# Mục lục

### 1 Bối cảnh hệ thống

6

1.1

Giới thiệu doanh nghiệp . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

6

1.2

Mục tiêu và nhu cầu xây dựng hệ thống . . . . . . . . . . . . . . . . . . . . . . . . . .

6

1.3

Cách hệ thống vận hành ở mức tổng quát . . . . . . . . . . . . . . . . . . . . . . . . .

6

1.4

Phạm vi dự án . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

7

1.5

Các mốc và điều kiện vận hành quan trọng . . . . . . . . . . . . . . . . . . . . . . . .

7

1.6

Các bên liên quan . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

7

### 2 Mô tả các Actors

7

2.1

Employee. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

7

2.2

Production Supervisor . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

8

2.3

HR Staff . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

8

### 2.4 Security Guard

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 8 2.5

System Administrator . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

8

2.6

Executive Management. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

9

2.7

Bảng tóm tắt actors. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

9

### 3 Hệ thống ngoài, giả định và ràng buộc

9

3.1

Hệ thống ngoài. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

9

3.2

Ràng buộc . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

9

3.3

Giả định . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

10

### 4 Yêu cầu chức năng và phi chức năng của hệ thống

11

4.1

Use Case Diagram tổng quan . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

11

### 4.2 Yêu cầu chức năng

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 11

4.2.1 Nhóm User & QR Code Management. . . . . . . . . . . . . . . . . . . . . .

11

4.2.2 Nhóm Gate Station Management . . . . . . . . . . . . . . . . . . . . . . . . .

12

### 4.2.3 Nhóm Attendance Event Recording

. . . . . . . . . . . . . . . . . . . . . . . 12

4.2.4 Nhóm Attendance Reporting & Auditing . . . . . . . . . . . . . . . . . . . . .

13

4.2.5 HR/Payroll Integration. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

13

4.2.6 System Administration & Monitoring. . . . . . . . . . . . . . . . . . . . . .

14

### 4.3 Yêu cầu phi chức năng

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 14

### 5 Kịch bản Use Case

16

5.1

UC-01: Đăng ký nhân viên và cấp QR. . . . . . . . . . . . . . . . . . . . . . . . . .

16

### 5.2 UC-02: Thu hồi / làm mới QR

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 16

5.3

UC-03: Vô hiệu hóa nhân viên nghỉ việc. . . . . . . . . . . . . . . . . . . . . . . . .

17

### 5.4 UC-04: Đăng ký trạm cổng và gán với cổng xoay

. . . . . . . . . . . . . . . . . . . . 17

5.5

UC-05: Giám sát trạng thái thiết bị và cảnh báo. . . . . . . . . . . . . . . . . . . . .

18

5.6

UC-06: Quét thủ công khi QR hoặc thiết bị lỗi . . . . . . . . . . . . . . . . . . . . . .

18

5.7

UC-07: QR động có thời hạn. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

19

5.8

UC-08: Quét QR vào/ra . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

19

5.9

UC-09: Đồng bộ dữ liệu chấm công sang HR/Payroll. . . . . . . . . . . . . . . . . .

21

5.10 UC-10: Xem báo cáo chấm công. . . . . . . . . . . . . . . . . . . . . . . . . . . . .

5.11 UC-11: Xem audit log . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

5.12 UC-12: Quản lý tài khoản và phân quyền . . . . . . . . . . . . . . . . . . . . . . . . .

5.13 UC-13: Cấu hình hệ thống và sao lưu . . . . . . . . . . . . . . . . . . . . . . . . . . .

<img src="images/page-004-image-01.png" alt="Page 4 image" width="30" />

*6 Đặc tính kiến trúc (Architecture Characteristics) 25 6.1 Danh mục đặc tính kiến trúc*

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 25

6.2

Đặc tính dẫn dắt . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

26

7 Quyết định kiến trúc (Architecture Decisions) 27 7.1 Tổng hợp các quyết định kiến trúc

. . . . . . . . . . . . . . . . . . . . . . . . . . . . 27

7.2

Chi tiết các quyết định . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

27

7.2.1 ADR-01: Sử dụng kiến trúc Microservices . . . . . . . . . . . . . . . . . . . .

27

7.2.2 ADR-02: Sử dụng Event-Driven Architecture. . . . . . . . . . . . . . . . . .

28

7.2.3 ADR-03: Attendance Service là System of Record . . . . . . . . . . . . . . . .

28

7.2.4 ADR-04: Transactional Outbox + Message Broker . . . . . . . . . . . . . . . .

29

7.2.5 ADR-05: Idempotency cho attendance event . . . . . . . . . . . . . . . . . . .

29

7.2.6 ADR-06: Tích hợp Turnstile thông qua Adapter / Anti-Corruption Layer. . . .

29

7.2.7 ADR-07: Tích hợp HR/Payroll thông qua Integration Service . . . . . . . . . .

30

7.2.8 ADR-08: Centralized Identity + RBAC + Audit Logging. . . . . . . . . . . .

30

7.2.9 ADR-09: Privacy by Design. . . . . . . . . . . . . . . . . . . . . . . . . . .

31

7.2.10 ADR-10: Database per Service và Read Model cho Reporting . . . . . . . . . .

31

8 Nguyên lý thiết kế (Design Principles) và áp dụng SOLID 33 8.1

Áp dụng 5 nguyên tắc SOLID . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

33

8.1.1 S – Single Responsibility Principle (Nguyên tắc đơn trách nhiệm). . . . . . .

33

8.1.2 O – Open/Closed Principle (Nguyên tắc đóng/mở) . . . . . . . . . . . . . . . .

33

8.1.3 L – Liskov Substitution Principle (Nguyên tắc thay thế Liskov) . . . . . . . . .

33

8.1.4 I – Interface Segregation Principle (Nguyên tắc phân tách giao diện). . . . . .

33

8.1.5 D – Dependency Inversion Principle (Nguyên tắc đảo ngược phụ thuộc). . . .

34

8.2

Các nguyên lý thiết kế bổ sung. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

34

9 Ma trận truy vết (Traceability Matrix) 35 10 Lựa chọn kiến trúc chủ đạo

36

10.1 Cơ sở lựa chọn kiến trúc . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

36

10.2 So sánh các kiến trúc ứng viên. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

36

10.3 Kiến trúc Microservices kết hợp Event Driven . . . . . . . . . . . . . . . . . . . . . .

37

10.4 Hệ quả và đánh đổi của lựa chọn. . . . . . . . . . . . . . . . . . . . . . . . . . . . .

37

### 11 Tổng quan kiến trúc và phân rã logic

38

### 11.1 Các bước hình thành kiến trúc logic

. . . . . . . . . . . . . . . . . . . . . . . . . . . 38

11.2 Catalog thành phần và quyền sở hữu dữ liệu. . . . . . . . . . . . . . . . . . . . . . .

38

11.3 Quy tắc tổ chức module trong từng service . . . . . . . . . . . . . . . . . . . . . . . .

39

### 12 Góc nhìn thành phần và kết nối

40

12.1 Sơ đồ Component tổng quan. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

40

### 12.2 Catalog interface

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 41

12.3 Các kiểu kết nối và cơ sở lựa chọn. . . . . . . . . . . . . . . . . . . . . . . . . . . .

42

12.4 Component của luồng quét QR và điều khiển cổng . . . . . . . . . . . . . . . . . . . .

43

12.5 Component của xử lý sự kiện và tích hợp HR . . . . . . . . . . . . . . . . . . . . . . .

12.6 Danh mục sự kiện và dữ liệu trao đổi . . . . . . . . . . . . . . . . . . . . . . . . . . .

12.7 Tính nhất quán và xử lý ngoại lệ. . . . . . . . . . . . . . . . . . . . . . . . . . . . .

12.8 Truy vết yêu cầu và tiêu chí kiểm chứng. . . . . . . . . . . . . . . . . . . . . . . . .

### 13 Mô hình hóa luồng tương tác nghiệp vụ

<img src="images/page-005-image-01.png" alt="Page 5 image" width="30" />

13.1 Luồng quét QR mở cổng. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 47 13.1.1 Mục tiêu và phạm vi xử lý. . . . . . . . . . . . . . . . . . . . . . . . . . . . 47 13.1.2 Các thành phần kiến trúc và chức năng . . . . . . . . . . . . . . . . . . . . . . 47 13.1.3 Mối quan hệ giữa các thành phần . . . . . . . . . . . . . . . . . . . . . . . . . 48 13.1.4 Sequence diagram . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 49 13.1.5 Bảng đặc tả luồng xử lý . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 50 13.2 Sequence diagram \- Luồng đẩy dữ liệu HR . . . . . . . . . . . . . . . . . . . . . . . . 51 13.2.1 Mục tiêu và phạm vi xử lý. . . . . . . . . . . . . . . . . . . . . . . . . . . . 51 13.2.2 Các thành phần kiến trúc và chức năng . . . . . . . . . . . . . . . . . . . . . . 51 13.2.3 Mối quan hệ giữa các thành phần . . . . . . . . . . . . . . . . . . . . . . . . . 52 13.2.4 Sequence diagram . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 52 13.2.5 Bảng đặc tả luồng xử lý . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 54

14 Quy trình từng bước xây dựng Kiến trúc Vật lý và Sơ đồ triển khai 56

14.1 Bước 1 \- Xác định các ràng buộc vật lý, vị trí địa lý và đặc tính \- AC. . . . . . . . . . 56 14.1.1 Phân bố địa lý:. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 56 14.1.2 Các đặc tính kiến trúc (Architecture Characteristics) định kiến trúc:. . . . . . 56 14.2 Bước 2 \- Quyết định mô hình phân bổ địa lý \- Kiến trúc lai giữa trung tâm và biên . . . 56 14.3 Bước 3 \- Ánh xạ các thành phần Logic sang nút vật lý và đơn vị triển khai. . . . . . . 57 14.4 Bước 4 \- Thiết kế phân vùng mạng và giao thức mạng (Protocol). . . . . . . . . . . . 57 14.5 Bước 5 \- Thiết kế cơ chế sẵn sàng cao (High Availability), sao chép dữ liệu (Replication) và chịu lỗi offline đa tầng (Multi-Tier Offline Resilience) cho hệ thống . . . . . . . . . 57 14.6 Bước 6 \- Định cỡ phần cứng và hoạch định dung lượng. . . . . . . . . . . . . . . . . 58 14.7 Bước 7: Kiểm chứng kiến trúc vật lý với các fitness Functions . . . . . . . . . . . . . . 58 15 Thiết kế chi tiết sơ đồ triển khai (Deployment View) 59 15.1 Sơ đồ triển khai Deployment View tổng quan. . . . . . . . . . . . . . . . . . . . . . 59 15.1.1 Khối người dùng truy cập từ Internet:. . . . . . . . . . . . . . . . . . . . . . 59 15.1.2 Khối Central site: . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 59 15.1.3 Khối HR/Payroll:. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 60 15.1.4 Khối 2 nhà máy \- Factory Bình Dương và Long An: . . . . . . . . . . . . . . . 60 15.2 Sơ đồ triển khai Deployment View chi tiết tại một nhà máy. . . . . . . . . . . . . . . 61 16 Thiết kế chi tiết tích hợp API

64

16.1 Kiến trúc tích hợp tổng thể. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 64 16.2 Thiết kế API nội bộ tại biên giữa Kiosk App và Edge Gateway. . . . . . . . . . . . . 64 16.3 Thiết kế API tích hợp Turnstile Control System. . . . . . . . . . . . . . . . . . . . . 66 16.3.1 Ngữ cảnh tích hợp và ràng buộc kỹ thuật . . . . . . . . . . . . . . . . . . . . . 66 16.3.2 Danh mục endpoint của Turnstile Control System API . . . . . . . . . . . . . . 67 16.3.3 Đặc tả API contract cho POST /gates/{gate_id}/open. . . . . . . . . . . . . . 67 16.4 Thiết kế chi tiết API tích hợp HR/Payroll System . . . . . . . . . . . . . . . . . . . . . 70 16.4.1 Ngữ cảnh tích hợp và ràng buộc SLA . . . . . . . . . . . . . . . . . . . . . . . 70 16.4.2 Danh mục các API tích hợp giữa TMC và HR/Payroll System . . . . . . . . . . 71 16.4.3 Đặc tả API Contract cho POST /api/v1/attendance-batches. . . . . . . . . . . 71

### 17 Tài liệu tham khảo

# Danh sách hình vẽ

4.1.1

Sơ đồ use case tổng quan hệ thống TMC. . . . . . . . . . . . . . . . . . . . . . 11 12.1.1

Tổng quan kiến trúc Microservices và Event-Driven của TMC . . . . . . . . . . . 41 12.4.1

Component của luồng quét QR và điều khiển cổng . . . . . . . . . . . . . . . . . 43 12.5.1

Component của xử lý sự kiện và tích hợp HR . . . . . . . . . . . . . . . . . . . . 44 13.1.1

Sequence diagram của luồng quét QR mở cổng. . . . . . . . . . . . . . . . . . . 49 13.2.1

Sequence diagram của luồng đồng bộ HR và đối soát. . . . . . . . . . . . . . . 53 15.1.1

Sơ đồ triển khai Deployment View tổng quan. . . . . . . . . . . . . . . . . . . . 59 15.2.1

Sơ đồ triển khai chi tiết tại một nhà máy. . . . . . . . . . . . . . . . . . . . . . 61 16.1.1

Kiến trúc tích hợp tổng thể theo mô hình Hexagonal. . . . . . . . . . . . . . . . 64 16.3.1

Sequence Diagram luồng quét QR tại biên (Edge). . . . . . . . . . . . . . . . . 70

<img src="images/page-007-image-01.png" alt="Page 7 image" width="30" />

# 1 Bối cảnh hệ thống

TMC Manufacturing Joint Stock Company (gọi tắt là TMC) cần một hệ thống dùng mã QR để kiểm soát nhân viên ra vào hai nhà máy và tự động ghi nhận chấm công, thay cho việc đối soát thủ công cuối tháng.

### 1.1 Giới thiệu doanh nghiệp

TMC hoạt động trong lĩnh vực sản xuất và hiện có hai nhà máy:

| Nhà máy | Quy mô nhân sự | Ghi chú |
| --- | --- | --- |
| Bình Dương | khoảng 800 nhân viên |  |
| Long An | khoảng 600 nhân viên |  |
| Tổng | khoảng 1.400 nhân viên | Mỗi nhân viên có điện thoại thông minh cá nhân |

### 1.2 Mục tiêu và nhu cầu xây dựng hệ thống

Hệ thống kiểm soát ra vào và chấm công bằng mã QR được xây dựng nhằm tự động hóa việc xác thực nhân viên tại các cổng nhà máy TMC, ghi nhận chính xác các lượt ra vào và hỗ trợ đối soát dữ liệu chấm công hằng tháng. Hệ thống hướng tới các mục tiêu cốt lõi:

- Kiểm soát ra vào nghiêm ngặt: Chỉ cho phép nhân viên ở trạng thái hoạt động, sở hữu mã QR hợp lệ và đủ quyền truy cập được phép đi qua cổng.

- Tự động hóa chấm công: Ghi nhận chính xác và tự động các thông tin giao dịch (thời điểm, định danh nhân viên, nhà máy, cổng và chiều ra/vào) để phục vụ đối soát hằng tháng.

- Giảm thiểu rủi ro và sai sót: Hạn chế tối đa việc ghi nhận/đối soát chấm công thủ công, đồng thời hỗ trợ phát hiện các trường hợp bất thường (quét lặp, đi trễ, về sớm).

- Hỗ trợ vận hành và ra quyết định: Cung cấp dữ liệu quản lý minh bạch cho các bên liên quan (HR Staff, Production Supervisor, Executive Management).

Tích hợp liền mạch: Đảm bảo khả năng giao tiếp và trao đổi dữ liệu với HR/Payroll System và Turnstile Control System qua API.

Với quy mô khoảng 1.400 nhân viên, việc kiểm soát ra vào và đối soát chấm công thủ công hoặc rời rạc dễ gây sai sót, khó truy vết và chậm đối soát.

Hệ thống mới dùng mã QR hiển thị trên điện thoại của nhân viên (qua ứng dụng di động, cổng web hoặc email). Việc này không đòi hỏi phát thẻ vật lý và tận dụng thiết bị nhân viên đã có.

### 1.3 Cách hệ thống vận hành ở mức tổng quát

1. Nhân viên đưa mã QR vào đầu quét tại cổng (kiosk).

2. Hệ thống xác thực mã QR và quyền ra vào của nhân viên.

3. Nếu hợp lệ, hệ thống gửi lệnh mở cổng đến hệ thống điều khiển cổng xoay (Turnstile Control System) qua API.

4. Mỗi lượt vào hoặc ra được lưu thành một giao dịch chấm công.

5. Cuối tháng, dữ liệu chấm công được chuyển sang hệ thống HR/Payroll qua API để đối soát.

<img src="images/page-008-image-01.png" alt="Page 8 image" width="30" />

### 1.4 Phạm vi dự án

| Trong phạm vi | Ngoài phạm vi |
| --- | --- |
| Quản lý hồ sơ và vòng đời trạng thái nhân viên | Quản lý khách đến thăm nhà máy |
| Cấp phát, hiển thị, làm mới và thu hồi mã QR định danh | Tính lương, khấu trừ thuế, phụ cấp và trả lương |
| Quản lý danh mục nhà máy, cổng, kiosk và thiết bị | Thay thế hệ thống quản trị nhân sự HR/Payroll System |
| Xác thực QR và gọi API điều khiển mở/đóng cổng xoay | Thay thế hệ thống điều khiển cổng Turnstile Control System |
| Ghi nhận lượt vào/ra tự động và xử lý ngoại lệ (mở thủ công) | Quản lý toàn bộ quy trình tuyển dụng và đào tạo nhân sự |
| Chống quét lặp (Anti-passback) và phát cảnh báo an ninh | Chấm công bằng sinh trắc học (nhận diện khuôn mặt, vân tay) |
| Báo cáo, đối soát và xuất/đồng bộ dữ liệu chấm công | Tính toán các công thức bảng lương trực tiếp trong hệ thống TMC |
| Quản lý tài khoản, phân quyền (RBAC), audit log, cấu hình hệ thống | Theo dõi định vị GPS hoặc vị trí bên ngoài phạm vi cổng nhà máy |
| Giám sát trạng thái hoạt động của thiết bị và xử lý sự cố | Các nghiệp vụ nhân sự khác chưa được xác định trong tài liệu yêu cầu |

### 1.5 Các mốc và điều kiện vận hành quan trọng

<table>
<tbody>
<tr><td>Điều kiện</td><td>Giá trị theo đề bài</td><td>Ý nghĩa với thiết kế</td></tr>
<tr><td>Hạn dữ liệu chấm công</td><td>Sẵn sàng đối soát trước 12:00 trưa ngày 1<br />
hằng tháng</td><td>Cần cơ chế đồng bộ có kiểm soát và xử lý lỗi, không<br />
thể làm vào phút chót</td></tr>
<tr><td>Tải giờ cao điểm</td><td>Khoảng 800 giao dịch trong 30 phút mỗi<br />
nhà máy (đổi ca)</td><td>Trung bình khoảng 27 giao dịch/phút (0,44 giao<br />
dịch/giây), nhưng tải thực tế dồn thành đợt nên cần<br />
dư địa cao hơn mức trung bình</td></tr>
<tr><td>Thời gian phản hồi tại kiosk</td><td>&lt;= 2 giây</td><td>Quyết định không để việc xử lý nặng nằm trên đường<br />
đi của lượt quét</td></tr>
<tr><td>Hệ thống có sẵn</td><td>HR/Payroll và Turnstile Control System,<br />
đều có API</td><td>TMC phải tích hợp, không thay thế các hệ thống này</td></tr>
</tbody>
</table>

### 1.6 Các bên liên quan

Hệ thống phục vụ 6 nhóm người dùng (Employee, Production Supervisor, HR Staff, Security Guard, System Administrator, Executive Management) và giao tiếp với 2 hệ thống ngoài. Mô tả chi tiết ở mục 2 và 3.

## 2 Mô tả các Actors 2.1 Employee

- Mô tả: công nhân và nhân viên của hai nhà máy, dùng mã QR trên điện thoại để ra vào và chấm công.

- Mục tiêu: đi qua cổng nhanh, không cần thẻ vật lý; lượt ra vào được ghi nhận đúng để không bị sai công.

- Chức năng chính: nhận và hiển thị QR qua mobile app, web portal hoặc email; làm mới QR khi mất điện thoại; quét QR tại kiosk để mở cổng; xem lịch sử ra vào của chính mình.

```text
•    Quyền hạn: chỉ xem và dùng dữ liệu của chính mình; không xem dữ liệu nhân viên khác;
     không thay đổi cấu hình hay dữ liệu chấm công.
```

<img src="images/page-009-image-01.png" alt="Page 9 image" width="30" />

### 2.2 Production Supervisor

- Mô tả: quản lý trực tiếp một nhóm hoặc một ca sản xuất, chịu trách nhiệm quân số trong ca.

- Mục tiêu: biết ai đã vào ca, ai vắng, ai đi trễ để điều phối sản xuất.

```text
•    Chức năng chính: xem danh sách có mặt và vắng của nhóm mình theo thời gian thực;
     xem báo cáo chấm công của nhóm; ghi chú hoặc xác nhận các trường hợp bất thường.
```

- Quyền hạn: chỉ xem dữ liệu của nhân viên thuộc nhóm hoặc ca mình quản lý; không tạo, thu hồi QR hay sửa dữ liệu gốc.

### 2.3 HR Staff

- Mô tả: bộ phận nhân sự chịu trách nhiệm hồ sơ nhân viên và dữ liệu chấm công.

- Mục tiêu: hồ sơ nhân viên luôn đúng; dữ liệu chấm công đầy đủ, đối soát được và chuyển sang HR/Payroll trước 12:00 ngày 1 hằng tháng.

```text
•    Chức năng chính: đăng ký, cập nhật, vô hiệu hóa nhân viên; cấp, thu hồi, làm mới QR;
     xử lý và hiệu chỉnh ngoại lệ chấm công; đối soát và xuất báo cáo.
```

- Quyền hạn: quản lý hồ sơ và QR của nhân viên ở cả hai nhà máy; xem và xuất dữ liệu chấm công; mọi hiệu chỉnh đều có audit log; không có quyền cấu hình thiết bị hay phân quyền hệ thống.

### 2.4 Security Guard

- Mô tả: nhân viên an ninh trực tại cổng, được phân công theo nhà máy và cổng.

- Mục tiêu: xử lý nhanh các trường hợp ngoại lệ tại cổng để không gây ùn tắc và vẫn giữ được kiểm soát ra vào.

- Chức năng chính: xử lý quét thủ công khi QR hoặc thiết bị lỗi; xem trạng thái thiết bị tại cổng mình phụ trách; nhận cảnh báo anti-passback; tra cứu nhanh trạng thái nhân viên.

- Quyền hạn: chỉ thao tác tại cổng mình được phân công; chỉ được mở cổng thủ công kèm lý do bắt buộc và được ghi log; không xem báo cáo chấm công, không sửa hồ sơ.

### 2.5 System Administrator

- Mô tả: người vận hành kỹ thuật toàn hệ thống.

- Mục tiêu: hệ thống chạy ổn định, thiết bị hoạt động, tài khoản và quyền truy cập đúng quy định.

```text
•     Chức năng chính: khai báo nhà máy, cổng, kiosk và gán với turnstile; giám sát thiết bị;
     quản lý tài khoản và phân quyền; cấu hình hệ thống và sao lưu; xem audit log.
```

- Quyền hạn: toàn quyền cấu hình kỹ thuật và quản lý tài khoản; không mặc định được xem dữ liệu chấm công chi tiết của nhân viên; thao tác của quản trị viên cũng bị ghi log.

<img src="images/page-010-image-01.png" alt="Page 10 image" width="30" />

### 2.6 Executive Management

- Mô tả: lãnh đạo công ty, dùng hệ thống để giám sát và ra quyết định.

- Mục tiêu: nắm tình hình chuyên cần và vận hành của hai nhà máy mà không cần xử lý dữ liệu chi tiết.

- Chức năng chính: xem báo cáo tổng quan và dashboard (tỷ lệ có mặt, đi trễ, lưu lượng ra vào theo nhà máy); xuất báo cáo tổng hợp.

- Quyền hạn: chỉ đọc; chỉ xem dữ liệu tổng hợp; không sửa dữ liệu và không xem thông tin cá nhân chi tiết nếu không cần thiết.

### 2.7 Bảng tóm tắt actors

<table>
<tbody>
<tr><td>Actor</td><td>Nhà máy truy cập</td><td>Phạm vi dữ liệu</td><td>Loại quyền</td></tr>
<tr><td>Employee</td><td>Cả hai</td><td>Chỉ dữ liệu của mình</td><td>Dùng QR, xem cá nhân</td></tr>
<tr><td>Production<br />
Supervisor</td><td>Theo nhóm quản lý</td><td>Nhân viên trong nhóm hoặc ca</td><td>Xem</td></tr>
<tr><td>HR Staff</td><td>Cả hai</td><td>Toàn bộ nhân viên</td><td>Quản lý hồ sơ, QR, chấm<br />
công</td></tr>
<tr><td>Security Guard</td><td>Theo cổng được phân công</td><td>Cổng của mình</td><td>Xử lý ngoại lệ tại cổng</td></tr>
<tr><td>System<br />
Administrator</td><td>Cả hai</td><td>Cấu hình, tài khoản, log</td><td>Quản trị kỹ thuật</td></tr>
<tr><td>Executive<br />
Management</td><td>Cả hai</td><td>Dữ liệu tổng hợp</td><td>Chỉ đọc</td></tr>
</tbody>
</table>

# 3 Hệ thống ngoài, giả định và ràng buộc

TMC tích hợp với hai hệ thống đã có sẵn và không thay thế chúng. Phần này chỉ giới thiệu ở mức bối cảnh; thiết kế API chi tiết thuộc phần Physical Views và API Integration.

### 3.1 Hệ thống ngoài

| Hệ thống | Vai trò | TMC trao đổi gì | Ràng buộc chính |
| --- | --- | --- | --- |
| HR/Payroll System | Hệ thống nhân sự và tính lương hiện có của công ty | TMC gửi dữ liệu chấm công sang qua API; có thể nhận danh sách nhân viên | Dữ liệu sẵn sàng để đối soát trước 12:00 ngày 1 hằng tháng; TMC chỉ xuất dữ liệu, không tính lương |
| Turnstile Control System | Hệ thống điều khiển cổng xoay hiện có | TMC gọi API để mở hoặc đóng cổng cho từng lượt ra vào | Phải dùng API sẵn có; thời gian gọi API nằm trong ngân sách 2 giây của mỗi lượt quét |

### 3.2 Ràng buộc

- RB-01: dữ liệu chấm công phải được truyền sang HR/Payroll qua API và sẵn sàng đối soát trước 12:00 ngày 1 hằng tháng.

- RB-02: việc điều khiển cổng phải thông qua API của Turnstile Control System hiện có.

<img src="images/page-011-image-01.png" alt="Page 11 image" width="30" />

- RB-03: thời gian phản hồi tại kiosk không quá 2 giây ở tải đỉnh khoảng 800 giao dịch trong 30 phút mỗi nhà máy.

- RB-04: không mất bản ghi chấm công khi hệ thống vận hành bình thường.

- RB-05: phải có RBAC, audit log đầy đủ và bảo vệ dữ liệu cá nhân theo quy định hiện hành.

- RB-06: ngoài phạm vi giai đoạn này: quản lý khách và tính lương.

### 3.3 Giả định

- A-01: mỗi nhân viên có smartphone dùng được app, web hoặc email của công ty và hiển thị được QR.

- A-02: mỗi cổng ra vào có một trạm gồm đầu quét QR và kết nối được tới controller của cổng xoay.

- A-03: mạng nội bộ tại nhà máy nhìn chung ổn định, nhưng có thể gián đoạn ngắn; hệ thống phải chịu được tình huống này (xem các NFR về tính sẵn sàng và chế độ dự phòng).

- A-04: danh sách nhân viên gốc (mã nhân viên, họ tên, đơn vị) nằm ở HR/Payroll; TMC có thể đồng bộ về thay vì nhập tay.

- A-05: API của HR/Payroll và Turnstile đã ổn định, có tài liệu và môi trường thử nghiệm.

- A-06: mỗi nhân viên thuộc một nhà máy chính, nhưng có thể được phép ra vào nhà máy còn lại.

- A-07: nhân viên chỉ có một mã nhân viên duy nhất trong cả hai nhà máy.

<img src="images/page-012-image-01.png" alt="Page 12 image" width="30" />

<table>
<tbody>
<tr><td>4</td><td>Yêu cầu chức năng và phi chức năng của hệ thống</td></tr>
<tr><td>4.1</td><td>Use Case Diagram tổng quan</td></tr>
</tbody>
</table>

<img src="images/page-012-image-02.png" alt="Page 12 image" width="653" />

*Hình 4.1.1 Sơ đồ use case tổng quan hệ thống TMC*

<table>
<tbody>
<tr><td>4.2</td><td>Yêu cầu chức năng</td></tr>
<tr><td>4.2.1</td><td>Nhóm User &amp; QR Code Management</td></tr>
</tbody>
</table>

Nhóm này quản lý vòng đời của nhân viên và mã QR, từ lúc đăng ký đến lúc nghỉ việc.

| ID | Tên chức năng | Mô tả chi tiết | Actor(s) | Độ ưu tiên |
| --- | --- | --- | --- | --- |
| FR-01 | Đăng ký hồ sơ nhân viên | Tạo hồ sơ nhân viên gồm mã nhân viên, họ tên, đơn vị, nhà máy chính và trạng thái hoạt động. Mã nhân viên là duy nhất. | HR Staff | Cao |
| FR-02 | Cập nhật hồ sơ nhân viên | Sửa thông tin hồ sơ (đơn vị, nhà máy, ca làm việc). Mọi thay đổi được ghi audit log. | HR Staff | Trung bình |

<img src="images/page-013-image-01.png" alt="Page 13 image" width="30" />

| ID | Tên chức năng | Mô tả chi tiết | Actor(s) | Độ ưu tiên |
| --- | --- | --- | --- | --- |
| FR-03 | Vô hiệu hóa nhân viên nghỉ việc | Đổi trạng thái nhân viên sang ngưng hoạt động; hệ thống tự thu hồi QR và từ chối mọi lượt quét sau thời điểm hiệu lực. Giữ nguyên lịch sử chấm công. | HR Staff, HR/Payroll System | Cao |
| FR-04 | Cấp định danh QR | Gán một định danh QR duy nhất cho nhân viên đang hoạt động khi đăng ký. Mỗi nhân viên chỉ có một định danh QR hiệu lực tại một thời điểm. | HR Staff, System Administrator | Cao |
| FR-05 | Hiển thị QR cho nhân viên | Nhân viên xem mã QR của mình qua mobile app, web portal hoặc nhận qua email, sau khi đăng nhập hoặc xác thực. | Employee | Cao |
| FR-06 | Thu hồi QR | Vô hiệu hóa QR hiện tại ngay lập tức (mất điện thoại, nghi ngờ lộ mã, nghỉ việc). Cổng từ chối QR đã thu hồi trong lần quét kế tiếp. | HR Staff, System Administrator | Cao |
| FR-07 | Làm mới QR | Cấp định danh QR mới thay thế QR cũ; QR cũ bị vô hiệu hóa tự động. Nhân viên yêu cầu qua app hoặc HR thực hiện thay. | Employee, HR Staff | Cao |
| FR-08 | Đồng bộ danh sách nhân viên từ HR/ Payroll | Nhập hoặc đồng bộ định kỳ danh sách nhân viên và thay đổi trạng thái từ HR/Payroll để tránh nhập tay (phụ thuộc giả định A-04). | HR Staff, HR/Payroll System | Trung bình |
| FR-09 | Tra cứu nhân viên và trạng thái QR | Tìm nhân viên theo mã, tên, đơn vị; xem trạng thái hoạt động và trạng thái QR. Security Guard chỉ xem thông tin tối thiểu. | HR Staff, Security Guard | Trung bình |
| FR-10 | Gán nhà máy và ca làm việc | Gán nhân viên vào nhà máy (Bình Dương hoặc Long An), nhóm và ca; Production Supervisor xem được nhóm mình. | HR Staff, Production Supervisor | Trung bình |

### 4.2.2 Nhóm Gate Station Management

Nhóm này quản lý các trạm cổng (gồm kiosk và đầu quét QR) ở từng nhà máy và liên kết chúng với cổng xoay trong Turnstile Control System.

| ID | Tên chức năng | Mô tả chi tiết | Actor(s) | Độ ưu tiên |
| --- | --- | --- | --- | --- |
| FR-11 | Khai báo nhà máy và cổng | Tạo và quản lý danh mục nhà máy (Bình Dương, Long An) và các cổng ra vào của từng nhà máy. | System Administrator | Cao |
| FR-12 | Đăng ký trạm cổng và gán với cổng xoay | Đăng ký trạm cổng (kiosk, đầu quét) và gán với một cổng xoay tương ứng trong Turnstile Control System. Mỗi lượt quét tại trạm chỉ mở đúng cổng đã gán. | System Administrator, Turnstile Control System | Cao |
| FR-13 | Cấu hình hướng và chế độ cổng | Cấu hình cổng là chiều vào, chiều ra hoặc hai chiều, để phân loại đúng giao dịch vào hoặc ra. | System Administrator | Cao |
| FR-14 | Giám sát trạng thái thiết bị | Hiển thị trạng thái từng trạm (đang hoạt động, mất kết nối, lỗi) dựa trên tín hiệu định kỳ từ thiết bị. Bảo vệ chỉ xem cổng mình phụ trách. | System Administrator, Security Guard | Cao |
| FR-15 | Cảnh báo thiết bị lỗi hoặc mất kết nối | Tự động gửi cảnh báo khi trạm mất tín hiệu quá ngưỡng cấu hình, để xử lý kịp thời và chuyển sang quy trình thủ công. | System Administrator, Security Guard | Trung bình |
| FR-16 | Bật, tắt trạm cổng để bảo trì | Đặt trạm vào chế độ bảo trì hoặc ngưng hoạt động; thay đổi được ghi audit log và hiển thị rõ cho bảo vệ. | System Administrator | Trung bình |

### 4.2.3 Nhóm Attendance Event Recording

Nhóm này xử lý việc tiếp nhận lượt quét QR, xác thực thông tin ra vào và ghi nhận sự kiện chấm công tại các cổng của nhà máy. Nhóm bảo đảm mỗi sự kiện được phân loại chính xác, lưu lại để tra cứu và không bị ghi nhận trùng khi xảy ra lỗi xử lý.

<img src="images/page-014-image-01.png" alt="Page 14 image" width="30" />

| ID | Tên chức năng | Mô tả chức năng | Actor(s) | Ưu tiên |
| --- | --- | --- | --- | --- |
| FR-17 | Kiểm soát ra vào và ghi nhận chấm công bằng QR | Hệ thống xác thực mã QR của nhân viên tại trạm. Nếu hợp lệ, tự động gọi API của Turnstile Control System để mở cổng và lưu thành một giao dịch chấm công. Các mã không hợp lệ sẽ bị từ chối trực tiếp tại trạm. | Turnstile Control System | Cao |
| FR-18 | Chống quét lặp (Anti- passback) | Hệ thống tự động ngăn chặn một nhân viên vào hoặc ra hai lần liên tiếp. Khi phát hiện vi phạm, hệ thống từ chối lệnh mở cổng, lưu sự kiện vi phạm và gửi cảnh báo ngay lập tức đến màn hình trực của bảo vệ. | Security Guard | Trung bình |
| FR-19 | Sinh và quản lý QR động | Hệ thống tự động sinh mã QR định danh cho nhân viên trên mobile app/web portal. Mã QR này là chuỗi token ngẫu nhiên (chứa thời gian tạo, mã nhân viên và chữ ký số/token bảo mật), tự động thay đổi sau mỗi khoảng thời gian ngắn (đề xuất 30 \- 60 giây, có thể cấu hình được) và chỉ có giá trị sử dụng một lần. | Employee, System Administrator | Cao |
| FR-20 | Xử lý ra vào ngoại lệ | Cho phép bảo vệ xác minh nhân sự và điều khiển mở cổng thủ công qua hệ thống khi xảy ra sự cố thiết bị hoặc lỗi QR. Thao tác này bắt buộc nhập lý do, đánh dấu giao dịch là ngoại lệ và được ghi nhận vào audit log. | Security Guard, Turnstile Control System | Cao |

### 4.2.4 Nhóm Attendance Reporting & Auditing

Nhóm này phục vụ việc tra cứu lịch sử, theo dõi tình hình hiện diện, báo cáo, đối soát dữ liệu và kiểm toán. Quyền truy cập dữ liệu được giới hạn theo vai trò và phạm vi nghiệp vụ.

| ID | Tên chức năng | Mô tả chức năng | Actor(s) | Ưu tiên |
| --- | --- | --- | --- | --- |
| FR-21 | Tra cứu lịch sử chấm công | Cho phép tra cứu lịch sử ra vào và dữ liệu chấm công. | Employee, Production Supervisor, HR Staff | Cao |
| FR-22 | Theo dõi tình hình hiện diện | Hiển thị danh sách nhân viên có mặt, vắng mặt theo nhóm/ca dựa trên dữ liệu sự kiện thời gian thực. | Production Supervisor | Cao |
| FR-23 | Xác nhận ngoại lệ chấm công | Cho phép quản lý sản xuất ghi chú hoặc xác nhận các bất thường (đi trễ, về sớm) của nhân sự trong nhóm. | Production Supervisor | Cao |
| FR-24 | Báo cáo chấm công theo nhóm/nhà máy | Tổng hợp dữ liệu chấm công theo ca, nhóm, nhà máy và khoảng thời gian để theo dõi tình hình chuyên cần. | Production Supervisor, HR Staff | Cao |
| FR-25 | Dashboard báo cáo tổng quan | Hiển thị chỉ số tổng hợp (tỷ lệ có mặt, đi trễ, lưu lượng ra vào) theo nhà máy. | Executive Management | Trung bình |
| FR-26 | Đối soát và xử lý sai lệch | Cho phép kiểm tra và xử lý các bản ghi chấm công thiếu, trùng hoặc bất thường kèm lý do. | HR Staff | Cao |
| FR-27 | Tra cứu nhật ký kiểm toán (Audit Log) | Tra cứu log về thay đổi hồ sơ, cấp/thu hồi QR, phân quyền, cấu hình và xử lý ngoại lệ, sai lệch. Không ai có quyền xóa/sửa log. | System Administrator | Cao |
| FR-28 | Xuất dữ liệu và báo cáo chấm công | Cho phép xuất file báo cáo theo dữ liệu được giới hạn theo vai trò | HR Staff, Production Supervisor, Executive Management | Trung bình |

### 4.2.5 HR/Payroll Integration

Nhóm này quản lý việc trao đổi dữ liệu giữa hệ thống chấm công của TMC và hệ thống HR/Payroll hiện có. Hệ thống không thay thế HR/Payroll và không thực hiện tính lương.

<table>
<tbody>
<tr><td>ID</td><td>Tên chức năng</td><td>Mô tả chức năng</td><td>Actor(s)</td><td>Ưu tiên</td></tr>
<tr><td>FR-29</td><td>Chuyển dữ liệu sang<br />
HR/Payroll</td><td>Tổng hợp và gửi dữ liệu chấm công qua API của HR/<br />
Payroll theo kỳ đối soát. Ghi nhận trạng thái truyền<br />
tải, kết quả và lỗi trả về để phục vụ đối soát định kỳ.</td><td>HR/Payroll System</td><td>Cao</td></tr>
</tbody>
</table>

<img src="images/page-015-image-01.png" alt="Page 15 image" width="30" />

<table>
<tbody>
<tr><td>ID</td><td>Tên chức năng</td><td>Mô tả chức năng</td><td>Actor(s)</td><td>Ưu tiên</td></tr>
<tr><td>FR-30</td><td>Giám sát và xử lý lỗi<br />
tích hợp</td><td>Theo dõi trạng thái đồng bộ API (chiều đi và về). Cho<br />
phép xử lý kỹ thuật và trigger gửi lại dữ liệu khi lỗi,<br />
có cơ chế kiểm soát chống trùng lặp dữ liệu.</td><td>System Administrator</td><td>Cao</td></tr>
</tbody>
</table>

### 4.2.6 System Administration & Monitoring

Nhóm này quản lý tài khoản, quyền truy cập, cấu hình chung, giám sát hệ thống và xử lý sự cố kỹ thuật.

| ID | Tên chức năng | Mô tả chức năng | Actor(s) | Ưu tiên |
| --- | --- | --- | --- | --- |
| FR-31 | Quản lý tài khoản và phân quyền | Tạo, cập nhật, vô hiệu hóa tài khoản quản trị. Phân quyền truy cập (RBAC) và phạm vi dữ liệu. Mọi thay đổi đều sinh audit log. | System Administrator | Cao |
| FR-32 | Quản lý cấu hình tham số hệ thống | Cấu hình các tham số vận hành chung (ngưỡng cảnh báo, chu kỳ đồng bộ API, hạn mức thời gian QR động). | System Administrator | Trung bình |
| FR-33 | Giám sát trạng thái hệ thống | Hiển thị trạng thái hoạt động của server, database, và kết nối tích hợp API (Turnstile, HR/Payroll). Tự động phát cảnh báo khi mất kết nối diện rộng. | System Administrator | Cao |
| FR-34 | Theo dõi và xử lý sự cố | Tra cứu log lỗi kỹ thuật (Error log), xác định nguyên nhân sự cố và thực hiện các thao tác khắc phục kỹ thuật được cấp phép. | System Administrator | Cao |
| FR-35 | Sao lưu và khôi phục | Quản lý tiến trình sao lưu dữ liệu hệ thống. Hỗ trợ khôi phục khi có sự cố và kiểm tra tính toàn vẹn của dữ liệu sau khôi phục. | System Administrator | Trung bình |

### 4.3 Yêu cầu phi chức năng

| ID | Phân loại | Mô tả chi tiết | Tiêu chí đo lường / Ràng buộc |
| --- | --- | --- | --- |
| NFR-01 | Bảo mật: kiểm soát truy cập (RBAC) | Mọi chức năng và API phải kiểm tra quyền ở phía server theo vai trò (6 actors) và phạm vi dữ liệu (nhà máy, nhóm, cổng). Áp dụng nguyên tắc tối thiểu quyền. | 100% chức năng quản trị và API có kiểm tra quyền. Truy cập vượt quyền bị từ chối (HTTP 403) và ghi log. Kiểm thử ma trận quyền đạt 100%. |
| NFR-02 | Bảo mật: audit log | Ghi lại lịch sử thao tác (ai, làm gì, lúc nào, từ đâu, kết quả) cho đăng nhập, thay đổi hồ sơ, cấp/thu hồi QR, mở cổng thủ công, cấu hình, v.v. Log chỉ được thêm, không ai sửa hoặc xóa. | 100% thao tác thuộc danh mục có log. Không tài khoản nào xóa/sửa được log. Lưu trữ tối thiểu 12 tháng. Truy vấn log trả kết quả ≤ 5 giây. |
| NFR-03 | Bảo mật: xác thực và phiên | Tài khoản quản trị phải xác thực mạnh; phiên đăng nhập tự hết hạn khi không hoạt động. | Bắt buộc MFA cho System Administrator và HR Staff. Khóa tạm tài khoản sau 5 lần đăng nhập sai. Hết hạn phiên sau 15 phút không hoạt động. |
| NFR-04 | Quyền riêng tư: bảo vệ dữ liệu cá nhân | Tuân thủ luật bảo vệ dữ liệu cá nhân. Thu thập tối thiểu (mã nhân viên, họ tên, đơn vị, nhà máy, thời điểm ra vào; không thu sinh trắc học/GPS), mã hóa dữ liệu. | 100% kết nối dùng TLS 1.2 trở lên. Dữ liệu cá nhân mã hóa khi lưu (AES- 256). Không có trường sinh trắc học hoặc vị trí trong cơ sở dữ liệu. |
| NFR-05 | Bảo mật: chống giả mạo QR | Mã QR không chứa thông tin cá nhân đọc được, là token ngẫu nhiên không đoán được, QR hết hạn/đã dùng/thu hồi phải bị từ chối. | 100% QR hết hạn, đã dùng hoặc bị thu hồi bị từ chối. Token QR tối thiểu 128 bit hoặc có chữ ký số. Kiểm tra token nằm trong ngân sách 2 giây. |
| NFR-06 | Hiệu năng: thời gian phản hồi tại kiosk | Hệ thống phải đảm bảo thời gian xử lý và phản hồi nhanh chóng tại trạm cổng trong giờ cao điểm. | Thời gian phản hồi tại kiosk \<= 2 giây ở tải đỉnh khoảng 800 giao dịch trong 30 phút mỗi nhà máy. |

<img src="images/page-016-image-01.png" alt="Page 16 image" width="38" />

| ID | Phân loại | Mô tả chi tiết | Tiêu chí đo lường / Ràng buộc |
| --- | --- | --- | --- |
| NFR-07 | Độ tin cậy: tính toàn vẹn dữ liệu | Bảo đảm không bị mất mát dữ liệu sự kiện ra vào và chấm công của nhân viên trong quá trình vận hành. | 100% giao dịch quét QR hợp lệ được ghi nhận chính xác; không mất bản ghi chấm công trong điều kiện vận hành bình thường. |
| NFR-08 | Vận hành: hạn đồng bộ dữ liệu | Dữ liệu chấm công phải được tổng hợp và chuyển sang hệ thống HR/Payroll đúng thời hạn để phục vụ tính lương. | Dữ liệu chấm công hoàn tất truyền tải và sẵn sàng đối soát trước 12:00 trưa ngày 1 hằng tháng. |
| NFR-09 | Tính sẵn sàng & Dự phòng mất mạng | Hệ thống phải duy trì hoạt động liên tục và có cơ chế xử lý khi mất kết nối mạng nội bộ/internet ngắn hạn. | Trạm kiosk tích hợp cơ chế lưu trữ cục bộ (offline queue) khi mất kết nối và tự động đồng bộ lại dữ liệu về server khi mạng khôi phục. |
| NFR-10 | Sao lưu & Khôi phục dữ liệu | Thực hiện sao lưu định kỳ cơ sở dữ liệu hệ thống để phòng ngừa rủi ro sự cố phần cứng hoặc mất dữ liệu. | Có lịch sao lưu tự động hàng ngày/hàng tuần, hỗ trợ khôi phục dữ liệu nhanh chóng và kiểm tra tính toàn vẹn sau khi restore. |

<img src="images/page-017-image-01.png" alt="Page 17 image" width="38" />

<table>
<tbody>
<tr><td>5</td><td>Kịch bản Use Case</td></tr>
<tr><td>5.1</td><td>UC-01: Đăng ký nhân viên và cấp QR</td></tr>
</tbody>
</table>

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-01</td></tr>
<tr><td>Use-case name</td><td>Đăng ký nhân viên và cấp QR</td></tr>
<tr><td>Actor</td><td>HR Staff (phụ: Employee, HR/Payroll System)</td></tr>
<tr><td>Description</td><td>HR Staff tạo hồ sơ nhân viên mới; hệ thống cấp định danh QR để nhân viên dùng ra vào và chấm<br />
công.</td></tr>
<tr><td>Trigger</td><td>Có nhân viên mới nhận việc hoặc nhân viên chưa có hồ sơ trong hệ thống.</td></tr>
<tr><td>Precondition</td><td>HR Staff đã đăng nhập và có quyền quản lý hồ sơ. Mã nhân viên chưa tồn tại trong hệ thống.</td></tr>
<tr><td>Postcondition</td><td>Hồ sơ nhân viên ở trạng thái hoạt động. Một định danh QR hiệu lực được gán. Nhân viên xem được<br />
QR qua app, web hoặc email. Thao tác được ghi audit log.</td></tr>
<tr><td>Normal flow</td><td>1. HR Staff chọn &quot;Đăng ký nhân viên&quot;và nhập mã nhân viên, họ tên, đơn vị, nhà máy chính, ca làm<br />
việc.<br />
2. Hệ thống kiểm tra dữ liệu hợp lệ và mã nhân viên không trùng.<br />
3. Hệ thống lưu hồ sơ ở trạng thái hoạt động.<br />
4. Hệ thống cấp định danh QR duy nhất cho nhân viên.<br />
5. Hệ thống thông báo cho nhân viên rằng QR đã sẵn sàng.<br />
6. Hệ thống ghi audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 1:<br />
1.1 HR Staff chọn nhập từ HR/Payroll thay vì nhập tay.<br />
1.2 Hệ thống lấy dữ liệu nhân viên từ HR/Payroll System, rồi tiếp tục từ bước 2.<br />
Tại bước 5:<br />
5.1 Nhân viên chưa cài app: hệ thống gửi QR hoặc liên kết xem QR qua email.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 2:<br />
2.1 Mã nhân viên đã tồn tại hoặc thiếu dữ liệu bắt buộc: hệ thống từ chối và chỉ rõ trường lỗi.<br />
2.2 HR Staff sửa dữ liệu hoặc hủy thao tác.<br />
Tại bước 4:<br />
4.1 Không cấp được QR: hồ sơ vẫn được lưu và đánh dấu &quot;chưa có QR&quot;.<br />
4.2 Hệ thống cảnh báo HR Staff để thử cấp lại.</td></tr>
</tbody>
</table>

### 5.2 UC-02: Thu hồi / làm mới QR

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-02</td></tr>
<tr><td>Use-case name</td><td>Thu hồi / làm mới QR</td></tr>
<tr><td>Actor</td><td>HR Staff, Employee</td></tr>
<tr><td>Description</td><td>Vô hiệu hóa QR hiện tại khi nhân viên mất điện thoại hoặc nghi ngờ lộ mã, và cấp QR mới thay thế.</td></tr>
<tr><td>Trigger</td><td>Nhân viên báo mất điện thoại, nghi ngờ lộ QR, hoặc yêu cầu làm mới QR qua app.</td></tr>
<tr><td>Precondition</td><td>Nhân viên đang hoạt động và có QR hiệu lực. Người thực hiện (HR Staff hoặc chính nhân viên) đã<br />
đăng nhập.</td></tr>
<tr><td>Postcondition</td><td>QR cũ bị vô hiệu hóa, QR mới có hiệu lực. Mọi lượt quét bằng QR cũ bị từ chối. Thao tác được ghi<br />
audit log.</td></tr>
<tr><td>Normal flow</td><td>1. Người dùng chọn &quot;Làm mới QR&quot;(Employee qua app, hoặc HR Staff thực hiện thay).<br />
2. Hệ thống xác thực người yêu cầu.<br />
3. Hệ thống vô hiệu hóa QR hiện tại ngay lập tức.<br />
4. Hệ thống cấp QR mới cho nhân viên.<br />
5. Hệ thống hiển thị hoặc gửi QR mới cho nhân viên.<br />
6. Hệ thống ghi audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 1:<br />
1.1 HR Staff chọn &quot;Thu hồi&quot;mà không cấp mới (ví dụ nghi ngờ gian lận).<br />
1.2 Hệ thống thực hiện bước 3, không cấp QR mới và đặt nhân viên ở trạng thái &quot;tạm khóa QR&quot;cho<br />
đến khi HR Staff cấp lại.</td></tr>
</tbody>
</table>

<img src="images/page-018-image-01.png" alt="Page 18 image" width="38" />

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-02</td></tr>
<tr><td>Exception flows</td><td>Tại bước 2:<br />
2.1 Xác thực thất bại: hệ thống từ chối và ghi log.<br />
2.2 Sau 5 lần sai liên tiếp, tài khoản bị khóa tạm.<br />
Tại bước 4:<br />
4.1 Không cấp được QR mới: QR cũ vẫn bị vô hiệu hóa.<br />
4.2 Hệ thống báo lỗi để thử lại; trong thời gian chờ, nhân viên được xử lý theo UC-06.</td></tr>
</tbody>
</table>

### 5.3 UC-03: Vô hiệu hóa nhân viên nghỉ việc

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-03</td></tr>
<tr><td>Use-case name</td><td>Vô hiệu hóa nhân viên nghỉ việc</td></tr>
<tr><td>Actor</td><td>HR Staff (phụ: HR/Payroll System)</td></tr>
<tr><td>Description</td><td>Chuyển nhân viên sang trạng thái ngưng hoạt động, tự thu hồi QR, vẫn giữ nguyên lịch sử chấm công.</td></tr>
<tr><td>Trigger</td><td>Nhân viên nghỉ việc, hoặc HR/Payroll System báo thay đổi trạng thái.</td></tr>
<tr><td>Precondition</td><td>HR Staff đã đăng nhập và có quyền quản lý hồ sơ. Nhân viên đang ở trạng thái hoạt động.</td></tr>
<tr><td>Postcondition</td><td>Nhân viên ở trạng thái ngưng hoạt động, QR bị thu hồi, mọi lượt quét sau thời điểm hiệu lực bị từ<br />
chối. Lịch sử chấm công được giữ. Thao tác được ghi audit log.</td></tr>
<tr><td>Normal flow</td><td>1. HR Staff chọn nhân viên và chọn &quot;Vô hiệu hóa&quot;.<br />
2. HR Staff nhập ngày hiệu lực và lý do.<br />
3. Hệ thống yêu cầu xác nhận.<br />
4. Đến thời điểm hiệu lực, hệ thống chuyển nhân viên sang ngưng hoạt động.<br />
5. Hệ thống tự thu hồi QR của nhân viên.<br />
6. Hệ thống giữ nguyên lịch sử chấm công.<br />
7. Hệ thống ghi audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 1:<br />
1.1 Thay đổi trạng thái đến từ HR/Payroll System: hệ thống nhận trạng thái mới.<br />
1.2 Hệ thống tiếp tục từ bước 4.<br />
Tại bước 2:<br />
2.1 Ngày hiệu lực ở tương lai: hệ thống lên lịch.<br />
2.2 Khi đến hạn, hệ thống tự thực hiện bước 4 và 5.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 1:<br />
1.1 Nhân viên không tồn tại hoặc đã ngưng hoạt động: hệ thống thông báo và kết thúc.<br />
Tại bước 4:<br />
4.1 Nhân viên đã vào nhà máy nhưng chưa có lượt ra: hệ thống vẫn vô hiệu hóa và cảnh báo HR Staff.<br />
4.2 Lượt ra của nhân viên được xử lý theo UC-06.</td></tr>
</tbody>
</table>

### 5.4 UC-04: Đăng ký trạm cổng và gán với cổng xoay

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-04</td></tr>
<tr><td>Use-case name</td><td>Đăng ký trạm cổng và gán với cổng xoay</td></tr>
<tr><td>Actor</td><td>System Administrator (phụ: Turnstile Control System)</td></tr>
<tr><td>Description</td><td>Đăng ký trạm cổng (kiosk và đầu quét QR) tại một nhà máy, gán với đúng một cổng xoay và cấu hình<br />
chiều vào, ra.</td></tr>
<tr><td>Trigger</td><td>Lắp đặt cổng hoặc kiosk mới, hoặc thay thiết bị.</td></tr>
<tr><td>Precondition</td><td>System Administrator đã đăng nhập. Nhà máy đã được khai báo. Cổng xoay tồn tại trong Turnstile<br />
Control System.</td></tr>
<tr><td>Postcondition</td><td>Trạm cổng được đăng ký, gán đúng một cổng xoay, cấu hình chiều và ở trạng thái hoạt động (hoặc bảo<br />
trì). Thao tác được ghi audit log.</td></tr>
</tbody>
</table>

<img src="images/page-019-image-01.png" alt="Page 19 image" width="38" />

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-04</td></tr>
<tr><td>Normal flow</td><td>1. System Administrator chọn nhà máy và chọn &quot;Thêm trạm cổng&quot;.<br />
2. System Administrator nhập mã trạm, tên, vị trí và mã kiosk, đầu quét.<br />
3. System Administrator chọn cổng xoay trong Turnstile Control System để gán cho trạm.<br />
4. System Administrator chọn chiều hoạt động: vào, ra hoặc hai chiều.<br />
5. Hệ thống gọi thử API của Turnstile Control System để kiểm tra kết nối.<br />
6. Hệ thống lưu và kích hoạt trạm.<br />
7. Hệ thống ghi audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 3:<br />
3.1 Cổng xoay chưa có trong danh mục: System Administrator khai báo cổng mới rồi quay lại bước 3.<br />
Tại bước 6:<br />
6.1 Hệ thống kích hoạt trạm ở chế độ bảo trì: trạm chưa nhận lượt quét.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 3:<br />
3.1 Cổng xoay đã được gán cho trạm khác: hệ thống từ chối.<br />
3.2 System Administrator chọn cổng khác hoặc gỡ gán cũ.<br />
Tại bước 5:<br />
5.1 Không kết nối được Turnstile Control System: hệ thống báo lỗi và không kích hoạt trạm.<br />
5.2 Hệ thống cho lưu nháp để thử lại sau.</td></tr>
</tbody>
</table>

### 5.5 UC-05: Giám sát trạng thái thiết bị và cảnh báo

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-05</td></tr>
<tr><td>Use-case name</td><td>Giám sát trạng thái thiết bị và cảnh báo</td></tr>
<tr><td>Actor</td><td>System Administrator, Security Guard</td></tr>
<tr><td>Description</td><td>Theo dõi trạng thái các trạm cổng và nhận cảnh báo khi thiết bị lỗi hoặc mất kết nối.</td></tr>
<tr><td>Trigger</td><td>Trạm gửi tín hiệu định kỳ, hoặc mất tín hiệu quá ngưỡng cấu hình.</td></tr>
<tr><td>Precondition</td><td>Trạm đã đăng ký và đang hoạt động. Người dùng đã đăng nhập; Security Guard chỉ xem cổng được<br />
phân công.</td></tr>
<tr><td>Postcondition</td><td>Trạng thái thiết bị được cập nhật. Khi có sự cố, cảnh báo được gửi và lưu lại.</td></tr>
<tr><td>Normal flow</td><td>1. Mỗi trạm cổng gửi tín hiệu trạng thái định kỳ về hệ thống.<br />
2. Hệ thống cập nhật trạng thái trạm (hoạt động, mất kết nối, lỗi).<br />
3. System Administrator hoặc Security Guard mở màn hình giám sát.<br />
4. Hệ thống hiển thị trạng thái các trạm trong phạm vi quyền của người xem.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 2:<br />
2.1 Trạm mất tín hiệu quá ngưỡng cấu hình hoặc báo lỗi thiết bị: hệ thống đổi trạng thái sang &quot;mất kết<br />
nối&quot;hoặc &quot;lỗi&quot;.<br />
2.2 Hệ thống gửi cảnh báo cho System Administrator và Security Guard của cổng đó.<br />
2.3 Security Guard chuyển sang xử lý thủ công theo UC-06.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 2:<br />
2.1 Gửi cảnh báo thất bại: hệ thống hiển thị cảnh báo trên màn hình giám sát và ghi log.<br />
Tại bước 4:<br />
4.1 Security Guard xem cổng không được phân công: hệ thống từ chối và ghi log.</td></tr>
</tbody>
</table>

### 5.6 UC-06: Quét thủ công khi QR hoặc thiết bị lỗi

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-06</td></tr>
<tr><td>Use-case name</td><td>Quét thủ công khi QR hoặc thiết bị lỗi</td></tr>
<tr><td>Actor</td><td>Security Guard (phụ: Employee, Turnstile Control System)</td></tr>
<tr><td>Description</td><td>Security Guard xác minh nhân viên và mở cổng thủ công khi nhân viên không quét được QR hoặc<br />
kiosk lỗi. Giao dịch được đánh dấu là thủ công.</td></tr>
<tr><td>Trigger</td><td>Nhân viên không quét được (QR lỗi, điện thoại hỏng hoặc hết pin) hoặc kiosk lỗi, mất kết nối.</td></tr>
<tr><td>Precondition</td><td>Security Guard đã đăng nhập và được phân công tại cổng. Nhân viên đang ở trạng thái hoạt động. API<br />
của Turnstile Control System khả dụng.</td></tr>
<tr><td>Postcondition</td><td>Cổng mở một lượt. Giao dịch chấm công được ghi dạng thủ công kèm lý do và người xử lý. Thao tác<br />
được ghi audit log.</td></tr>
</tbody>
</table>

<img src="images/page-020-image-01.png" alt="Page 20 image" width="38" />

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-06</td></tr>
<tr><td>Normal flow</td><td>1. Nhân viên báo với Security Guard là không quét được.<br />
2. Security Guard tra nhân viên theo mã hoặc họ tên.<br />
3. Security Guard xác minh danh tính nhân viên theo quy trình của công ty.<br />
4. Hệ thống kiểm tra nhân viên đang hoạt động.<br />
5. Security Guard chọn hướng vào hoặc ra và nhập lý do (bắt buộc).<br />
6. Hệ thống gọi API của Turnstile Control System để mở cổng một lượt.<br />
7. Hệ thống ghi giao dịch chấm công dạng thủ công và audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 5:<br />
5.1 Nhiều nhân viên cùng gặp lỗi do kiosk hỏng: Security Guard xử lý lần lượt từng người.<br />
5.2 Lý do &quot;Thiết bị lỗi&quot;được dùng lại cho các lượt tiếp theo.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 2:<br />
2.1 Không tìm thấy nhân viên: hệ thống từ chối và hướng dẫn liên hệ HR Staff.<br />
Tại bước 4:<br />
4.1 Nhân viên đã ngưng hoạt động hoặc QR bị thu hồi: hệ thống từ chối, không mở cổng.<br />
Tại bước 6:<br />
6.1 API của Turnstile Control System không phản hồi: hệ thống báo lỗi.<br />
6.2 Security Guard mở cổng bằng cơ chế dự phòng của Turnstile Control System và ghi nhận lượt thủ<br />
công khi hệ thống phục hồi.</td></tr>
</tbody>
</table>

### 5.7 UC-07: QR động có thời hạn

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-07</td></tr>
<tr><td>Use-case name</td><td>QR động có thời hạn</td></tr>
<tr><td>Actor</td><td>Employee (phụ: System Administrator)</td></tr>
<tr><td>Description</td><td>QR trên mobile app hoặc web portal tự đổi sau một chu kỳ ngắn và chỉ dùng được một lần, để chống<br />
chụp màn hình hoặc chuyển QR cho người khác.</td></tr>
<tr><td>Trigger</td><td>Nhân viên mở app hoặc web để lấy QR.</td></tr>
<tr><td>Precondition</td><td>Nhân viên đang hoạt động, đã được cấp QR và đã đăng nhập vào app hoặc web.</td></tr>
<tr><td>Postcondition</td><td>QR hiện hành được hiển thị và tự hết hạn. QR hết hạn hoặc đã dùng bị từ chối khi quét.</td></tr>
<tr><td>Normal flow</td><td>1. Employee mở mobile app hoặc web portal và đăng nhập.<br />
2. Hệ thống xác thực nhân viên.<br />
3. Hệ thống sinh mã QR có thời hạn gắn với nhân viên (đề xuất 30 đến 60 giây, cấu hình được).<br />
4. App hiển thị QR kèm đếm ngược thời gian còn lại.<br />
5. Khi hết hạn, hệ thống tự sinh QR mới thay thế.<br />
6. Khi QR được quét tại kiosk, hệ thống chỉ chấp nhận mã còn hạn và chưa dùng, rồi đánh dấu mã đã<br />
dùng.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 1:<br />
1.1 Nhân viên chọn nhận QR qua email: hệ thống gửi QR tĩnh có hạn theo ngày thay cho QR động<br />
(xem lưu ý tại mục 6).<br />
Tại bước 3:<br />
3.1 System Administrator thay đổi chu kỳ hiệu lực: giá trị mới áp dụng cho các QR sinh sau đó.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 5:<br />
5.1 Điện thoại mất kết nối nên không làm mới được QR: app báo không thể làm mới.<br />
5.2 Nhân viên khôi phục kết nối hoặc nhờ Security Guard xử lý theo UC-06.<br />
Tại bước 6:<br />
6.1 QR hết hạn hoặc đã dùng: kiosk từ chối và hiển thị thông báo yêu cầu làm mới QR.<br />
6.2 Hệ thống ghi nhận sự kiện bị từ chối.</td></tr>
</tbody>
</table>

### 5.8 UC-08: Quét QR vào/ra

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-08</td></tr>
<tr><td>Use-case name</td><td>Quét QR vào/ra</td></tr>
<tr><td>Actor</td><td>Employee (phụ: Turnstile Control System, Security Guard)</td></tr>
</tbody>
</table>

<img src="images/page-021-image-01.png" alt="Page 21 image" width="38" />

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-08</td></tr>
<tr><td>Description</td><td>Nhân viên đưa QR vào đầu quét tại kiosk; hệ thống xác thực, mở đúng cổng xoay và ghi một giao dịch<br />
chấm công vào hoặc ra. Use case này gồm cả các ngoại lệ: QR hết hạn, quét trùng và mất mạng.</td></tr>
<tr><td>Trigger</td><td>Nhân viên đưa QR lên đầu quét tại kiosk.</td></tr>
<tr><td>Precondition</td><td>Nhân viên đang hoạt động và có QR hiệu lực (UC-07). Trạm cổng đã đăng ký và đang hoạt động<br />
(UC-04). Kiosk kết nối được máy chủ TMC (trường hợp mất mạng xem ngoại lệ 2.x).</td></tr>
<tr><td>Postcondition</td><td>Thành công: cổng mở một lượt; đúng một sự kiện chấm công được lưu (mã nhân viên, thời điểm, nhà<br />
máy, cổng, chiều vào/ra, nguồn QR). Thất bại: cổng không mở, lý do hiển thị tại kiosk, sự kiện bị từ<br />
chối được ghi nhận. Toàn bộ xử lý tại kiosk nằm trong 2 giây ở tải đỉnh.</td></tr>
<tr><td>Normal flow</td><td>1. Employee hiển thị QR trên app hoặc web portal và đưa vào đầu quét.<br />
2. Kiosk gửi token QR, mã trạm, thời điểm quét và mã giao dịch duy nhất đến hệ thống TMC.<br />
3. TMC xác thực chữ ký token, thời hạn, token chưa dùng và chưa bị thu hồi; kiểm tra nhân viên đang<br />
hoạt động và có quyền vào nhà máy này.<br />
4. TMC xác định chiều vào hoặc ra theo cấu hình cổng và kiểm tra quy tắc anti-passback.<br />
5. Nếu hợp lệ, TMC đánh dấu token đã dùng và gửi lệnh mở đúng cổng qua API của Turnstile Control<br />
System.<br />
6. Sau khi Turnstile Control System xác nhận, TMC lưu sự kiện chấm công.<br />
7. Kiosk thông báo kết quả; nhân viên đi qua cổng.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 1:<br />
1.1 Kiosk hoặc điện thoại lỗi: Security Guard xử lý theo UC-06.<br />
1.2 Nhân viên dùng QR nhận qua email: kiosk áp dụng thêm kiểm tra hạn theo ngày; các bước sau giữ<br />
nguyên.<br />
Tại bước 2:<br />
2.1 Nhân viên vô tình quét lại cùng một QR tại cùng kiosk trong khoảng thời gian ngắn (cấu hình<br />
được) sau khi đã thành công: hệ thống trả lại kết quả đã có, không mở cổng lần hai và không tạo sự<br />
kiện thứ hai.<br />
Tại bước 4:<br />
4.1 Cổng hai chiều: hệ thống xác định chiều theo lượt gần nhất của nhân viên (đang ở ngoài thì là vào,<br />
đang ở trong thì là ra).</td></tr>
<tr><td>Exception flows</td><td>Tại bước 2 (mất mạng):<br />
2.1 Kiosk mất kết nối tới máy chủ TMC: kiosk chuyển sang chế độ offline, kiểm tra chữ ký và hạn<br />
token cục bộ cùng danh sách thu hồi đã đồng bộ gần nhất. Nếu hợp lệ, kiosk mở cổng qua kết nối cục<br />
bộ tới controller cổng, lưu giao dịch vào hàng đợi cục bộ (đánh dấu &quot;offline&quot;) và thông báo cho nhân<br />
viên.<br />
2.2 Khi mạng khôi phục: kiosk tự đẩy hàng đợi theo thứ tự thời gian kèm mã giao dịch duy nhất; TMC<br />
kiểm tra lại (token dùng ở nhiều kiosk, anti-passback), bỏ qua giao dịch đã nhận và đánh dấu giao<br />
dịch bất thường để HR Staff xử lý ở UC-09.<br />
2.3 Kiosk không xác thực được khi offline (chưa có dữ liệu đồng bộ hoặc hàng đợi cục bộ lỗi, đầy):<br />
kiosk không mở cổng, báo lỗi và Security Guard xử lý theo UC-06; sự cố được cảnh báo theo UC-05.<br />
Tại bước 3:<br />
3.1 QR hết hạn: từ chối, kiosk báo &quot;QR hết hạn, hãy làm mới QR&quot;; sự kiện từ chối được ghi nhận.<br />
3.2 QR đã dùng (quét trùng khác với 2.1, chụp màn hình hoặc chuyển QR): từ chối, ghi nhận sự kiện<br />
bất thường; nhiều lần liên tiếp thì cảnh báo Security Guard.<br />
3.3 QR đã thu hồi hoặc token sai chữ ký: từ chối, không mở cổng, ghi nhận lý do.<br />
3.4 Nhân viên ngưng hoạt động hoặc không có quyền vào nhà máy này: từ chối, không mở cổng.<br />
Tại bước 4:<br />
4.1 Vi phạm anti-passback (vào hai lần liên tiếp hoặc ra hai lần liên tiếp): từ chối, lưu sự kiện vi phạm<br />
và cảnh báo ngay cho Security Guard. Nếu xác minh hợp lệ (ví dụ quên quét lượt trước), Security<br />
Guard xử lý theo UC-06 và HR Staff hiệu chỉnh ở UC-09.<br />
Tại bước 5:<br />
5.1 API Turnstile Control System timeout hoặc lỗi: không báo thành công; kiosk yêu cầu quét lại,<br />
token chưa bị đánh dấu đã dùng, sự kiện ghi trạng thái &quot;chưa xác định&quot;cho đến khi có kết quả xác<br />
nhận.<br />
Tại bước 6:<br />
6.1 Không lưu được sự kiện chấm công: không bỏ qua sự kiện; hệ thống đưa vào hàng đợi bền vững<br />
để ghi lại và chỉ xác nhận hoàn tất khi đã được lưu an toàn.</td></tr>
</tbody>
</table>

<img src="images/page-022-image-01.png" alt="Page 22 image" width="38" />

### 5.9 UC-09: Đồng bộ dữ liệu chấm công sang HR/Payroll

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-09</td></tr>
<tr><td>Use-case name</td><td>Đồng bộ dữ liệu chấm công sang HR/Payroll</td></tr>
<tr><td>Actor</td><td>HR Staff, System Administrator (phụ: HR/Payroll System)</td></tr>
<tr><td>Description</td><td>Hệ thống tổng hợp và gửi dữ liệu sự kiện chấm công sang HR/Payroll System theo định kỳ để phục<br />
vụ đối soát; hỗ trợ xử lý sai lệch, gửi lại an toàn và theo dõi trạng thái.</td></tr>
<tr><td>Trigger</td><td>Đến lịch đồng bộ định kỳ hoặc HR Staff yêu cầu đồng bộ.</td></tr>
<tr><td>Precondition</td><td>Sự kiện chấm công đã được lưu; dữ liệu cần đồng bộ nằm trong kỳ đối soát.</td></tr>
<tr><td>Postcondition</td><td>Thành công: dữ liệu được HR/Payroll tiếp nhận và trạng thái đồng bộ được cập nhật, sẵn sàng trước<br />
12:00 ngày 1 hằng tháng. Thất bại: bản ghi lỗi được giữ lại để xử lý, hệ thống ghi log và hỗ trợ gửi lại<br />
an toàn. Mọi hiệu chỉnh được ghi audit log.</td></tr>
<tr><td>Normal flow</td><td>1. Hệ thống tổng hợp các sự kiện chấm công trong kỳ cần đối soát.<br />
2. HR Staff kiểm tra báo cáo, phát hiện bản ghi thiếu, trùng hoặc ngoại lệ cần xác minh.<br />
3. HR Staff xử lý các ngoại lệ theo quyền hạn (kèm lý do) và hoàn tất bước đối soát.<br />
4. Hệ thống xác định tập dữ liệu đủ điều kiện gửi sang HR/Payroll.<br />
5. Hệ thống tạo lô đồng bộ có mã định danh, kỳ dữ liệu và thông tin kiểm tra.<br />
6. TMC gửi dữ liệu qua API của HR/Payroll System.<br />
7. HR/Payroll phản hồi trạng thái tiếp nhận hoặc danh sách lỗi theo giao ước API.<br />
8. TMC cập nhật trạng thái từng lô hoặc từng bản ghi, lưu log và thông báo kết quả cho HR Staff.<br />
9. HR Staff kiểm tra kết quả cuối cùng, bảo đảm dữ liệu sẵn sàng đối soát trước 12:00 ngày 1 hằng<br />
tháng.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 2:<br />
2.1 Đối soát phát hiện sai lệch: giữ lô ở trạng thái chờ xử lý cho tới khi HR Staff giải quyết theo quy<br />
trình được phê duyệt.<br />
Tại bước 6:<br />
6.1 Gửi lại lô đã tiếp nhận: dùng mã lô hoặc khóa chống lặp để không nhập dữ liệu trùng ở hệ thống<br />
đích.<br />
Tại bước 9:<br />
9.1 Gần đến hạn 12:00 ngày 1: cảnh báo HR Staff và System Administrator khi còn dữ liệu chưa gửi<br />
hoặc chưa được xác nhận.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 6:<br />
6.1 Mạng gián đoạn: giữ dữ liệu trong hàng đợi và thử lại theo chính sách cấu hình.<br />
Tại bước 7:<br />
7.1 HR/Payroll không phản hồi: đánh dấu lô hoặc bản ghi &quot;chưa xác định&quot;, không mặc định là thất bại<br />
hoàn toàn hay thành công.<br />
7.2 HR/Payroll trả về lỗi dữ liệu: lưu mã lỗi, xác định bản ghi liên quan và chuyển HR Staff xử lý.<br />
7.3 Một phần bản ghi bị lỗi: cập nhật trạng thái theo kết quả thực tế; chỉ gửi lại phần chưa được tiếp<br />
nhận nếu API hỗ trợ.</td></tr>
</tbody>
</table>

### 5.10 UC-10: Xem báo cáo chấm công

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-10</td></tr>
<tr><td>Use-case name</td><td>Xem báo cáo chấm công</td></tr>
<tr><td>Actor</td><td>Production Supervisor, HR Staff, Executive Management (phụ: Employee)</td></tr>
<tr><td>Description</td><td>Người dùng xem, lọc và xuất báo cáo chấm công trong phạm vi quyền của vai trò: Employee xem lịch<br />
sử của mình; Production Supervisor xem hiện diện và báo cáo của nhóm/ca; HR Staff xem toàn bộ;<br />
Executive Management xem dashboard tổng hợp.</td></tr>
<tr><td>Trigger</td><td>Người dùng mở chức năng báo cáo hoặc dashboard.</td></tr>
<tr><td>Precondition</td><td>Người dùng đã đăng nhập (HR Staff có MFA). Dữ liệu chấm công đã được ghi nhận.</td></tr>
<tr><td>Postcondition</td><td>Báo cáo hiển thị đúng phạm vi dữ liệu của vai trò. Việc xuất file được ghi audit log.</td></tr>
</tbody>
</table>

<img src="images/page-023-image-01.png" alt="Page 23 image" width="38" />

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-10</td></tr>
<tr><td>Normal flow</td><td>1. Người dùng chọn loại báo cáo (lịch sử cá nhân, hiện diện theo nhóm/ca, chấm công theo nhóm/nhà<br />
máy, dashboard tổng quan).<br />
2. Hệ thống xác định phạm vi dữ liệu theo vai trò và phạm vi được gán (RBAC).<br />
3. Người dùng chọn bộ lọc: nhà máy, nhóm, ca, khoảng thời gian.<br />
4. Hệ thống truy vấn và hiển thị kết quả (bảng hoặc biểu đồ) gồm giờ vào/ra, đi trễ, về sớm, vắng mặt<br />
và các giao dịch thủ công.<br />
5. Người dùng xem chi tiết hoặc ghi chú nếu có.<br />
6. Người dùng chọn xuất file; hệ thống xuất dữ liệu đúng phạm vi quyền và ghi audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 1:<br />
1.1 Production Supervisor xem danh sách có mặt và vắng mặt của nhóm theo thời gian thực.<br />
1.2 Production Supervisor ghi chú hoặc xác nhận bất thường (đi trễ, về sớm): hệ thống lưu ghi chú<br />
kèm người thực hiện và thời điểm, không sửa dữ liệu gốc, ghi audit log.<br />
1.3 Executive Management chỉ xem số liệu tổng hợp (tỷ lệ có mặt, đi trễ, lưu lượng ra vào theo nhà<br />
máy), không xem thông tin cá nhân chi tiết.<br />
Tại bước 6:<br />
6.1 Dữ liệu lớn: hệ thống xuất ở chế độ nền và thông báo khi file sẵn sàng.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 2:<br />
2.1 Người dùng yêu cầu dữ liệu ngoài phạm vi (ví dụ Employee xem người khác, Supervisor xem<br />
nhóm khác): hệ thống từ chối (403) và ghi log.<br />
Tại bước 4:<br />
4.1 Không có dữ liệu phù hợp bộ lọc: hệ thống thông báo kết quả rỗng.<br />
4.2 Truy vấn lỗi hoặc quá thời gian: hệ thống báo lỗi và cho phép thử lại.<br />
4.3 Một số kiosk đang offline và chưa đồng bộ: hệ thống hiển thị cảnh báo &quot;dữ liệu có thể chưa đầy<br />
đủ&quot;.<br />
Tại bước 6:<br />
6.2 Xuất file thất bại: hệ thống báo lỗi, không tạo file dở dang và cho phép thử lại.</td></tr>
</tbody>
</table>

### 5.11 UC-11: Xem audit log

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-11</td></tr>
<tr><td>Use-case name</td><td>Xem audit log</td></tr>
<tr><td>Actor</td><td>System Administrator</td></tr>
<tr><td>Description</td><td>Tra cứu nhật ký kiểm toán (ai, làm gì, lúc nào, từ đâu, kết quả) về thay đổi hồ sơ, cấp/thu hồi QR, mở<br />
cổng thủ công, phân quyền, cấu hình và xử lý đối soát. Log chỉ được xem, không ai sửa hoặc xóa.</td></tr>
<tr><td>Trigger</td><td>Cần điều tra sự cố, kiểm tra thao tác nhạy cảm hoặc kiểm toán định kỳ.</td></tr>
<tr><td>Precondition</td><td>System Administrator đã đăng nhập (có MFA).</td></tr>
<tr><td>Postcondition</td><td>Kết quả tra cứu được hiển thị; bản thân việc tra cứu cũng được ghi lại. Dữ liệu log không thay đổi.</td></tr>
<tr><td>Normal flow</td><td>1. System Administrator mở chức năng &quot;Audit log&quot;.<br />
2. System Administrator nhập bộ lọc: khoảng thời gian, người thực hiện, loại hành động, đối tượng<br />
tác động, kết quả.<br />
3. Hệ thống xác thực quyền và truy vấn log (kết quả trong 5 giây).<br />
4. Hệ thống hiển thị danh sách theo thứ tự thời gian, có phân trang.<br />
5. System Administrator chọn một bản ghi để xem chi tiết (giá trị trước và sau thay đổi, nguồn thao<br />
tác).<br />
6. Hệ thống ghi lại lượt tra cứu vào audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 2:<br />
2.1 System Administrator tra theo mã nhân viên hoặc mã giao dịch để truy vết một sự việc cụ thể.<br />
Tại bước 4:<br />
4.1 System Administrator xuất kết quả đã lọc; việc xuất được ghi log.</td></tr>
</tbody>
</table>

<img src="images/page-024-image-01.png" alt="Page 24 image" width="38" />

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-11</td></tr>
<tr><td>Exception flows</td><td>Tại bước 1:<br />
1.1 Người dùng không có quyền (ví dụ HR Staff, Security Guard): hệ thống từ chối (403) và ghi log.<br />
Tại bước 3:<br />
3.1 Không có bản ghi phù hợp: hệ thống thông báo kết quả rỗng.<br />
3.2 Truy vấn quá thời gian hoặc lỗi: hệ thống báo lỗi và gợi ý thu hẹp bộ lọc.<br />
Tại bước 5:<br />
5.1 Có yêu cầu sửa hoặc xóa log (qua giao diện hoặc API): hệ thống từ chối, không cung cấp thao tác<br />
này và ghi cảnh báo bảo mật.</td></tr>
</tbody>
</table>

### 5.12 UC-12: Quản lý tài khoản và phân quyền

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-12</td></tr>
<tr><td>Use-case name</td><td>Quản lý tài khoản và phân quyền</td></tr>
<tr><td>Actor</td><td>System Administrator</td></tr>
<tr><td>Description</td><td>Tạo, cập nhật, khóa hoặc vô hiệu hóa tài khoản người dùng quản trị và vận hành; gán vai trò (RBAC)<br />
và phạm vi dữ liệu (nhà máy, nhóm, cổng).</td></tr>
<tr><td>Trigger</td><td>Có người mới đảm nhận vai trò, thay đổi vai trò hoặc phạm vi, hoặc người dùng rời vị trí.</td></tr>
<tr><td>Precondition</td><td>System Administrator đã đăng nhập (có MFA).</td></tr>
<tr><td>Postcondition</td><td>Tài khoản và quyền được cập nhật và có hiệu lực; các phiên bị thay đổi quyền hoặc bị vô hiệu hóa<br />
được kết thúc. Thao tác được ghi audit log.</td></tr>
<tr><td>Normal flow</td><td>1. System Administrator chọn &quot;Quản lý tài khoản và phân quyền&quot;.<br />
2. System Administrator chọn &quot;Tạo tài khoản&quot;.<br />
3. System Administrator nhập thông tin: họ tên, mã nhân viên, email, nhà máy.<br />
4. System Administrator chọn vai trò (Production Supervisor, HR Staff, Security Guard, System<br />
Administrator, Executive Management).<br />
5. System Administrator chọn phạm vi dữ liệu: nhà máy, nhóm/ca (Production Supervisor) hoặc cổng<br />
(Security Guard).<br />
6. Hệ thống kiểm tra tài khoản không trùng và vai trò, phạm vi hợp lệ.<br />
7. Hệ thống lưu tài khoản và gửi hướng dẫn kích hoạt, thiết lập MFA.<br />
8. Hệ thống ghi audit log.</td></tr>
<tr><td>Alternative flows</td><td>Tại bước 2:<br />
2.1 Cập nhật tài khoản có sẵn: System Administrator đổi vai trò hoặc phạm vi; hệ thống áp dụng và<br />
kết thúc các phiên đang mở để quyền mới có hiệu lực ngay.<br />
2.2 Vô hiệu hóa tài khoản: hệ thống kết thúc mọi phiên đang mở, giữ nguyên lịch sử thao tác.<br />
2.3 Mở khóa tài khoản đang bị khóa tạm do đăng nhập sai 5 lần hoặc đặt lại MFA: hệ thống thực hiện<br />
sau khi xác minh và ghi log.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 3:<br />
3.1 Tài khoản đã tồn tại hoặc thiếu dữ liệu bắt buộc: hệ thống từ chối và chỉ rõ trường lỗi.<br />
Tại bước 5:<br />
5.1 Vai trò hoặc phạm vi không hợp lệ (ví dụ Security Guard không có cổng được gán): hệ thống từ<br />
chối.<br />
5.2 System Administrator tự gỡ quyền quản trị của mình hoặc vô hiệu hóa quản trị viên cuối cùng: hệ<br />
thống từ chối.<br />
Tại bước 7:<br />
7.1 Lưu thất bại: không có thay đổi nào được áp dụng, hệ thống báo lỗi.<br />
7.2 Gửi hướng dẫn kích hoạt thất bại: tài khoản ở trạng thái &quot;chờ kích hoạt&quot;và System Administrator<br />
gửi lại.</td></tr>
</tbody>
</table>

### 5.13 UC-13: Cấu hình hệ thống và sao lưu

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-13</td></tr>
<tr><td>Use-case name</td><td>Cấu hình hệ thống và sao lưu</td></tr>
<tr><td>Actor</td><td>System Administrator</td></tr>
</tbody>
</table>

<img src="images/page-025-image-01.png" alt="Page 25 image" width="38" />

<table>
<tbody>
<tr><td>Use-case ID</td><td>UC-13</td></tr>
<tr><td>Description</td><td>Điều chỉnh các tham số vận hành chung (ngưỡng cảnh báo mất tín hiệu, chu kỳ đồng bộ API, thời hạn<br />
QR động) và quản lý sao lưu, khôi phục dữ liệu hệ thống.</td></tr>
<tr><td>Trigger</td><td>Cần thay đổi tham số vận hành; đến lịch sao lưu; hoặc xảy ra sự cố cần khôi phục dữ liệu.</td></tr>
<tr><td>Precondition</td><td>System Administrator đã đăng nhập (có MFA).</td></tr>
<tr><td>Postcondition</td><td>Tham số mới có hiệu lực và giá trị cũ, mới được ghi audit log; hoặc bản sao lưu được tạo và kiểm tra<br />
toàn vẹn; hoặc dữ liệu được khôi phục và kiểm tra toàn vẹn.</td></tr>
<tr><td>Normal flow</td><td>1. System Administrator mở &quot;Cấu hình hệ thống&quot;.<br />
2. System Administrator chọn tham số và nhập giá trị mới.<br />
3. Hệ thống kiểm tra giá trị nằm trong khoảng cho phép.<br />
4. Hệ thống hiển thị giá trị cũ và mới, yêu cầu xác nhận.<br />
5. Hệ thống lưu và áp dụng (ví dụ thời hạn QR mới áp dụng cho các QR sinh sau đó, theo UC-07).<br />
6. Hệ thống ghi audit log.</td></tr>
<tr><td>Alternative flows</td><td>Sao lưu và khôi phục:<br />
1.1 Hệ thống tự chạy sao lưu theo lịch hằng ngày hoặc hằng tuần, kiểm tra tính toàn vẹn bản sao lưu<br />
và ghi kết quả.<br />
1.2 System Administrator chọn &quot;Sao lưu ngay&quot;ngoài lịch; hệ thống thực hiện như 1.1.<br />
1.3 Khôi phục: System Administrator chọn bản sao lưu và xác nhận (có cảnh báo ảnh hưởng); hệ thống<br />
khôi phục, kiểm tra toàn vẹn (số bản ghi, checksum) rồi đưa vào hoạt động và ghi audit log. Trong<br />
thời gian khôi phục, kiosk hoạt động theo chế độ offline của UC-08.</td></tr>
<tr><td>Exception flows</td><td>Tại bước 3:<br />
3.1 Giá trị ngoài khoảng cho phép hoặc sai định dạng: hệ thống từ chối và nêu khoảng hợp lệ.<br />
Tại bước 5:<br />
5.1 Lưu thất bại: giữ nguyên giá trị cũ, báo lỗi.<br />
Sao lưu (luồng thay thế 1.1, 1.2):<br />
6.1 Sao lưu thất bại (thiếu dung lượng, lỗi cơ sở dữ liệu): giữ bản sao lưu gần nhất, cảnh báo System<br />
Administrator và thử lại.<br />
Khôi phục (luồng thay thế 1.3):<br />
6.2 Kiểm tra toàn vẹn sau khôi phục thất bại: không đưa vào hoạt động, thử lại với bản sao lưu trước<br />
đó và ghi log.</td></tr>
</tbody>
</table>

Bảng trạng thái đồng bộ:

| Trạng thái | Ý nghĩa |
| --- | --- |
| PENDING | Chưa được gửi. |
| IN_PROGRESS | Đang gửi hoặc xử lý. |
| SYNCED | Đã có xác nhận tiếp nhận theo giao ước API. |
| FAILED | Đã xác định lỗi và cần xử lý. |
| RETRY_PENDING | Chờ gửi lại. |
| UNKNOWN | Chưa xác định được kết quả (ví dụ timeout sau khi gửi). |

<img src="images/page-026-image-01.png" alt="Page 26 image" width="38" />

<table>
<tbody>
<tr><td>6</td><td>Đặc tính kiến trúc (Architecture Characteristics)</td></tr>
<tr><td>6.1</td><td>Danh mục đặc tính kiến trúc</td></tr>
</tbody>
</table>

Bảng 6.1.1: Danh mục Architecture Characteristics của hệ thống TMC

| Mã | Đặc tính | Ý nghĩa trong bối cảnh TMC | Nguồn | Tiêu chí đo lường (Fitness function) | Ưu tiên |
| --- | --- | --- | --- | --- | --- |
| AC- 01 | Performance – Responsiveness | Thời gian từ lúc quét QR đến lúc kiosk hiển thị kết quả và gửi lệnh mở cổng. | Explicit (NFR-1) | • p95 ≤ 1,0 s; p99 ≤ 2,0 s end-to-end tại kiosk | Cao |
| AC- 02 | Availability / Fault tolerance (tại cổng) | Hệ thống duy trì khả năng phục vụ các chức năng quan trọng khi một service hoặc hệ thống tích hợp bên ngoài tạm thời gặp sự cố. | Implicit (miền vận hành) | • Availability ≥ 99,9% trong giờ làm việc. • HR/Payroll unavailable không làm gián đoạn check-in/check-out. • Message Broker/downstream failure không làm mất attendance event. • Service failure được retry/recover theo chính sách đã định. | Cao |
| AC- 03 | Reliability – Data integrity / Durability | Không mất, không trùng bản ghi chấm công trong vận hành bình thường và khi có sự cố mạng. | Explicit (NFR-2) | • RPO = 0 đối với sự kiện đã được xác nhận tại kiosk • Đối soát hằng ngày: số sự kiện tại biên = số sự kiện tại trung tâm (sai lệch 0) • 0 bản ghi trùng (ràng buộc unique event_id) | Cao |
| AC- 04 | Security (AuthN/AuthZ) | Chỉ người có quyền mới truy cập chức năng/dữ liệu tương ứng; QR không thể giả mạo hoặc dùng lại. | Explicit (NFR-3) | • 100% API được bảo vệ bởi RBAC • MFA cho SysAdmin, HR Staff • QR có thời gian hiệu lực ngắn (mục tiêu thiết kế ≤ 30 giây) nhằm hạn chế nguy cơ replay. | Cao |
| AC- 05 | Auditability | Audit log được thiết kế theo append-only và có cơ chế kiểm tra tính toàn vẹn; việc sửa/xóa trực tiếp phải bị hạn chế và được phát hiện. | Explicit (NFR-3) | • 100% thao tác ghi/sửa/xóa có audit log (ai, gì, khi nào, ở đâu) • Audit log append-only | TB |
| AC- 06 | Privacy / Compliance | Bảo vệ dữ liệu cá nhân nhân viên theo Nghị định 13/2023/NĐ-CP và Luật Bảo vệ dữ liệu cá nhân 2025. | Explicit (NFR-4) | • Mã QR không chứa PII • 100% truy cập PII được log; job xóa dữ liệu hết hạn lưu trữ | Cao |
| AC- 07 | Interoperability | Tích hợp đúng hợp đồng với HR/Payroll và hệ thống turnstile hiện có. | Explicit (Integration) | • Dữ liệu tháng sẵn sàng tại HR trước 12:00 ngày 1 (mục tiêu nội bộ: 08:00) • Contract test pass 100% với API HR và turnstile | TB |
| AC- 08 | Scalability / Elasticity | Thêm nhà máy, thêm cổng, tăng nhân sự mà không sửa mã nguồn. | Implicit | • Thêm nhà máy mới bằng cách triển khai thêm instance/service configuration tương ứng mà không thay đổi business logic. | Thấp |
| AC- 09 | Maintainability / Extensibility | Dễ thay đổi quy tắc ra vào, thay nhà cung cấp turnstile, thêm định dạng báo cáo. | Implicit (SOLID – đề bài) | • Thêm 1 quy tắc truy cập/1 adapter turnstile: chỉ thêm lớp mới, không sửa lớp cũ • Test coverage tầng domain/application ≥ 80% • Không vi phạm quy tắc phụ thuộc (ArchUnit) | TB |
| AC- 10 | Recoverability | Khôi phục nhanh khi máy chủ biên hoặc trung tâm hỏng. | Implicit | • RTO trung tâm ≤ 4 giờ; RTO node biên ≤ 30 phút (node dự phòng) • Backup DB hằng ngày + WAL archiving | TB |

<img src="images/page-027-image-01.png" alt="Page 27 image" width="30" />

| Mã | Đặc tính | Ý nghĩa trong bối cảnh TMC | Nguồn | Tiêu chí đo lường (Fitness function) | Ưu tiên |
| --- | --- | --- | --- | --- | --- |
| AC- 11 | Observability | Phát hiện sớm cổng ngoại tuyến, hàng đợi đồng bộ tồn đọng, đẩy HR thất bại. | Implicit | • Cảnh báo khi cổng offline \> 60 s, outbox tồn \> 5 phút, lỗi HR sync • Dashboard sức khỏe từng nhà máy | TB |
| AC- 12 | Usability (tại kiosk) | Phản hồi trực quan, rõ ràng cho công nhân và bảo vệ. | Implicit | • Phản hồi thành công/thất bại phải được hiển thị rõ ràng trong ≤ 2 giây. | Thấp |

### 6.2 Đặc tính dẫn dắt

Dựa trên các yêu cầu chức năng (FR) và phi chức năng (NFR) (đặc biệt là lượng tải tập trung vào giờ giao ca và yêu cầu khắt khe về bảo toàn dữ liệu), hệ thống được định hướng thiết kế để thỏa mãn các đặc tính kiến trúc cốt lõi sau:

1. Performance: Yêu cầu thời gian phản hồi tại kiosk ≤ 2 giây. Kiến trúc sử dụng mô hình event-driven: các bước critical path gồm xác thực QR, kiểm tra quyền, ghi nhận attendance và điều khiển turnstile được xử lý đồng bộ; các tác vụ hậu xử lý như reporting, audit và HR integration được tách sang asynchronous event processing để không làm tăng latency của giao dịch chính.

2. Reliability & Durability (Độ tin cậy & Bền vững dữ liệu): Yêu cầu "Không mất dữ liệu điểm danh". Việc sử dụng Message Broker (hàng đợi tin nhắn) trong Event-Driven đảm bảo các sự kiện quét QR được lưu trữ tạm thời một cách an toàn; ngay cả khi database đích gặp sự cố, dữ liệu vẫn không bị mất và sẽ được xử lý lại khi hệ thống phục hồi.

3. Interoperability (Khả năng tương tác/Tích hợp): Hệ thống phải tích hợp mượt mà với 2 hệ thống ngoại vi: Hệ thống Nhân sự (HR/Payroll) hiện có và Hệ thống điều khiển cổng (Turnstile). Việc sử dụng Microservices cho phép xây dựng các Anti-Corruption Layer (Lớp chống dội) hoặc các service tích hợp độc lập mà không ảnh hưởng đến core logic.

4. Security (Bảo mật): Đảm bảo Role-Based Access Control (RBAC) và tuân thủ quyền riêng tư dữ liệu cá nhân của nhân viên. Các service sẽ được xác thực và phân quyền tập trung thông qua API Gateway và Identity Service (như OAuth2/JWT).

5. Scalability (Khả năng mở rộng): Dù hiện tại chỉ có 2 nhà máy (Bình Dương và Long An), kiến trúc Microservices cho phép scale-out linh hoạt các service chịu tải cao vào đúng khung giờ giao ca mà không cần scale toàn bộ hệ thống.

<img src="images/page-028-image-01.png" alt="Page 28 image" width="38" />

<table>
<tbody>
<tr><td>7</td><td>Quyết định kiến trúc (Architecture Decisions)</td></tr>
<tr><td>7.1</td><td>Tổng hợp các quyết định kiến trúc</td></tr>
</tbody>
</table>

Bảng 7.1.1: Danh sách Architecture Decision Records

| Mã | Quyết định | Đặc tính phục vụ |
| --- | --- | --- |
| ADR-01 | Sử dụng kiến trúc Microservices, phân chia hệ thống theo các bounded context nghiệp vụ | AC-08, AC-09, AC-11 |
| ADR-02 | Sử dụng Event-Driven Architecture cho giao tiếp bất đồng bộ giữa các service | AC-02, AC-08, AC-10, AC-11 |
| ADR-03 | Attendance Service là nguồn dữ liệu chính (System of Record) cho các sự kiện chấm công | AC-02, AC-05, AC-10 |
| ADR-04 | Sử dụng Transactional Outbox + Message Broker để đảm bảo sự kiện được phát hành đáng tin cậy | AC-02, AC-10, AC-11 |
| ADR-05 | Áp dụng Idempotency cho việc ghi nhận và xử lý sự kiện chấm công | AC-02, AC-05, AC-10 |
| ADR-06 | Tích hợp hệ thống turnstile thông qua Adapter / Anti-Corruption Layer | AC-01, AC-07, AC-09 |
| ADR-07 | Tích hợp HR/Payroll thông qua Integration Service và API Contract riêng biệt | AC-07, AC-09, AC-11 |
| ADR-08 | Sử dụng Centralized Identity + RBAC + Audit Logging cho xác thực và phân quyền | AC-04, AC-05 |
| ADR-09 | Áp dụng Privacy by Design và tối thiểu hóa dữ liệu cá nhân được lưu trữ/truyền tải | AC-04, AC-06 |
| ADR-10 | Mỗi microservice sở hữu dữ liệu nghiệp vụ của mình; Reporting sử dụng read model riêng | AC-08, AC-09, AC-10 |

<table>
<tbody>
<tr><td>7.2</td><td>Chi tiết các quyết định</td></tr>
<tr><td>7.2.1</td><td>ADR-01: Sử dụng kiến trúc Microservices</td></tr>
</tbody>
</table>

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Hệ thống có nhiều nhóm nghiệp vụ tương đối độc lập như quản lý người dùng/QR, quản lý gate station, ghi nhận attendance, reporting/auditing và tích hợp HR/Payroll. Các chức năng này có yêu cầu thay đổi, mở rộng và tích hợp khác nhau. Kiến trúc nguyên khối có thể tạo coupling cao và khiến việc thay đổi một chức năng ảnh hưởng đến toàn hệ thống. |
| Quyết định (Decision) | Sử dụng kiến trúc Microservices, trong đó hệ thống được chia thành các service theo nghiệp vụ, ví dụ: User/QR Service, Gate Station Service, Attendance Service, Reporting Service, HR Integration Service và Audit/Admin Service. Mỗi service chịu trách nhiệm cho một bounded context rõ ràng, giao tiếp thông qua API hoặc event thay vì truy cập trực tiếp vào logic nội bộ của service khác. |
| Phương án đã cân nhắc | \- Monolithic Architecture: triển khai đơn giản nhưng coupling cao và khó mở rộng độc lập → loại. \- Modular Monolith: giảm coupling hơn monolith nhưng các module vẫn nằm trong cùng một deployment unit → không được chọn vì hệ thống có nhu cầu tích hợp và mở rộng service độc lập. \- Microservices: tăng tính độc lập, khả năng mở rộng và bảo trì → chọn. |
| Hệ quả (Consequences) | (+) Các service có thể phát triển, triển khai và mở rộng độc lập. (+) Giới hạn lỗi trong phạm vi từng service tốt hơn. (+) Phù hợp với việc kết hợp REST/gRPC và Event-Driven Communication. (–) Tăng độ phức tạp vận hành, monitoring và deployment. (–) Phải xử lý các vấn đề distributed transaction, network failure và eventual consistency. |

<img src="images/page-029-image-01.png" alt="Page 29 image" width="38" />

| Kiểm chứng tuân thủ (Compliance) | Kiểm tra component/deployment diagram bảo đảm mỗi service có trách nhiệm rõ ràng; kiểm tra source code không cho phép service truy cập trực tiếp database của service khác; kiểm thử khả năng deploy và scale service độc lập. |
| --- | --- |
| Liên kết | AC-08, AC-09, AC-11 · Phần 3 (Logical Architecture) · Phần 4 (Deployment View) |

### 7.2.2 ADR-02: Sử dụng Event-Driven Architecture

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Một attendance event sau khi được ghi nhận có thể cần được sử dụng bởi nhiều chức năng khác nhau như reporting, audit và HR/Payroll. Nếu Attendance Service gọi tuần tự tất cả các service liên quan, hệ thống sẽ tăng coupling và thời gian phản hồi của critical path. |
| Quyết định (Decision) | Sử dụng Event-Driven Architecture cho các tác vụ không yêu cầu phản hồi tức thời. Sau khi Attendance Service ghi nhận thành công một attendance event, service phát hành event thông qua Message Broker. Reporting, Audit và HR Integration có thể subscribe và xử lý event độc lập. Các thao tác yêu cầu phản hồi tức thời như xác thực QR và điều khiển turnstile vẫn sử dụng synchronous API. |
| Phương án đã cân nhắc | \- Synchronous REST giữa tất cả service: đơn giản nhưng tạo coupling và làm tăng latency → loại. \- Event-Driven cho toàn bộ hệ thống: giảm coupling nhưng không phù hợp với các thao tác cần phản hồi tức thời như mở cổng → loại. \- Hybrid synchronous + event-driven: chọn. |
| Hệ quả (Consequences) | (+) Giảm coupling giữa các service. (+) Reporting, Audit và HR Integration có thể xử lý độc lập. (+) Dễ mở rộng consumer mới mà không thay đổi Attendance Service. (–) Hệ thống có eventual consistency. (–) Cần Message Broker, retry, dead-letter handling và monitoring event. |
| Kiểm chứng tuân thủ (Compliance) | Kiểm tra attendance event được publish sau khi transaction ghi nhận thành công; kiểm thử consumer có thể retry khi downstream service tạm thời unavailable; kiểm tra event không làm chậm critical path vượt giới hạn 2 giây. |
| Liên kết | AC-02, AC-08, AC-10, AC-11 · Phần 3 (Sequence/Component View) · Phần 4 (Integration/Deployment View) |

### 7.2.3 ADR-03: Attendance Service là System of Record

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Attendance event là dữ liệu nghiệp vụ quan trọng và phải được bảo đảm không mất trong điều kiện hoạt động bình thường. Đồng thời, nhiều thành phần khác như Reporting và HR/Payroll cần sử dụng cùng nguồn dữ liệu để tránh sai lệch. |
| Quyết định (Decision) | Attendance Service là nguồn dữ liệu chính cho attendance transaction. Mọi lượt check- in/check-out hợp lệ được ghi nhận tại Attendance Service trước khi được phát hành thành event cho các consumer khác. Reporting, Audit và HR Integration không được tự tạo attendance transaction độc lập. |
| Phương án đã cân nhắc | \- Cho mỗi service tự lưu attendance → dễ phát sinh dữ liệu không nhất quán → loại. \- Để Reporting hoặc HR/Payroll làm nguồn dữ liệu chính → không phù hợp với trách nhiệm nghiệp vụ → loại. \- Attendance Service làm System of Record → chọn. |
| Hệ quả (Consequences) | (+) Có một nguồn dữ liệu chuẩn để đối soát. (+) Giảm nguy cơ duplicate hoặc conflicting attendance records. (+) Dễ audit và truy vết nguồn gốc dữ liệu. (–) Attendance Service trở thành thành phần quan trọng cần được bảo đảm availability và durability cao. |

<img src="images/page-030-image-01.png" alt="Page 30 image" width="38" />

| Kiểm chứng tuân thủ (Compliance) | Kiểm tra schema và quyền truy cập database; chỉ Attendance Service được phép tạo attendance transaction; kiểm thử reconciliation giữa Attendance Service và Reporting/HR Integration. |
| --- | --- |
| Liên kết | AC-02, AC-05, AC-10 · Phần 3 (Component View) |

### 7.2.4 ADR-04: Transactional Outbox + Message Broker

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Sau khi attendance transaction được lưu, hệ thống cần phát hành event cho Message Broker. Nếu database commit thành công nhưng việc publish event thất bại, dữ liệu vẫn tồn tại nhưng các service downstream không nhận được event. Ngược lại, nếu publish trước rồi database transaction thất bại, hệ thống có thể phát hành event không hợp lệ. |
| Quyết định (Decision) | Attendance Service sử dụng Transactional Outbox Pattern. Attendance transaction và outbox event được ghi trong cùng một database transaction. Một publisher riêng đọc các outbox record chưa gửi và publish chúng tới Message Broker, sau đó cập nhật trạng thái event. |
| Phương án đã cân nhắc | \- Publish trực tiếp sau database commit → có nguy cơ mất event. \- Distributed transaction/2PC → phức tạp và tăng coupling. \- Transactional Outbox → phù hợp với microservices và event-driven → chọn. |
| Hệ quả (Consequences) | (+) Giảm nguy cơ mất event giữa database và broker. (+) Có thể retry việc publish. (+) Phù hợp với eventual consistency. (–) Có thêm outbox storage và publisher process. (–) Consumer vẫn phải hỗ trợ idempotency vì event có thể được gửi lại. |
| Kiểm chứng tuân thủ (Compliance) | Integration test mô phỏng broker unavailable sau khi database commit; xác nhận outbox event vẫn tồn tại và được publish lại sau khi broker phục hồi. |
| Liên kết | AC-02, AC-10, AC-11 · ADR-02 · ADR-05 |

### 7.2.5 ADR-05: Idempotency cho attendance event

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Trong hệ thống Event-Driven, message có thể được retry hoặc delivery nhiều lần. Nếu cùng một attendance event được xử lý nhiều lần mà không có cơ chế kiểm soát, hệ thống có thể tạo duplicate transaction hoặc duplicate báo cáo. |
| Quyết định (Decision) | Mỗi attendance event có một event ID duy nhất. Attendance Service và các consumer sử dụng event ID để phát hiện và bỏ qua các bản ghi đã được xử lý. Các thao tác ghi nhận attendance phải có unique constraint/idempotency key phù hợp. |
| Phương án đã cân nhắc | \- Không kiểm soát duplicate → không đáp ứng yêu cầu integrity. \- Chỉ dựa vào Message Broker → không đủ vì broker không loại bỏ mọi trường hợp duplicate delivery. \- Idempotent consumer + unique event ID → chọn. |
| Hệ quả (Consequences) | (+) Tránh duplicate attendance và duplicate downstream processing. (+) Cho phép retry an toàn. (+) Tăng độ tin cậy của Event-Driven Architecture. (–) Các service phải lưu trạng thái/event ID đã xử lý. (–) Tăng một phần storage và logic xử lý. |
| Kiểm chứng tuân thủ (Compliance) | Gửi cùng một event nhiều lần và kiểm tra hệ thống chỉ tạo một attendance transaction và một kết quả nghiệp vụ tương ứng. |
| Liên kết | AC-02, AC-05, AC-10 · ADR-02, ADR-04 |

7.2.6 ADR-06: Tích hợp Turnstile thông qua Adapter / Anti-Corruption Layer

<img src="images/page-031-image-01.png" alt="Page 31 image" width="38" />

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Turnstile Control System đã tồn tại và cung cấp API riêng. Logic nghiệp vụ của hệ thống Attendance không nên phụ thuộc trực tiếp vào API hoặc data model cụ thể của hệ thống turnstile. |
| Quyết định (Decision) | Xây dựng Turnstile Adapter làm lớp trung gian giữa Gate Station Service và Turnstile Control System. Adapter chuyển đổi interface nội bộ của hệ thống sang API của turnstile và xử lý các khác biệt về protocol, request/response và error handling. |
| Phương án đã cân nhắc | \- Gọi trực tiếp Turnstile API từ business logic → coupling cao → loại. \- Đưa logic turnstile vào Attendance Service → sai trách nhiệm → loại. \- Adapter/Anti-Corruption Layer → chọn. |
| Hệ quả (Consequences) | (+) Cô lập dependency với hệ thống bên ngoài. (+) Có thể thay đổi vendor/API của turnstile mà ít ảnh hưởng business logic. (+) Dễ mock khi testing. (–) Có thêm một abstraction layer cần duy trì. |
| Kiểm chứng tuân thủ (Compliance) | Unit test business logic với mock adapter; integration test kiểm tra adapter chuyển đổi đúng request/response với Turnstile API. |
| Liên kết | AC-01, AC-07, AC-09 · Phần 3 (Component View) · Phần 4 (Integration View) |

### 7.2.7 ADR-07: Tích hợp HR/Payroll thông qua Integration Service

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Hệ thống phải truyền dữ liệu attendance sang HR/Payroll thông qua API và dữ liệu phải sẵn sàng để đối soát trước 12 giờ ngày đầu tiên của mỗi tháng. HR/Payroll là hệ thống bên ngoài và không nên được tích hợp trực tiếp vào các service nghiệp vụ cốt lõi. |
| Quyết định (Decision) | Xây dựng HR Integration Service chịu trách nhiệm nhận attendance data/event, chuyển đổi dữ liệu theo API contract của HR/Payroll, thực hiện retry khi cần và hỗ trợ reconciliation. Attendance Service không gọi trực tiếp HR/Payroll trong critical path của việc quét QR. |
| Phương án đã cân nhắc | \- Attendance Service gọi trực tiếp HR/Payroll → coupling cao và ảnh hưởng latency → loại. \- Mỗi service tự tích hợp HR/Payroll → duplicate integration logic → loại. \- Dedicated HR Integration Service → chọn. |
| Hệ quả (Consequences) | (+) Tách biệt hệ thống nội bộ với API bên ngoài. (+) HR/Payroll unavailable không làm dừng quá trình check-in/check-out. (+) Có nơi tập trung cho retry và reconciliation. (–) Dữ liệu HR có thể có độ trễ do asynchronous processing. (–) Cần monitoring integration failures. |
| Kiểm chứng tuân thủ (Compliance) | Integration test với mock HR/Payroll API; kiểm tra retry khi API unavailable; thực hiện reconciliation và xác nhận dữ liệu attendance được cung cấp trước deadline quy định. |
| Liên kết | AC-03, AC-07, AC-11 · ADR-02 |

### 7.2.8 ADR-08: Centralized Identity + RBAC + Audit Logging

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Hệ thống có nhiều nhóm người dùng với quyền khác nhau: nhân viên, production supervisor, HR staff, security guard, system administrator và executive management. Các thao tác quản trị và truy cập dữ liệu cần được truy vết. |
| Quyết định (Decision) | Sử dụng cơ chế Centralized Identity cho authentication và áp dụng RBAC để kiểm soát quyền theo role. Các thao tác quan trọng như đăng nhập, thay đổi quyền, thay đổi cấu hình gate, truy cập hoặc điều chỉnh dữ liệu attendance được ghi vào Audit Log. |

<img src="images/page-032-image-01.png" alt="Page 32 image" width="38" />

| Phương án đã cân nhắc | \- Mỗi microservice tự quản lý authentication → duplicate và khó đồng bộ → loại. \- Phân quyền chỉ ở UI → không đủ an toàn → loại. \- Centralized Identity + RBAC + server-side authorization → chọn. |
| --- | --- |
| Hệ quả (Consequences) | (+) Chính sách authentication/authorization nhất quán. (+) Dễ quản lý quyền theo role. (+) Hỗ trợ audit và truy vết hành động. (–) Identity component trở thành infrastructure quan trọng. (–) Các service phải kiểm tra authorization phù hợp với API của mình. |
| Kiểm chứng tuân thủ (Compliance) | Security test với từng role; xác nhận user không thể truy cập API ngoài quyền; kiểm tra audit log chứa actor, action, timestamp và resource đối với các thao tác quan trọng. |
| Liên kết | AC-04, AC-05 · Phần 3 (Security/Component View) |

### 7.2.9 ADR-09: Privacy by Design

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Hệ thống xử lý thông tin nhân viên và dữ liệu attendance. Yêu cầu đặt ra việc bảo vệ dữ liệu cá nhân trong quá trình lưu trữ, xử lý và truyền tải. |
| Quyết định (Decision) | Áp dụng Privacy by Design: chỉ thu thập và lưu trữ dữ liệu cần thiết; giới hạn quyền truy cập theo role; bảo vệ dữ liệu khi truyền tải; không đưa thông tin cá nhân không cần thiết vào event/message; áp dụng retention policy phù hợp và ghi nhận các hoạt động truy cập dữ liệu nhạy cảm. |
| Phương án đã cân nhắc | \- Bảo vệ dữ liệu chỉ ở tầng database → không đủ vì dữ liệu còn được truyền qua API/event → loại. \- Thu thập đầy đủ dữ liệu để thuận tiện phát triển → tăng rủi ro privacy → loại. \- Privacy by Design trên toàn bộ lifecycle → chọn. |
| Hệ quả (Consequences) | (+) Giảm lượng dữ liệu cá nhân bị phơi nhiễm. (+) Hỗ trợ tuân thủ yêu cầu bảo vệ dữ liệu. (+) Giảm tác động nếu một thành phần bị compromise. (–) Cần thiết kế thêm policy về access, retention và data masking. |
| Kiểm chứng tuân thủ (Compliance) | Security/privacy review đối với database schema, API và event schema; kiểm tra role không được phép không thể truy cập dữ liệu cá nhân; kiểm tra dữ liệu nhạy cảm không xuất hiện trong event nếu không cần thiết. |
| Liên kết | AC-04, AC-06 · ADR-08 |

7.2.10 ADR-10: Database per Service và Read Model cho Reporting

| Trạng thái | Accepted (đã chấp thuận cho M1) |
| --- | --- |
| Bối cảnh (Context) | Microservices cần giảm coupling dữ liệu. Trong khi đó, Reporting phải thực hiện các truy vấn tổng hợp như số lượt ra/vào, dữ liệu theo nhân viên, theo nhà máy và theo khoảng thời gian. Nếu Reporting truy cập trực tiếp database của nhiều service, coupling sẽ tăng cao. |
| Quyết định (Decision) | Mỗi microservice sở hữu database/schema dữ liệu nghiệp vụ của mình. Reporting Service nhận dữ liệu cần thiết thông qua event và xây dựng read model riêng được tối ưu cho truy vấn báo cáo. Reporting không truy cập trực tiếp database nội bộ của các service khác. |
| Phương án đã cân nhắc | \- Shared database cho tất cả service → coupling cao → loại. \- Reporting query trực tiếp nhiều database → phức tạp và phụ thuộc implementation → loại. \- Database per service + Reporting read model → chọn. |

<img src="images/page-033-image-01.png" alt="Page 33 image" width="38" />

| Hệ quả (Consequences) | (+) Giảm coupling dữ liệu giữa các service. (+) Reporting có thể tối ưu database riêng cho workload đọc. (+) Có thể scale Reporting độc lập. (–) Dữ liệu báo cáo có thể có độ trễ nhỏ so với dữ liệu nguồn. (–) Cần cơ chế đồng bộ và rebuild read model khi cần. |
| --- | --- |
| Kiểm chứng tuân thủ (Compliance) | Kiểm tra database access giữa các service; xác nhận Reporting không truy cập trực tiếp database của Attendance; kiểm thử read model được cập nhật từ attendance events. |
| Liên kết | AC-08, AC-09, AC-10 · ADR-02, ADR-03 |

<img src="images/page-034-image-01.png" alt="Page 34 image" width="30" />

```text
8 Nguyên lý thiết kế (Design Principles) và áp dụng SOLID
8.1 Áp dụng 5 nguyên tắc SOLID
8.1.1 S – Single Responsibility Principle (Nguyên tắc đơn trách nhiệm)
```

Định nghĩa: "Một module chỉ nên có một lý do để thay đổi- hay chính xác hơn theo Clean Architecture: một module chỉ chịu trách nhiệm trước một tác nhân (actor).

Áp dụng trong TMC: luồng quét QR liên quan tới ít nhất 4 nguồn thay đổi độc lập: (1) bộ phận an ninh/IT thay đổi cơ chế mã hóa QR; (2) ban quản lý thay đổi chính sách ra vào (theo ca, theo nhà máy);

- (3) HR thay đổi cách ghi nhận công; (4) nhà cung cấp thay đổi API turnstile. Gộp chúng vào một lớp sẽ khiến mọi thay đổi đều có nguy cơ làm hỏng luồng mở cổng.

Lợi ích: thay đổi chính sách ra vào không chạm tới mã điều khiển cổng (giảm rủi ro cho AC-02); từng lớp test độc lập (AC-09).

### 8.1.2 O – Open/Closed Principle (Nguyên tắc đóng/mở)

Định nghĩa: thực thể phần mềm nên mở để mở rộng nhưng đóng để sửa đổi – thêm hành vi mới bằng cách thêm mã mới, không sửa mã đã kiểm thử.

Áp dụng trong TMC: ba điểm biến đổi (variation points) được dự đoán sẽ thay đổi thường xuyên:

- (1) quy tắc ra vào (theo ca, khu vực, danh sách cấm, anti-passback. . . ); (2) định dạng báo cáo/xuất dữ liệu (Excel, CSV, PDF, định dạng HR mới); (3) nhà cung cấp turnstile (ADR-06).

Lợi ích: đáp ứng AC-09 (thêm quy tắc không sửa lớp cũ – đây chính là fitness function đã đặt ra) và giảm rủi ro hồi quy trên luồng có yêu cầu 2 s.

8.1.3 L – Liskov Substitution Principle (Nguyên tắc thay thế Liskov)

Định nghĩa: đối tượng của lớp con phải thay thế được đối tượng của lớp cha (hay cài đặt thay thế được cho interface) mà không làm thay đổi tính đúng đắn của chương trình: không tăng điều kiện đầu vào (precondition), không giảm cam kết đầu ra (postcondition), không ném ngoại lệ ngoài hợp đồng.

Áp dụng trong TMC: các thành phần tích hợp được thiết kế thông qua các abstraction có hợp đồng rõ ràng, cho phép nhiều implementation thay thế nhau mà không làm thay đổi logic nghiệp vụ. Ví dụ, TurnstileController có thể được triển khai bởi adapter cho các nhà cung cấp thiết bị khác nhau hoặc implementation giả lập phục vụ kiểm thử. Use case mở cổng phải hoạt động đúng với mọi implementation tuân thủ cùng một contract.

Lợi ích: thay nhà cung cấp turnstile, chuyển từ Simulated (UAT) sang thiết bị thật mà không sửa use case (AC-07, AC-09); bảo đảm AC-01 không bị phá bởi một adapter "chậm".

8.1.4 I – Interface Segregation Principle (Nguyên tắc phân tách giao diện)

Định nghĩa: client không nên bị buộc phụ thuộc vào những phương thức mà nó không sử dụng; nên có nhiều interface nhỏ, chuyên biệt thay vì một interface "béo".

Áp dụng trong TMC: một trạm cổng gồm nhiều loại thiết bị (đầu đọc QR, màn hình kiosk, turnstile) và dữ liệu chấm công có nhiều nhóm client với nhu cầu khác nhau (luồng ghi tại biên, báo cáo, xuất HR).

Ở mức API, ISP được áp dụng bằng cách tách các nhóm API theo tác nhân thay vì một API chung: /self-service/* (Employee: xem công, lấy QR), /supervision/* (Supervisor), /hr/* (HR Staff: tra cứu, đối soát và điều chỉnh dữ liệu chấm công theo quyền được cấp ), /security/* (Bảo vệ: giám sát cổng, mở

<img src="images/page-035-image-01.png" alt="Page 35 image" width="30" />

*tay), /admin/* (SysAdmin), /executive/* (dashboard). Cách tách này khớp trực tiếp với RBAC (ADR-08), giúp phân quyền đơn giản và giảm bề mặt tấn công.*

Lợi ích: module báo cáo không thể vô tình ghi dữ liệu chấm công (AC-03, AC-05); kiosk chỉ cài đặt phần nó có; thay đổi trên API HR không ảnh hưởng client của Employee.

8.1.5 D – Dependency Inversion Principle (Nguyên tắc đảo ngược phụ thuộc)

Định nghĩa: (1) module cấp cao không phụ thuộc module cấp thấp – cả hai phụ thuộc vào abstraction; (2) abstraction không phụ thuộc chi tiết – chi tiết phụ thuộc abstraction. Điểm mấu chốt: interface thuộc sở hữu của tầng cấp cao (application), không phải của tầng hạ tầng.

Áp dụng trong TMC: chính sách nghiệp vụ quan trọng nhất (quy trình đồng bộ và đối soát dữ liệu chấm công với HR/Payroll – ràng buộc hạn 12:00) không được phụ thuộc vào chi tiết như thư viện HTTP, định dạng JSON của HR hay CSDL cụ thể.

Lợi ích: domain/application độc lập với hạ tầng (AC-09); kiểm thử đầy đủ các kịch bản hạn chót (AC-07); DIP là nguyên tắc nền giúp OCP và LSP vận hành được.

### 8.2 Các nguyên lý thiết kế bổ sung

Ngoài SOLID, các nguyên lý sau là hướng dẫn bắt buộc khi xây dựng hệ thống TMC:

Bảng 8.2.1: Bộ nguyên lý thiết kế (Design Principles) của hệ thống TMC

| \# | Nguyên lý | Hướng dẫn cụ thể | Phục vụ |
| --- | --- | --- | --- |
| DP-01 | Dependency Rule / Separation of Concerns | Phụ thuộc chỉ hướng vào domain; không đặt logic nghiệp vụ trong controller, adapter hay SQL. | AC-09 |
| DP-02 | Idempotency by Default | Mọi lệnh/thông điệp có ID duy nhất (event_id, command_id, batch_id); bên nhận xử lý trùng là no-op. | AC-03 |
| DP-03 | Design for Failure | Mọi lời gọi ra ngoài có timeout, retry có giới hạn + backoff, circuit breaker và đường lùi (fallback). | AC-02, AC-07 |
| DP-04 | Fail-secure / Fail-safe | Xảy ra lỗi không xác định trong quyết định kiểm soát truy cập, hệ thống không được tự động cấp quyền vào; các cơ chế mở lối khẩn cấp phải tuân theo quy định an toàn và cơ chế của hệ thống kiểm soát vật lý, đồng thời ghi nhận sự kiện liên quan. | AC-04, an toàn |
| DP-05 | Least Privilege & Defense in Depth | Mỗi vai trò/dịch vụ chỉ có quyền tối thiểu; bảo vệ nhiều lớp: mạng, TLS, AuthN, AuthZ, mã hóa dữ liệu, audit. | AC-04, AC-05 |
| DP-06 | Privacy by Design | Không đưa PII vào QR, log, thông điệp sự kiện; tham chiếu bằng UUID; mã hóa PII khi lưu. | AC-06 |
| DP-07 | Single Source of Truth | Attendance Service là nguồn dữ liệu chuẩn cho các attendance transaction; các báo cáo và dữ liệu đồng bộ sang HR/Payroll phải được xây dựng từ dữ liệu attendance đã được xác nhận và có khả năng truy vết | AC-03 |
| DP-08 | API-first & Contract Testing | Mọi giao tiếp được đặc tả bằng OpenAPI/AsyncAPI trước khi hiện thực; contract test trong CI. | AC-07 |
| DP-09 | Observability by Default | Log có cấu trúc + correlation id, metric và trace (OpenTelemetry) cho mọi luồng; không log PII. | AC-11 |
| DP-10 | Configuration over Code | Nhà máy, cổng, ca, tham số quy tắc, adapter thiết bị cấu hình được, không hard-code. | AC-08 |

<img src="images/page-036-image-01.png" alt="Page 36 image" width="30" />

# 9 Ma trận truy vết (Traceability Matrix)

Ma trận dưới đây nối yêu cầu của đề bài với đặc tính kiến trúc, quyết định kiến trúc và nguyên lý/module hiện thực, bảo đảm không có yêu cầu nào "bị bỏ rơi"và không có quyết định nào thiếu căn cứ.

Bảng 9.0.1: Truy vết Yêu cầu → Đặc tính → Quyết định → Thiết kế

| Yêu cầu / Mục tiêu | Đặc tính kiến trúc | ADR liên quan | Nguyên lý / SOLID liên quan |
| --- | --- | --- | --- |
| Kiosk phản hồi ≤ 2 giây, 800 lượt/30 phút/nhà máy | AC-01 Performance | ADR-01, ADR-06, ADR-10 | SRP, OCP, LSP, DIP |
| Không mất bản ghi chấm công trong vận hành bình thường | AC-02 Availability, AC-03 Reliability & Data Integrity | ADR-02, ADR-03, ADR-04, ADR-05 | DP-02 Idempotency, DP-03 Design for Failure, DP-07 Single Source of Truth |
| RBAC và audit logging đầy đủ | AC-04 Security, AC-05 Auditability | ADR-08 | ISP, DIP, DP-05 Least Privilege & Defense in Depth, DP-09 Observability |
| Bảo vệ dữ liệu cá nhân | AC-04 Security, AC-06 Privacy & Compliance | ADR-08, ADR-09 | DP-05 Least Privilege & Defense in Depth, DP-06 Privacy by Design |
| Dữ liệu chấm công phải được cung cấp cho HR/Payroll trước 12:00 ngày đầu tháng | AC-07 Interoperability | ADR-02, ADR-04, ADR-07 | OCP, DIP, DP-03 Design for Failure, DP-08 API-first & Contract Testing |
| Tích hợp với API turnstile hiện có | AC-01 Performance, AC-07 Interoperability, AC-09 Maintainability | ADR-06 | OCP, LSP, DIP, DP-01 Dependency Rule |
| Quản lý người dùng và QR Code | AC-04 Security, AC-06 Privacy & Compliance | ADR-08, ADR-09 | SRP, ISP, DIP, DP-05, DP-06 |
| Quản lý nhà máy, cổng và kiosk | AC-08 Scalability, AC-11 Observability | ADR-01, ADR-06 | SRP, OCP, DP-10 Configuration over Code |
| Ghi nhận và đảm bảo tính toàn vẹn của attendance event | AC-02 Availability, AC-03 Reliability & Data Integrity | ADR-03, ADR-04, ADR-05 | SRP, DIP, DP-02 Idempotency, DP-07 Single Source of Truth |
| Báo cáo và auditing | AC-05 Auditability, AC-10 Recoverability | ADR-03, ADR-04, ADR-10 | ISP, OCP, DP-07 Single Source of Truth, DP-09 Observability |
| Quản trị hệ thống và giám sát trạng thái | AC-04 Security, AC-09 Maintainability, AC-11 Observability | ADR-01, ADR-08 | SRP, ISP, DP-05 Least Privilege, DP-09 Observability, DP-10 Configuration over Code |
| Hỗ trợ mở rộng hệ thống khi tăng số lượng nhà máy, cổng và dữ liệu | AC-08 Scalability, AC-09 Maintainability | ADR-01, ADR-02, ADR-10 | OCP, DIP, DP-01 Dependency Rule, DP-10 Configuration over Code |
| Tuân thủ nguyên tắc SOLID trong thiết kế | AC-09 Maintainability | — | SRP, OCP, LSP, ISP, DIP |

<img src="images/page-037-image-01.png" alt="Page 37 image" width="30" />

<table>
<tbody>
<tr><td>10</td><td>Lựa chọn kiến trúc chủ đạo</td></tr>
<tr><td>10.1</td><td>Cơ sở lựa chọn kiến trúc</td></tr>
</tbody>
</table>

Các yêu cầu của Phần 1 được chuyển thành tiêu chí thiết kế ở Bảng 3.1. Mỗi lựa chọn phải giải thích được thành phần nào chịu trách nhiệm, dữ liệu nào cần được bảo toàn và ảnh hưởng đến thời gian phản hồi. Các mã NFR trong phần này được dẫn theo bảng chi tiết của Phần 1.

Bảng 10.1.1: Từ yêu cầu đến lựa chọn kiến trúc TMC

| Yêu cầu | Ý nghĩa kiến trúc | Lựa chọn và căn cứ |
| --- | --- | --- |
| Kiosk phản hồi ≤ 2 giây tại tải 800 lượt/30 phút/nhà máy. | Cần giới hạn công việc bắt buộc trước khi trả kết quả. | Giữ xác thực và điều khiển cổng trên tuyến đồng bộ; tách hậu xử lý. Đề M1 trang 2; Phần 1, NFR- 06. |
| Không mất bản ghi chấm công khi vận hành bình thường. | Ghi nhận thành công phải gắn với dữ liệu đã lưu bền vững. | Attendance sở hữu dữ liệu chuẩn; transaction cục bộ, outbox và chống trùng. NFR-07; ADR-03-05. |
| HR có dữ liệu trước 12:00 ngày 1 hằng tháng. | Tích hợp cần retry, đối soát và theo dõi tiến độ riêng. | HR Integration tiêu thụ event; scheduler hoàn tất đối soát, không chặn kiosk. FR-29-30, NFR-08; ADR-07. |
| Quản lý QR, cổng, báo cáo và tích hợp thay đổi khác nhau. | Cần giới hạn phạm vi sửa đổi và phát hành từng chức năng. | Tách service theo nghiệp vụ và dữ liệu; adapter riêng cho Turnstile/HR. FR-01-35; AC-09; ADR- 01,06-07,10. |
| RBAC, bảo vệ dữ liệu cá nhân và audit đầy đủ. | Kiểm soát phải được duy trì khi yêu cầu đi qua nhiều service. | Identity tập trung; service kiểm tra quyền và lưu dấu vết theo ngữ cảnh. NFR-01-05; ADR-08-09. |
| Kiosk chịu được gián đoạn mạng ngắn. | Dữ liệu chưa chuyển về trung tâm phải được giữ và gửi lại an toàn. | Lưu cục bộ, đồng bộ bằng định danh ổn định và đối soát ngoại lệ. NFR-09; UC-08; ADR-05. |

### 10.2 So sánh các kiến trúc ứng viên

Các phương án được so sánh theo khả năng phân chia trách nhiệm, sở hữu dữ liệu, phản hồi tại cổng và tích hợp HR. Microservices xác định cách tổ chức các service; Event-Driven xác định cách chúng phối hợp qua sự kiện. Hai hướng này có thể kết hợp, đồng thời từng service vẫn tổ chức code theo tầng \[P3-1\], \[P3-2\].

Bảng 10.2.1: So sánh các phương án kiến trúc trong bối cảnh TMC

| Phương án | Điểm phù hợp | Đánh đổi | Kết luận |
| --- | --- | --- | --- |
| Layered /Modular Monolith | Triển khai đơn giản, gọi nội bộ và transaction cục bộ. Phân tầng tách trách nhiệm kỹ thuật; modular monolith bổ sung ranh giới nghiệp vụ rõ \[P3-3\]. | Các module thường cùng một đơn vị phát hành; nâng cấp tích hợp hoặc tăng tài nguyên báo cáo có thể kéo theo triển khai cả ứng dụng. | Khả thi với tải hiện tại, nhưng không ưu tiên khi TMC chọn khả năng phát hành và vận hành từng dịch vụ độc lập. |
| Service-Based Architecture | Một số dịch vụ nghiệp vụ lớn giúp phân chia trách nhiệm với ít đầu mối vận hành hơn microservices chi tiết. | Nếu chia sẻ schema hoặc truy cập dữ liệu chéo, thay đổi dữ liệu vẫn ràng buộc nhiều dịch vụ. Tách service chỉ trên sơ đồ chưa đủ bảo đảm độc lập. | Là phương án cân bằng; TMC chọn ranh giới sở hữu dữ liệu và hợp đồng riêng chặt hơn theo ADR-10. |

<img src="images/page-038-image-01.png" alt="Page 38 image" width="30" />

| Phương án | Điểm phù hợp | Đánh đổi | Kết luận |
| --- | --- | --- | --- |
| Microservices chỉ dùng giao tiếp đồng bộ | Dịch vụ có thể triển khai độc lập; request/response thuận tiện cho thao tác cần biết kết quả ngay \[P3- 1\]. | Khi cập nhật Reporting, Audit và HR bằng lời gọi đồng bộ, bên phát sinh giao dịch phụ thuộc thời điểm sẵn sàng của bên nhận; lỗi và retry phải được điều phối riêng. | Giữ cho tuyến kiểm soát cổng, không dùng làm cơ chế duy nhất cho toàn hệ thống. |
| Event-Driven cho toàn bộ tương tác | Producer và consumer phối hợp qua sự kiện; thuận tiện phân phối dữ liệu cho nhiều bên và xử lý lệch nhịp \[P3-2\]. | Muốn dùng cho mở cổng phải bổ sung tương quan phản hồi, deadline và xác nhận tác động. Broker nhận message không chứng minh Turnstile đã mở. | Không chọn cho mọi tương tác. EDA có thể nhanh, nhưng TMC cần ngữ nghĩa xác nhận rõ trong hạn phản hồi tại kiosk. |
| Microservices + Event- Driven | Kết hợp ranh giới triển khai/dữ liệu độc lập với hậu xử lý bất đồng bộ; giữ request/response cho quyết định truy cập và điều khiển thiết bị. | Cần broker, outbox, quản lý phiên bản hợp đồng, chống trùng, giám sát và xử lý dữ liệu nhất quán cuối cùng. | Chọn làm kiến trúc TMC: giới hạn số service theo nghiệp vụ, triển khai ADR- 01-02 cùng các cơ chế tin cậy bổ trợ. |

### 10.3 Kiến trúc Microservices kết hợp Event Driven

Microservices + Event-Driven tách ba nhịp vận hành: giao dịch tại cổng cần phản hồi ngay; báo cáo cần tổng hợp dữ liệu; tích hợp HR cần đối soát theo hạn tháng. Với hợp đồng ổn định, HR Integration hoặc Reporting có thể được cập nhật, mở rộng và phục hồi riêng, giảm phạm vi thay đổi tác động đến tuyến mở cổng.

Quyết định không xuất phát từ giả định tải quá lớn. 800/1.800 ≈ 0,44 giao dịch/giây/nhà máy chỉ là trung bình trong 30 phút, không mô tả đợt dồn tải tức thời. Monolith vẫn khả thi; nhóm chấp nhận chi phí microservices để có các biên thay đổi và vận hành nêu trên. Đạt ≤ 2 giây phải được kiểm chứng đầu-cuối, không suy ra từ tên kiến trúc.

Luồng QR gọi đồng bộ các năng lực cần thiết để xác thực, kiểm tra quyền, ghi trạng thái và điều khiển đúng cổng. Attendance quản lý bản ghi chuẩn; dữ liệu nghiệp vụ và outbox được ghi cùng transaction. Publisher phát event sau commit để Reporting, Audit và HR Integration xử lý độc lập \[P3-4\]. Timeout thiết bị giữ trạng thái chưa xác định và cần kiểm tra kết quả trước khi gửi lại.

Mỗi service chỉ ghi dữ liệu thuộc quyền sở hữu của mình; dữ liệu bên khác được lấy qua API/event. Điều này không bắt buộc mỗi service có một máy chủ vật lý riêng. Bên trong service có thể phân tầng và dùng Ports & Adapters; broker và database đều là chi tiết hạ tầng được thể hiện ở mức cần thiết trong Component View.

### 10.4 Hệ quả và đánh đổi của lựa chọn

Chi phí vận hành tăng do nhiều tiến trình, hợp đồng và điểm lỗi mạng. Thiết kế cần giới hạn service theo trách nhiệm ổn định, timeout có ngân sách và theo dõi yêu cầu bằng correlation ID. Phân quyền vẫn thực hiện tại service; gateway không thay thế kiểm tra phạm vi dữ liệu.

Reporting và HR có thể chậm hơn nguồn Attendance. Cần đo độ trễ, cảnh báo tồn đọng, retry có giới hạn và consumer chống trùng \[P3-5\]. Audit bất đồng bộ chỉ phù hợp khi dấu vết cần phát đã được lưu bền vững. Broker không tự bảo đảm mọi tác động nghiệp vụ xảy ra đúng một lần.

Kiosk offline chỉ biết quyền và danh sách thu hồi đã đồng bộ gần nhất. Cần chính sách hiệu lực cache và đối soát khi kết nối phục hồi; không cam kết thu hồi tức thời trên thiết bị mất mạng. Các ngưỡng p95/p99 nội bộ bổ trợ đo lường, không thay thế yêu cầu gốc ≤ 2 giây.

<img src="images/page-039-image-01.png" alt="Page 39 image" width="30" />

# 11 Tổng quan kiến trúc và phân rã logic

Kiến trúc TMC được tổ chức thành các giao diện sử dụng, các service nghiệp vụ và các thành phần hỗ trợ tích hợp. Web Portal/Mobile App cung cấp chức năng theo vai trò qua API Gateway; Kiosk tiếp nhận yêu cầu quét QR tại cổng. Gate Station điều phối việc xác thực, kiểm tra chính sách và điều khiển Turnstile. Attendance giữ bản ghi chấm công chuẩn, còn Reporting, Audit và HR Integration sử dụng các sự kiện được phát sau khi dữ liệu nguồn đã được lưu.

Các góc nhìn dưới đây bổ sung cho nhau: Module View giải thích đơn vị hiện thực và hướng phụ thuộc; Component-and-Connector thể hiện các thành phần phối hợp khi hệ thống chạy; Deployment View phân bổ phần mềm lên môi trường thực thi \[P3-8\]. Phần 3 thể hiện thiết kế bằng catalog, quy tắc và sơ đồ phù hợp; Sequence Diagram bổ sung thứ tự tương tác trong từng kịch bản. Ký pháp Component, interface và Sequence tham chiếu UML \[P3-6\].

Bảng 11.0.1: Phạm vi của các góc nhìn kiến trúc

| Góc nhìn | Câu hỏi cần trả lời | Thể hiện trong báo cáo |
| --- | --- | --- |
| Module View | Code và trách nhiệm được phân chia như thế nào; module nào được phép phụ thuộc module nào? | Catalog service, dữ liệu sở hữu và quy tắc phân tầng ở mục 3.2. |
| Component and Connector View | Thành phần nào cung cấp hoặc sử dụng interface; kết nối nào đồng bộ, kết nối nào truyền sự kiện? | Hình 3.1-3.3, catalog interface, connector và sự kiện ở mục 3.3. |
| Sequence Diagram | Các thành phần phối hợp theo thứ tự nào; kết quả và ngoại lệ được xử lý ra sao? | Luồng quét QR ở mục 3.4.1; luồng đồng bộ HR ở mục 3.4.2. |
| Deployment View | Các thành phần được phân bổ lên server, kiosk và mạng hai nhà máy như thế nào? | Được cụ thể hóa ở Phần 4 từ các ranh giới logic đã xác định. |

### 11.1 Các bước hình thành kiến trúc logic

Bước 1 Xác định biên hệ thống. TMC quản lý nhân viên, QR, trạm cổng, chấm công, báo cáo và quản trị trong phạm vi hai nhà máy. Turnstile và HR/Payroll là hai hệ thống nghiệp vụ ngoài. Identity, Broker và Monitoring thuộc phần hỗ trợ của TMC. Sáu actor tạo ra nhu cầu sử dụng; việc phân rã service dựa trên trách nhiệm nghiệp vụ thay vì tạo một service riêng cho mỗi actor.

Bước 2 Gom chức năng theo miền nghiệp vụ. FR-01-10 và FR-19 hình thành User/QR; FR-11-18 và FR-20 hình thành Gate Station; ghi nhận và hiệu chỉnh công thuộc Attendance; báo cáo thuộc Reporting; FR-29-30 thuộc HR Integration. Audit/Admin tập hợp chức năng truy vết và quản trị nhưng giữ các module với quyền riêng. Cách phân chia này cụ thể hóa ADR-01.

Bước 3 Xác định nguồn dữ liệu chuẩn. Attendance sở hữu bản ghi chấm công. Gate Station sở hữu chính sách và trạng thái kiểm soát ra vào; User/QR sở hữu hồ sơ và vòng đời QR. Mỗi service mở dữ liệu qua API hoặc sự kiện. Reporting xây read model riêng; HR Integration chỉ lưu dữ liệu phục vụ tích hợp và đối soát theo ADR-03, ADR-10.

Bước 4 Xác định hợp đồng và cơ chế phối hợp. Gate Station cần kết quả xác thực QR, ghi nhận lượt và điều khiển cổng qua các API đồng bộ. Sự kiện attendance đã commit được phát để Reporting, Audit và HR xử lý độc lập. Turnstile Adapter và HR Adapter đặt sau các interface ổn định, cụ thể hóa ADR-02 và ADR-04-07.

Bước 5 Kiểm tra ranh giới bằng kịch bản. Với mỗi luồng, đối chiếu dữ liệu chủ sở hữu, trạng thái trung gian, nơi ghi bền vững và thành phần chịu trách nhiệm khi timeout. Hai Sequence ở mục 3.4.1 và 3.4.2 kiểm tra thứ tự tương tác; các quy tắc ở mục 3.3.7 xử lý chống trùng, offline và sai lệch đồng bộ.

### 11.2 Catalog thành phần và quyền sở hữu dữ liệu

Bảng 11.2.1: Trách nhiệm và dữ liệu của các thành phần TMC

<img src="images/page-040-image-01.png" alt="Page 40 image" width="30" />

| Thành phần | Trách nhiệm chính | Dữ liệu và hợp đồng | Truy vết |
| --- | --- | --- | --- |
| Web Portal /Mobile App | Cung cấp QR, báo cáo và tác vụ quản trị theo vai trò. | Gọi API qua Gateway; không truy cập DB. | FR-05, FR-21-28; ADR-08 |
| Kiosk / QR Reader | Quét, hiển thị kết quả; giữ hàng đợi cục bộ khi đủ điều kiện offline. | IGateAccess; cache và offline queue. | FR-17, FR-20; NFR-09 |
| API Gateway / Identity Service | Gateway định tuyến; Identity xác thực, quản lý tài khoản và quyền. Service vẫn kiểm tra quyền. | IIdentityAccess; Identity Store. | FR-31; ADR-08 |
| User / QR Service | Quản lý hồ sơ, trạng thái, vòng đời QR; kiểm tra hạn, thu hồi và replay. | IUserQR /IQRValidation; User/QR Store. | FR-01-10, FR-19; ADR-01, ADR-09 |
| Gate Station Service | Quản lý trạm, quy tắc và anti-passback; điều phối truy cập. Sở hữu Turnstile Adapter. | IGateAccess, ITurnstile Controller; Gate Store lưu cấu hình, chính sách và trạng thái truy cập. | FR-11-18, FR-20; ADR-06 |
| Attendance Service | Ghi nhận, hiệu chỉnh có kiểm soát; sở hữu Attendance DB, Outbox và Publisher. | IAttendanceCommand / Query; AttendanceEvents. | FR-17, FR-23, FR-26; ADR-03-05 |
| Reporting Service | Cập nhật dữ liệu đọc, báo cáo và dashboard theo phạm vi được phép. | IReporting; Reporting Read Model. | FR-21-25, FR-28; ADR-10 |
| HR Integration Service | Chuyển đổi, đồng bộ, retry và đối soát; nhận hồ sơ HR nếu áp dụng A-04. | HR API; Idempotency & Sync DB. | FR-08, FR-29-30; ADR-07 |
| Audit / Admin Service | Tách module audit chỉ thêm với module quản trị tham số và điều phối sao lưu. | Audit Store; cấu hình riêng; API quản trị. | FR-27, FR-32, FR-35; ADR-08-09 |
| Message Broker | Phân phối sự kiện tới các consumer độc lập; hỗ trợ retry và DLQ. | AttendanceEvents; hàng đợi bền vững. | ADR-02, ADR-04-05 |
| Monitoring & Alerting | Theo dõi thiết bị, lỗi tích hợp, tồn đọng Outbox và hạn đồng bộ HR. | Metrics, log kỹ thuật, cảnh báo. | FR-14-15, FR-30, FR- 33-34; AC-11 |

DB/schema riêng thể hiện quyền sở hữu và quyền truy cập của từng service. Các service có thể dùng hạ tầng cơ sở dữ liệu chung ở mức triển khai nếu vẫn được cô lập tài khoản, schema và migration; chúng không đọc hoặc ghi chéo bảng nghiệp vụ. Cách phân bổ máy chủ và sản phẩm dữ liệu cụ thể được quyết định ở Phần 4. Outbox Publisher, consumer và adapter là thành phần nội bộ; mỗi hộp trong sơ đồ không mặc nhiên là một microservice mới.

Audit/Admin giữ ranh giới dịch vụ trong ADR-01 nhưng phân tách module Audit và module Admin. Module Audit quản lý nhật ký chỉ thêm; module Admin quản lý cấu hình và điều phối tác vụ vận hành theo quyền. Identity sở hữu tài khoản và vai trò; Admin sử dụng API của Identity khi cần thay đổi quyền. Quyền quản trị kỹ thuật không mặc nhiên cho phép sửa audit hoặc đọc toàn bộ dữ liệu công cá nhân.

### 11.3 Quy tắc tổ chức module trong từng service

Trong mỗi service, lớp API tiếp nhận và chuẩn hóa đầu vào; lớp Application điều phối use case và ranh giới giao dịch; lớp Domain giữ chính sách và bất biến nghiệp vụ. Các interface phục vụ lưu trữ, tích hợp và phát sự kiện được định nghĩa tại biên Application/Domain. Adapter hiện thực những interface đó bằng cơ chế kỹ thuật phù hợp. Hướng phụ thuộc được mô tả ở Bảng 3.5 và liên hệ với SRP, ISP, DIP của Phần 2.

Bảng 11.3.1: Các quan hệ trong Module View

<img src="images/page-041-image-01.png" alt="Page 41 image" width="30" />

| Quan hệ | Quy tắc áp dụng | Ví dụ TMC |
| --- | --- | --- |
| is part of | Module hoặc worker thuộc phạm vi một service, dùng quyền dữ liệu của service đó. | Outbox Publisher thuộc Attendance; Turnstile Adapter thuộc Gate Station. |
| uses or depends on | Logic ứng dụng sử dụng hợp đồng ổn định và các quy tắc miền. | Gate Application sử dụng IQRValidation, IAttendanceCommand và ITurnstileController. |
| realizes | Adapter hiện thực interface mà logic ứng dụng yêu cầu. | Turnstile Adapter hiện thực ITurnstileController; HR Adapter hiện thực port tích hợp HR. |
| allowed to use | Lớp API gọi Application; Application sử dụng Domain và port; adapter hiện thực port. | Quy tắc quyền ra vào không gọi trực tiếp driver thiết bị hoặc SQL. |
| data ownership | Chỉ thành phần thuộc service sở hữu được truy cập kho dữ liệu nội bộ. | Attendance Publisher đọc Attendance Outbox; HR đối soát qua IAttendanceQuery. |

Phụ thuộc ở mức mã nguồn đi qua interface hoặc client của hợp đồng, không import lớp Application hay Domain nội bộ của service khác. Các service có thể dùng chung thư viện tiện ích cho log, định dạng lỗi hoặc cấu trúc thông điệp nếu quản lý phiên bản tương thích. Thư viện này không nắm dữ liệu nghiệp vụ toàn hệ thống và không biến Outbox, Inbox hay transaction thành tài nguyên dùng chung giữa các service.

# 12 Góc nhìn thành phần và kết nối

Góc nhìn thành phần và kết nối mô tả các thành phần tham gia khi TMC vận hành, giao diện chúng cung cấp hoặc yêu cầu, và cách chúng phối hợp. Microservices xác lập ranh giới nghiệp vụ, dữ liệu và triển khai độc lập; Event-Driven Architecture tổ chức việc truyền các sự kiện đã được ghi nhận tới những thành phần xử lý phía sau. Cách kết hợp này cụ thể hóa ADR-01 và ADR-02, đồng thời giữ luồng quét QR có phản hồi trực tiếp.

Kiến trúc được trình bày qua ba hình: tổng quan các service; tuyến quét QR, ghi nhận và điều khiển cổng; tuyến sự kiện, báo cáo, kiểm toán và tích hợp HR. Các hình giữ cùng tên thành phần và giao diện với Sequence Diagram 3.4-3.5. Khung service biểu diễn quyền sở hữu logic, chưa xác định máy chủ hoặc công nghệ triển khai. Port và connector làm rõ bên cung cấp, bên sử dụng và hình thức tương tác \[P3-6\].

### 12.1 Sơ đồ Component tổng quan

Hình 3.1 cho thấy các service nghiệp vụ, các thành phần hỗ trợ và hai hệ thống ngoài. Đường liền biểu diễn lời gọi API theo chiều từ bên sử dụng đến bên cung cấp; đường nét đứt xanh biểu diễn hướng truyền sự kiện. Các store ghi trong thành phần thể hiện chủ sở hữu dữ liệu. Những lời gọi quản trị hoặc báo cáo qua Gateway được lược bớt để hình tập trung vào luồng chấm công và tích hợp; các hợp đồng tương ứng có trong Bảng 3.6.

<img src="images/page-042-image-02.png" alt="Page 42 image" width="653" />

*Hình 12.1.1 Tổng quan kiến trúc Microservices và Event-Driven của TMC*

<img src="images/page-042-image-01.png" alt="Page 42 image" width="30" />

Tại tuyến truy cập, User/QR cung cấp kết quả xác thực, Gate Station giữ quyết định cho phép ra vào và Attendance giữ dữ liệu chấm công. Tại tuyến sự kiện, Broker phân phối dữ liệu đã commit đến các consumer riêng. Reporting và HR có thể được thay đổi hoặc mở rộng theo tải xử lý riêng, trong khi các hợp đồng với Attendance vẫn được giữ tương thích.

### 12.2 Catalog interface

Interface diễn tả năng lực một thành phần cung cấp và thành phần khác yêu cầu. Các tên dưới đây là hợp đồng logic; endpoint, payload, phương thức xác thực và ánh xạ API nhà cung cấp được đặc tả ở Phần 4. Port trên biên service trong hình đánh dấu điểm giao tiếp; connector liên kết các điểm đó theo loại tương tác.

Bảng 12.2.1: Interface được cung cấp và được sử dụng

| Interface | Bên cung cấp | Bên sử dụng | Hợp đồng chính |
| --- | --- | --- | --- |
| IGateAccess | Gate Station | Kiosk; Guard qua Gateway | Tiếp nhận quét QR; mở thủ công chỉ theo quyền Guard, có lý do và audit. Retry giữ ID lượt truy cập. |
| IUserQR IQRValidation | User/QR | Portal, Gate Station | Tách cấp/thu hồi với xác minh QR; kiểm soát trạng thái và sử dụng lại token. |
| IAttendanceCommand IAttendanceQuery | Attendance | Gate Station; HR Integration; Portal theo quyền | Ghi lượt/kết quả; HR hiệu chỉnh, Supervisor ghi chú theo quyền. Query cung cấp dữ liệu chuẩn cho đối soát; không mở quyền ghi trực tiếp từ consumer. |

<img src="images/page-043-image-01.png" alt="Page 43 image" width="30" />

| Interface | Bên cung cấp | Bên sử dụng | Hợp đồng chính |
| --- | --- | --- | --- |
| ITurnstileController | Turnstile Adapter | Gate Station | Ánh xạ đúng cổng; chuẩn hóa OPENED, ERROR, UNKNOWN; không retry mù khi timeout. |
| AttendanceEvents | Attendance qua Broker | Reporting, Audit, HR Integration | Phát sau commit; giao ít nhất một lần; khử trùng messageId, kiểm soát version. |
| IHRPayroll /HR/ Payroll API | HR/Payroll bên ngoài | HR Integration | Gửi dữ liệu đủ điều kiện; xác nhận kết quả; đối soát, chống trùng theo hợp đồng. |
| IHRSync | HR Adapter | HR Integration Application | Port nội bộ cho gửi, tra cứu và đối soát; adapter gọi IHRPayroll của hệ thống ngoài theo khả năng API. |
| IReporting | Reporting | Portal | Chỉ đọc và xuất theo quyền; thể hiện độ trễ hoặc dữ liệu chưa đầy đủ. |
| IIdentityAccess | Identity | Gateway, các service | Xác thực, kiểm tra token và phạm vi quyền; audit thao tác nhạy cảm. |
| IOfflineAttendance Ingest | Attendance | Kiosk / bộ đồng bộ cục bộ | Nhận nhật ký đã ghi bền vững; giữ eventId, chống trùng và đối soát; không gửi lại lệnh mở cổng. |
| IGateManagement / IAuditQuery / IAdmin | Gate Station / Audit- Admin | Portal qua Gateway | Quản lý trạm, cấu hình, đọc audit theo quyền; thao tác nhạy cảm phải có nhật ký. |
| IEmployeeImport | User / QR | HR Integration | Nhập hồ sơ và trạng thái HR nếu API đáp ứng A-04/FR-08; không đọc hoặc ghi chéo DB. |

Gateway áp dụng chính sách truy cập chung; từng service vẫn kiểm tra quyền theo vai trò và phạm vi tài nguyên. Employee chỉ xem dữ liệu của mình; Supervisor giới hạn nhóm/ca; Security Guard giới hạn cổng được giao; Executive xem dữ liệu tổng hợp. HR có phạm vi nghiệp vụ xuyên hai nhà máy theo Phần 1. Các thao tác nhạy cảm phải ghi audit theo ADR-08-09.

### 12.3 Các kiểu kết nối và cơ sở lựa chọn

Bảng 12.3.1: Catalog connector của TMC

| Kiểu kết nối | Đặc tính | Ví dụ áp dụng |
| --- | --- | --- |
| API đồng bộ | Bên gọi chờ kết quả; áp dụng timeout và giới hạn quyền. | Kiosk gọi IGateAccess; Gate Station gọi IQRValidation và IAttendanceCommand. |
| API truy vấn | Đọc theo quyền, không mở quyền truy cập DB nội bộ. | Portal dùng IReporting; HR Integration dùng IAttendanceQuery để đối soát. |
| Port và adapter | Ứng dụng yêu cầu interface; adapter cung cấp hiện thực. | Turnstile Adapter hiện thực ITurnstileController rồi gọi API thiết bị. |
| Lưu trữ cục bộ của service | Ghi trạng thái và Outbox trong cùng giao dịch ACID. | Attendance lưu lượt truy cập, kết quả và sự kiện tương ứng. |
| Pub/Sub bất đồng bộ | Phát sau commit, giao ít nhất một lần, subscription độc lập. | AttendanceEvents cập nhật Reporting, Audit và dữ liệu đủ điều kiện gửi HR. |
| Đồng bộ và tích hợp | Theo dõi tiếp nhận, chống trùng và xử lý kết quả chưa xác định. | IOfflineAttendanceIngest nhập nhật ký cục bộ; IHRPayroll chuyển dữ liệu sang HR. |

Luồng quét QR sử dụng request/response để trả trạng thái truy cập trong ngân sách phản hồi tại kiosk. Các hệ quả đã được ghi bền vững được truyền theo Pub/Sub để mỗi consumer xử lý theo tốc độ riêng. Khi HR bị gián đoạn, công việc cần đồng bộ được giữ và theo dõi tại tuyến tích hợp; luồng cổng

<img src="images/page-044-image-01.png" alt="Page 44 image" width="30" />

*không chờ HR. Sự tách biệt này vẫn cần giới hạn tài nguyên, kiểm soát tồn đọng và xử lý lỗi ở các thành phần cùng dùng hạ tầng.*

Mô hình sự kiện sử dụng topology broker: producer công bố sự kiện nghiệp vụ, các consumer đăng ký loại dữ liệu cần nhận. Broker chịu trách nhiệm phân phối và xác nhận giao nhận; nó không quyết định quyền mở cổng hoặc tính hợp lệ của công. Gate Station điều phối use case tại cổng, còn HR Integration điều phối công việc đồng bộ của chính service \[P3-2\].

### 12.4 Component của luồng quét QR và điều khiển cổng

Tuyến đồng bộ bắt đầu từ Kiosk với một eventId ổn định cho lượt quét. Gate Station gọi IQRValidation để kiểm tra mã, thời hạn, trạng thái nhân viên và khả năng sử dụng token. Các quy tắc quyền ra vào, chiều cổng và anti-passback được đánh giá trong phạm vi Gate Station. QR hợp lệ chưa tự động đồng nghĩa với quyền mở cổng.

Khi yêu cầu được chấp thuận, Gate Station gọi IAttendanceCommand. Attendance lưu lượt truy cập ở trạng thái PENDING_GATE và Outbox trong cùng giao dịch; chỉ sau commit, Gate Station gọi ITurnstileController. Adapter ánh xạ đúng cổng vật lý và chuẩn hóa kết quả thiết bị. Attendance tiếp tục lưu kết quả cùng sự kiện tương ứng trước khi hoàn tất phản hồi.

GATE_OPENED xác nhận kết quả điều khiển theo contract thiết bị, chưa chứng minh người đã đi qua cảm biến. Timeout được giữ là UNKNOWN: hệ thống tra cứu kết quả, nhận callback nếu API hỗ trợ hoặc chuyển bảo vệ xác minh. Không tự gửi thêm lệnh mở. Kiosk không chờ Reporting, Audit consumer hoặc HR Integration hoàn tất, nhờ đó giới hạn số thao tác trong ngân sách hai giây.

Hình 3.2 phân rã các thành phần bên trong Gate Station và Attendance. Gate Application yêu cầu ITurnstileController, còn Turnstile Adapter hiện thực interface này. Attendance Application và Publisher cùng thuộc Attendance Service; việc Publisher đọc Outbox vì vậy không vi phạm quy tắc sở hữu dữ liệu. Các giao dịch cơ sở dữ liệu và tác động mở cổng vật lý là các bước riêng, có trạng thái trung gian để đối soát.

<img src="images/page-044-image-02.png" alt="Page 44 image" width="653" />

*Hình 12.4.1 Component của luồng quét QR và điều khiển cổng*

<img src="images/page-045-image-01.png" alt="Page 45 image" width="30" />

Nếu Attendance chưa commit được lượt truy cập, luồng online không được xác nhận ghi nhận thành công. Nếu lỗi xảy ra sau khi đã gửi lệnh mở cổng, lượt đang chờ phải tiếp tục được đối chiếu bằng khả năng của thiết bị hoặc quy trình bảo vệ. Hệ thống không suy diễn trạng thái vật lý từ việc request bị timeout, và không mở lại cổng chỉ vì chưa nhận được phản hồi.

### 12.5 Component của xử lý sự kiện và tích hợp HR

Sau commit, Outbox Publisher đọc các bản ghi chưa phát thuộc Attendance DB và gửi tới Broker. Chỉ khi Broker xác nhận tiếp nhận, Publisher mới cập nhật publishStatus. Nếu phản hồi bị mất, Publisher có thể gửi lại cùng thông điệp. Outbox bảo vệ khoảng trống giữa ghi DB và phát sự kiện; cơ chế này không thay thế việc commit dữ liệu nguồn \[P3-4\].

Reporting, Audit và HR Integration dùng subscription riêng để cùng nhận dữ liệu cần thiết. Reporting biến sự kiện thành mô hình đọc, còn Audit lưu lịch sử phục vụ truy vết. HR chỉ tiêu thụ các bản ghi chấm công đã được xác nhận đủ điều kiện. AccessAttemptRecorded và kết quả cổng chưa xác định có thể phục vụ theo dõi vận hành nhưng không được tính thành dữ liệu công đã chốt.

HR Integration chuyển đổi dữ liệu qua HR Adapter và IHRPayroll, lưu trạng thái tiếp nhận vào DB của mình. Khi chốt kỳ, service truy vấn Attendance qua IAttendanceQuery rồi so sánh với lịch sử đồng bộ. Batch và bản hiệu chỉnh phải có ngữ nghĩa cập nhật, đối soát rõ ràng để không ghi trùng dữ liệu đã gửi theo sự kiện.

Hình 3.3 cho thấy ba consumer với dữ liệu riêng: Reporting cập nhật read model, Audit lưu dấu vết, HR Integration quản lý đồng bộ và đối soát. Các service phát sinh thay đổi quyền, QR hoặc cấu hình cũng lưu audit bền vững tại nguồn rồi chuyển tiếp; Attendance không phải nguồn duy nhất của nhật ký toàn hệ thống. Bản ghi chống trùng của consumer thuộc service tương ứng.

<img src="images/page-045-image-02.png" alt="Page 45 image" width="653" />

*Hình 12.5.1 Component của xử lý sự kiện và tích hợp HR*

<img src="images/page-046-image-01.png" alt="Page 46 image" width="30" />

Mỗi nhóm consumer dùng subscription riêng để Reporting, Audit và HR đều nhận dữ liệu cần thiết. Nhiều instance của cùng một consumer có thể chia việc trong subscription của mình. Chỉ ACK sau khi service đã xử lý bền vững hoặc đã tiếp nhận công việc vào kho riêng theo hợp đồng; ACK tiếp nhận không đồng nghĩa HR/Payroll đã đồng bộ thành công.

### 12.6 Danh mục sự kiện và dữ liệu trao đổi

Bảng 3.8 định nghĩa các loại sự kiện trong hợp đồng AttendanceEvents và luồng audit. Chúng cụ thể hóa thông điệp publishAttendanceEvent trong Sequence ở mức thiết kế. Mỗi sự kiện công bố một thay đổi đã được lưu; lệnh yêu cầu mở cổng vẫn đi qua ITurnstileController. Các tên sự kiện có thể được ánh xạ thành loại thông điệp tương ứng trong đặc tả API của Phần 4.

Bảng 12.6.1: Danh mục sự kiện nghiệp vụ

| Sự kiện | Producer | Consumer | Ý nghĩa |
| --- | --- | --- | --- |
| AccessAttemptRecorded | Attendance | Audit; Reporting vận hành | Thông báo lượt hợp lệ đã được lưu ở PENDING_GATE; chưa đủ điều kiện gửi HR. |
| GateResultRecorded | Attendance | Audit; Reporting vận hành | Ghi nhận OPENED, ERROR hoặc UNKNOWN; bảo toàn mức chắc chắn của kết quả thiết bị. |
| AttendanceConfirmed | Attendance | Reporting, HR Integration, Audit | Công bố bản ghi đã đáp ứng quy tắc xác nhận chấm công để xử lý tiếp. |
| AttendanceCorrected | Attendance | Reporting, HR Integration, Audit | Công bố phiên bản hiệu chỉnh được phép; giữ liên hệ với giao dịch gốc. |
| AuditRecorded | Service phát sinh thao tác | Audit/Admin | Chuyển dấu vết đã lưu bền vững về quyền, QR, cấu hình hoặc truy cập bị từ chối. |

Thông điệp có messageId, eventType, schemaVersion, eventId/aggregateId, aggregateVersion, occurredAt, correlationId, source và phạm vi nhà máy/cổng khi phù hợp. eventId nhận diện lượt truy cập; messageId nhận diện một thông điệp cụ thể. Payload chứa dữ liệu tối thiểu cần thiết cho consumer, không mang token QR thô hoặc các trường hồ sơ không phục vụ mục đích xử lý (ADR-05, ADR-09).

Quy tắc xác nhận được chọn cho thiết kế M1 là: ở luồng QR thông thường, mã và quyền truy cập phải hợp lệ, Turnstile trả OPENED, và Attendance đã commit kết quả GATE_OPENED. Khi đó Attendance công bố AttendanceConfirmed với thời điểm, nhà máy và chiều vào/ra tương ứng. Dữ liệu này xác nhận giao dịch truy cập đã được chấp nhận và mở cổng theo contract; nếu nghiệp vụ yêu cầu chứng cứ người thực sự đi qua, phải bổ sung tín hiệu cảm biến làm điều kiện xác nhận trong Phần 4. Các lượt ERROR, UNKNOWN hoặc mở thủ công chờ xác minh không tự động trở thành công hợp lệ.

AttendanceCorrected mang phiên bản mới và quan hệ với giao dịch gốc; nếu hiệu chỉnh làm mất hiệu lực dữ liệu đã gửi HR, HR Integration phải gửi điều chỉnh tương ứng thay vì bỏ qua. Reporting thể hiện thời điểm cập nhật và tình trạng chưa đối soát khi dữ liệu còn tồn đọng.

### 12.7 Tính nhất quán và xử lý ngoại lệ

Outbox thuộc từng chủ sở hữu dữ liệu. Attendance Publisher chỉ đọc Outbox của Attendance. User/QR, Gate Station hoặc Identity khi phát audit cũng phải ghi bền vững tại nguồn rồi chuyển tiếp; không dùng chung Attendance DB. Thư viện messaging có thể dùng chung mã nguồn, nhưng mỗi service giữ transaction, quyền truy cập và trạng thái xử lý của mình.

eventId nhận diện giao dịch; messageId nhận diện một thông điệp; aggregateVersion phân biệt các lần cập nhật. Retry giữ nguyên messageId. Consumer ghi dấu đã xử lý và cập nhật dữ liệu trong cùng

<img src="images/page-047-image-01.png" alt="Page 47 image" width="30" />

giao dịch cục bộ, khử trùng theo consumer-messageId và không áp dụng phiên bản cũ lên trạng thái mới \[P3-5\]. PENDING_GATE của nghiệp vụ khác PENDING của publishStatus.

Khi offline, cache và nhật ký bền vững tại kiosk hỗ trợ chế độ dự phòng của UC-08. Cần lưu ý định trước thao tác cổng rồi bổ sung kết quả; chỉ thông báo đã lưu cục bộ sau khi ghi an toàn. Phục hồi dùng IOfflineAttendanceIngest với ID cũ, đối soát theo thời điểm ghi nhận và chính sách áp dụng, không phát lại requestAccess để mở thêm lượt.

Cache quá hạn, thiếu dữ liệu hoặc nhật ký lỗi/đầy phải chặn cấp quyền tự động và chuyển UC-06. Mất kết nối giữa kiosk làm giới hạn khả năng kiểm soát QR dùng lại và thu hồi tức thì; không dùng Reporting có độ trễ để quyết định anti-passback. Các ngoại lệ sau đồng bộ phải được giữ vết, xác minh trước khi thành dữ liệu chấm công chuẩn.

HR timeout không chứng minh hệ thống ngoài chưa ghi dữ liệu. Gửi lại an toàn cần HR hỗ trợ khóa idempotency, upsert hoặc tra cứu kết quả. Lỗi tạm thời được retry có giới hạn; lỗi nghiệp vụ được giữ để sửa; hết ngưỡng retry chuyển DLQ và cảnh báo. Chỉ đánh dấu SYNCED sau xác nhận, tiếp tục đối soát trước 12:00 ngày 1.

Quy tắc phiên bản áp dụng cho trạng thái hiện hành và projection: phiên bản cũ không ghi đè phiên bản mới. Audit vẫn giữ các sự kiện hợp lệ khác nhau để bảo toàn lịch sử. Các khoảng thiếu dữ liệu cần được đối chiếu với Attendance; consumer không suy diễn rằng các bản tin khác loại đều phải được nhận trong cùng subscription.

Kiểm tra QR và anti-passback phối hợp bằng định danh lượt quét ổn định. User/QR thực hiện bước tiêu thụ token nguyên tử trong dữ liệu của mình; Gate Station thực hiện bước giữ chỗ trạng thái truy cập nguyên tử trong dữ liệu của Gate. Hai bước không dùng transaction chung xuyên hai service. Retry cùng lượt trả kết quả đã biết hoặc tiếp tục từ trạng thái đã lưu. Gate Station kiểm tra trạng thái của mình, không dựa vào read model báo cáo có độ trễ. Khi kết quả thiết bị là UNKNOWN, hệ thống không tự giải phóng giữ chỗ theo cách cho phép mở lại cổng trước khi đối soát.

Trong hai Sequence ở mục 3.4.1 và 3.4.2, eventId là định danh giao dịch ở mức khái quát; hiện thực checkEventStatus và markOutboxPublished phải dùng messageId hoặc khóa giao dịch kèm phiên bản phù hợp. PENDING_GATE là trạng thái attendance, còn PENDING/PUBLISHED là trạng thái Outbox. Mốc 08:00 của AC-07 là mục tiêu nội bộ; hạn bắt buộc vẫn là trước 12:00 ngày 1 hằng tháng.

Trong Sequence HR, FAILED_RETRY_LATER biểu diễn trạng thái công việc chờ xử lý tiếp. Nếu nguyên nhân là timeout, kết quả tại HR/Payroll vẫn là UNKNOWN; nhãn này không chứng minh hệ thống đích chưa ghi dữ liệu. Trước khi gửi lại, HR Integration phải tra cứu kết quả hoặc dùng khóa idempotency/upsert theo contract. SYNCED chỉ được ghi khi kết quả đã được xác nhận.

Các hình Sequence được giữ để mô tả thứ tự phối hợp chính. Trong hình, vùng External Systems chứa Broker/Monitoring là cách nhóm lifeline; theo ranh giới Component, hai thành phần đó thuộc hạ tầng TMC. Hệ thống nghiệp vụ ngoài là Turnstile và HR/Payroll.

### 12.8 Truy vết yêu cầu và tiêu chí kiểm chứng

Bảng 3.9 liên kết quyết định với cơ chế thể hiện trong Component và tình huống cần kiểm chứng khi hiện thực. Các tiêu chí này mô tả kế hoạch kiểm chứng kiến trúc, chưa phải kết quả đo hiệu năng hoặc kiểm thử hệ thống.

Bảng 12.8.1: Truy vết từ yêu cầu đến cơ chế kiến trúc

| Yêu cầu và ADR | Cơ chế thiết kế | Tình huống cần kiểm chứng |
| --- | --- | --- |
| NFR-06; ADR-02 | Tuyến đồng bộ ngắn; Reporting/HR xử lý qua sự kiện. | Đo thời gian kiosk đầu-cuối ở tải giao ca; kiểm tra số lượt vượt 2 giây và ảnh hưởng khi HR chậm. |

<img src="images/page-048-image-01.png" alt="Page 48 image" width="30" />

| Yêu cầu và ADR | Cơ chế thiết kế | Tình huống cần kiểm chứng |
| --- | --- | --- |
| NFR-07; ADR-03-05 | Attendance và Outbox commit cùng transaction; consumer chống trùng. | Ngắt broker sau commit, làm mất ACK và giao lại thông điệp; dữ liệu chuẩn không mất, projection không nhân đôi. |
| NFR-08; ADR-07 | HR có sync log, retry và scheduler đối soát nguồn chuẩn. | HR phục hồi sau gián đoạn; sự kiện trùng, hiệu chỉnh và batch cuối kỳ hội tụ đúng trước hạn. |
| NFR-01-05; ADR-08-09 | RBAC theo tài nguyên, bảo vệ QR, audit bền vững tại nguồn. | Thử truy cập ngoài phạm vi vai trò/nhà máy/cổng; kiểm tra truy vết và mức dữ liệu có trong event. |
| NFR-09; UC-08 | Cache và journal cục bộ; IOfflineAttendance Ingest giữ định danh. | Mất mạng, khởi động lại kiosk, journal đầy/cache cũ và đồng bộ lặp; không tự phát lại lệnh mở cổng. |
| ADR-01, ADR-06, ADR- 10 | Service sở hữu DB/schema; tích hợp qua interface và adapter. | Thay adapter hoặc phiên bản HR Integration; kiểm tra hợp đồng và chặn truy cập chéo DB. |

<table>
<tbody>
<tr><td>13</td><td>Mô hình hóa luồng tương tác nghiệp vụ</td></tr>
<tr><td>13.1</td><td>Luồng quét QR mở cổng</td></tr>
<tr><td>13.1.1</td><td>Mục tiêu và phạm vi xử lý</td></tr>
</tbody>
</table>

Luồng quét mã QR và kiểm soát truy cập cổng mô tả quá trình nhân viên sử dụng mã QR để xác thực danh tính, kiểm tra quyền ra vào và thực hiện ghi nhận chấm công tại cổng kiểm soát. Đây là luồng nghiệp vụ có yêu cầu phản hồi nhanh và cần bảo đảm rằng quyết định cho phép truy cập được đưa ra dựa trên kết quả xác thực và các quy tắc truy cập hợp lệ. Luồng xử lý bao gồm hai giai đoạn chính:

- Giai đoạn xử lý đồng bộ: Tiếp nhận yêu cầu quét QR, xác thực mã, kiểm tra quyền truy cập, ghi nhận lượt truy cập và điều khiển thiết bị cổng. Kết quả được phản hồi cho nhân viên thông qua thiết bị quét.

- Giai đoạn xử lý bất đồng bộ: Phát các sự kiện chấm công đã được ghi nhận đến Message Broker để các dịch vụ liên quan tiếp tục xử lý, chẳng hạn như báo cáo, tích hợp HR/Payroll và kiểm toán.

Việc tách hai giai đoạn giúp giới hạn các thao tác cần hoàn thành trong luồng truy cập trực tiếp, đồng thời cho phép các nghiệp vụ phụ trợ hoạt động độc lập hơn với quá trình mở cổng.

### 13.1.2 Các thành phần kiến trúc và chức năng

Luồng quét mã QR và kiểm soát truy cập cổng được tổ chức thông qua sự phối hợp giữa các thành phần tiếp nhận yêu cầu, xác thực danh tính, ghi nhận chấm công và điều khiển thiết bị vật lý. Kiến trúc phân tách các trách nhiệm này nhằm giảm sự phụ thuộc giữa nghiệp vụ kiểm soát truy cập, lưu trữ dữ liệu và giao tiếp với hệ thống Turnstile.

Bảng 13.1.1: Các thành phần kiến trúc trong luồng quét QR và kiểm soát truy cập cổng

| STT | Thành phần | Chức năng |
| --- | --- | --- |
| 1 | Employee | Khởi tạo yêu cầu truy cập bằng cách sử dụng mã QR cá nhân tại cổng kiểm soát. |
| 2 | Kiosk / QR Reader | Quét mã QR và gửi yêu cầu truy cập đến Gate Station Service, bao gồm thông tin mã QR, định danh cổng và mã sự kiện. |

<img src="images/page-049-image-01.png" alt="Page 49 image" width="38" />

| STT | Thành phần | Chức năng |
| --- | --- | --- |
| 3 | Gate Station Service | Điều phối quá trình xử lý yêu cầu truy cập, tiếp nhận kết quả xác thực, đánh giá các quy tắc truy cập, khởi tạo yêu cầu điều khiển cổng và trả kết quả về Kiosk. |
| 4 | User / QR Service | Xác thực mã QR dựa trên tính hợp lệ và thời hạn sử dụng; trả về kết quả xác thực cùng định danh nhân viên khi yêu cầu hợp lệ. |
| 5 | Attendance Service | Quản lý việc ghi nhận sự kiện chấm công, cập nhật trạng thái xử lý và phối hợp lưu trữ dữ liệu chấm công cùng thông tin sự kiện cần phát. |
| 6 | Attendance DB (Attendance + Outbox) | Lưu trữ bản ghi chấm công và sự kiện Outbox. Theo cơ chế Transactional Outbox, hai loại dữ liệu được ghi trong cùng một giao dịch cơ sở dữ liệu nhằm đảm bảo tính nhất quán khi lưu trữ. |
| 7 | Turnstile Adapter | Đóng vai trò trung gian giữa Gate Station Service và Turnstile System, chuyển đổi yêu cầu điều khiển sang giao diện tương thích và chuyển tiếp kết quả phản hồi. |
| 8 | Turnstile System | Tiếp nhận và thực thi yêu cầu điều khiển cổng; cung cấp kết quả thực thi khi có thể xác nhận trạng thái hoạt động. |
| 9 | Outbox Publisher | Truy xuất các sự kiện Outbox đang chờ xử lý, phát sự kiện đến Message Broker và cập nhật trạng thái phát dựa trên kết quả xác nhận từ Broker. |
| 10 | Message Broker | Tiếp nhận và phân phối các sự kiện chấm công đến những thành phần tiêu thụ phục vụ xử lý bất đồng bộ. |

### 13.1.3 Mối quan hệ giữa các thành phần

- Điều phối và xác thực truy cập: Gate Station Service đóng vai trò điều phối luồng xử lý, tiếp nhận yêu cầu từ Kiosk/QR Reader, phối hợp với User/QR Service để xác thực mã QR và kiểm tra điều kiện truy cập trước khi yêu cầu điều khiển cổng.

- Ghi nhận chấm công và điều khiển cổng: Attendance Service chịu trách nhiệm quản lý dữ liệu chấm công, trong khi Turnstile Adapter đảm nhiệm giao tiếp với hệ thống cổng vật lý. Việc tách biệt hai thành phần giúp giới hạn sự phụ thuộc giữa nghiệp vụ chấm công và giao thức của thiết bị.

- Đảm bảo tính nhất quán dữ liệu: Attendance DB lưu bản ghi chấm công và thông tin sự kiện Outbox trong cùng một giao dịch. Cơ chế Transactional Outbox giúp hạn chế tình trạng dữ liệu đã được lưu nhưng sự kiện chưa được ghi nhận để phát đi.

- Phân phối sự kiện bất đồng bộ: Outbox Publisher lấy các sự kiện đang chờ xử lý và gửi đến Message Broker. Broker phân phối sự kiện cho các dịch vụ liên quan, giúp tách biệt luồng kiểm soát cổng với các tác vụ xử lý phía sau.

<img src="images/page-050-image-01.png" alt="Page 50 image" width="30" />

*13.1.4 Sequence diagram*

<img src="images/page-050-image-02.png" alt="Page 50 image" width="637" />

*Hình 13.1.1 Sequence diagram của luồng quét QR mở cổng*

<img src="images/page-051-image-01.png" alt="Page 51 image" width="38" />

### 13.1.5 Bảng đặc tả luồng xử lý

- a. Xác thực QR và kiểm soát truy cập cổng

```text
Luồng xử lý Luồng tương tác Mô tả xử lý & Xử lý ngoại lệ
1.1–1.2.yêu cầu Tiếp nhận EmployeescanQR(qrToken)requestAccess(qrToken, gateId, eventId)→ Kiosk→ Gate Station Service:/QR Reader: •• Nhân viên quét mã QR tại Kiosk.Thiết bị tiếp nhận mã, tạo eventId để định danh lượt yêucầu và gửi thông tin gồm qrToken, gateId và eventId đếnGate Station Service để xử lý.
1.3–1.5.QR Xác thực GateService:validationResult(isValid, employeeUuid)StationvalidateQR(qrToken)Service ↔ User /QR→ •• Gate Station Service gửi qrToken đến User/QR ServiceNếu mã QR không hợp lệ hoặc đã hết hạn, hệ thống ghiđể kiểm tra tính hợp lệ và xác định nhân viên tương ứng.nhận sự kiện bảo mật, trả về DENIED_INVALID_QRvà kết thúc yêu cầu mà không thực hiện các bước tiếptheo.
1.6–1.8.quyền truy cậpKiểm tra GateevaluateAccessRules(employeeUuid,gateId) → accessDecisionStation Service: •• Sau khi xác thực thành công, Gate Station Service kiểmNếu nhân viên không đáp ứng điều kiện truy cập, hệtra quyền ra vào dựa trên thông tin nhân viên, cổng vàcác quy tắc áp dụng theo ca làm việc hoặc nhà máy.thống ghi nhận lý do từ chối, trả về DENIED_POLICYvà không gửi lệnh mở cổng.
1.9.truy cậpGhi nhận lượt Gate Station Service→saveAttendance DB: recordAttempt(...)AttemptAndOutb→oxAttendance Service(PENDING_GAT→E) •• Khi yêu cầu được chấp thuận, Gate Station Service gửiDịch vụ này lưu bản ghi chấm công và sự kiện Outbox ởthông tin lượt truy cập đến Attendance Service.trạng thái PENDING_GATE trong cùng một giao dịchcơ sở dữ liệu, nhằm bảo đảm dữ liệu và sự kiện tươngứng được ghi nhận nhất quán trước khi điều khiển cổng.
1.10. Điều khiển mởcổng Gate Station Service→eventId)Kết quả: OPENED / ERROR / TIMEOUTTurnstile System: openTurnstile(gateId,→ openGate(hardwareGateId)→ Turnstile Adapter→ ••• Gate Station Service gửi lệnh mở cổng thông quaAdapter chuyển đổi yêu cầu sang giao thức phù hợp vớiNếu thiết bị báo lỗi hoặc không phản hồi đúng thời hạn,Turnstile Adapter.thiết bị và chuẩn hóa kết quả trả về thành OPENED,ERROR hoặc TIMEOUT.hệ thống chuyển sang xử lý ngoại lệ và không mặc địnhrằng cổng chắc chắn đã đóng.
1.11–1.15. Cập nhậttrạnghồi thái và phản GateService:recordGateError(eventId)ServiceStation→ Kiosk: displayResult(...)recordGateOpened(eventId)Service →→ Gate StationAttendance/••• Nếu cổng mở thành công, Gate Station Service yêuNếu thao tác thất bại, hệ thống ghi nhận trạng thái lỗi;Không tự động gửi lại lệnh mở cổng khi chưa xác minhcầuGATE_OPENED.nếu xảy ra timeout và chưa xác định được trạng thái thựctế của cổng, hệ thống ghi nhận trạng thái chưa xác địnhđể tiếp tục kiểm tra.kết quả vật lý. Cuối cùng, Kiosk hiển thị kết quả tươngứng cho nhân viên.Attendance Service cập nhật trạng thái thành
```

- b. Xử lý sự kiện bất đồng bộ

| Luồng xử lý | Luồng tương tác | Mô tả xử lý & Xử lý ngoại lệ |
| --- | --- | --- |
| 2.1. Truy xuất sự kiện đang chờ | Outbox Publisher ↔ Attendance DB: fetchPendingEvents() → pendingEvents | • Tiến trình nền định kỳ lấy các sự kiện Outbox có trạng thái PENDING để chuẩn bị gửi đến Message Broker. |

<img src="images/page-052-image-01.png" alt="Page 52 image" width="30" />

| Luồng xử lý | Luồng tương tác | Mô tả xử lý & Xử lý ngoại lệ |
| --- | --- | --- |
| 2.2–2.4. Phát hành sự kiện và xử lý kết quả | Outbox Publisher → Message Broker: publishAttendanceEvent(eventId, status) → kết quả xác nhận; Outbox Publisher → Attendance DB: markOutboxPublished(eventId) | • Gửi thành công: Khi Message Broker xác nhận tiếp nhận sự kiện, Outbox Publisher cập nhật trạng thái bản ghi thành PUBLISHED. • Gửi thất bại hoặc quá thời gian chờ: Giữ trạng thái PENDING để thử gửi lại. Các dịch vụ nhận sử dụng eventId để nhận diện và tránh xử lý trùng lặp. |

<table>
<tbody>
<tr><td>13.2</td><td>Sequence diagram - Luồng đẩy dữ liệu HR</td></tr>
<tr><td>13.2.1</td><td>Mục tiêu và phạm vi xử lý</td></tr>
</tbody>
</table>

Luồng đồng bộ dữ liệu chấm công với HR/Payroll chịu trách nhiệm chuyển dữ liệu từ hệ thống chấm công nội bộ sang hệ thống nhân sự và tính lương. Luồng này phải hỗ trợ việc tiếp nhận sự kiện, kiểm soát trùng lặp, chuyển đổi dữ liệu theo hợp đồng tích hợp, xử lý lỗi và theo dõi trạng thái đồng bộ. Ngoài việc xử lý từng sự kiện, hệ thống cần đối soát dữ liệu chấm công trước khi chốt kỳ công. Mục tiêu của bước đối soát là xác định các bản ghi chưa được đồng bộ, bị từ chối hoặc còn trạng thái chưa hoàn tất, từ đó thực hiện đồng bộ bổ sung hoặc hiệu chỉnh theo quy trình nghiệp vụ. Luồng được chia thành hai giai đoạn:

- Giai đoạn 1 – Tiếp nhận và đồng bộ dữ liệu chấm công: Xử lý từng sự kiện được phát qua Message Broker và đồng bộ dữ liệu sang HR/Payroll.

- Giai đoạn 2 – Đối soát dữ liệu và chốt kỳ công: So sánh dữ liệu chấm công chính thức với trạng thái đồng bộ, xử lý các sai lệch và tổng hợp kết quả trước thời hạn chốt công.

### 13.2.2 Các thành phần kiến trúc và chức năng

Luồng tích hợp HR/Payroll và đối soát định kỳ được thiết kế nhằm tách biệt quá trình ghi nhận chấm công khỏi quá trình truyền dữ liệu đến hệ thống bên ngoài. HR Integration Service đóng vai trò trung tâm trong việc tiếp nhận sự kiện, quản lý trạng thái đồng bộ, giao tiếp với HR/Payroll và thực hiện đối soát dữ liệu theo kỳ. Cơ chế này giúp hạn chế sự phụ thuộc trực tiếp của hoạt động chấm công tại cổng vào khả năng sẵn sàng của hệ thống HR/Payroll.

Bảng 13.2.1: Các thành phần kiến trúc trong luồng tích hợp HR/Payroll và đối soát định kỳ

| STT | Thành phần | Chức năng |
| --- | --- | --- |
| 1 | Message Broker | Phân phối sự kiện chấm công đến HR Integration Service, hỗ trợ xử lý bất đồng bộ giữa hệ thống TMC và hệ thống HR/Payroll bên ngoài. |
| 2 | HR Integration Service | Tiếp nhận và xử lý sự kiện chấm công; kiểm tra trạng thái đồng bộ; xác thực, chuyển đổi dữ liệu theo hợp đồng API; gửi dữ liệu đến HR/Payroll; xử lý lỗi và điều phối quá trình đối soát định kỳ. |
| 3 | HR Integration DB (Idempotency & Sync Log) | Lưu trữ trạng thái xử lý sự kiện, thông tin phục vụ chống xử lý trùng, nhật ký đồng bộ và trạng thái đối soát. Dữ liệu này hỗ trợ truy vết, xác định các bản ghi cần xử lý lại và theo dõi kết quả tích hợp. |
| 4 | Attendance Service (System of Attendance) | Cung cấp dữ liệu chấm công chuẩn để HR Integration Service truy xuất và đối chiếu trong quá trình đối soát. Đây là nguồn dữ liệu chuẩn của các giao dịch chấm công trong hệ thống TMC. |

<img src="images/page-053-image-01.png" alt="Page 53 image" width="30" />

| STT | Thành phần | Chức năng |
| --- | --- | --- |
| 5 | HR/Payroll System (External API) | Tiếp nhận dữ liệu chấm công thông qua API và phản hồi kết quả xử lý. Trong phạm vi đề tài, hệ thống TMC chỉ chịu trách nhiệm cung cấp dữ liệu chấm công, không thực hiện tính toán bảng lương. |
| 6 | Monitoring & Alerting | Tiếp nhận thông tin về lỗi đồng bộ và đối soát, hỗ trợ giám sát trạng thái xử lý và phát cảnh báo khi xảy ra sự cố hoặc khi tiến độ xử lý có nguy cơ không đáp ứng thời hạn yêu cầu. |

### 13.2.3 Mối quan hệ giữa các thành phần

- Tiếp nhận và quản lý sự kiện: Message Broker đóng vai trò trung gian phân phối sự kiện chấm công đến HR Integration Service. HR Integration DB lưu trạng thái xử lý và thông tin cần thiết để kiểm tra sự kiện trùng lặp thông qua eventId.

- Tích hợp với hệ thống HR/Payroll: HR Integration Service chịu trách nhiệm kiểm tra, chuyển đổi dữ liệu theo hợp đồng API và gửi thông tin chấm công đến HR/Payroll System. Việc tách riêng thành phần tích hợp giúp hạn chế sự phụ thuộc trực tiếp giữa hệ thống chấm công và hệ thống bên ngoài.

- Đối soát dữ liệu: Attendance Service là nguồn dữ liệu chấm công chuẩn để HR Integration Service đối chiếu với trạng thái đồng bộ. Quá trình này hỗ trợ phát hiện các bản ghi còn thiếu, đồng bộ thất bại hoặc dữ liệu chưa khớp trước khi hoàn tất đối soát định kỳ.

- Giám sát và xử lý sự cố: Monitoring & Alerting tiếp nhận thông tin lỗi và kết quả đối soát để phát cảnh báo khi cần. Cơ chế này hỗ trợ theo dõi trạng thái tích hợp, phát hiện nguy cơ chậm tiến độ và xử lý các trường hợp chưa hoàn tất trước thời hạn yêu cầu.

### 13.2.4 Sequence diagram

<img src="images/page-054-image-02.png" alt="Page 54 image" width="436" />

*Hình 13.2.1 Sequence diagram của luồng đồng bộ HR và đối soát*

<img src="images/page-054-image-01.png" alt="Page 54 image" width="30" />

<img src="images/page-055-image-01.png" alt="Page 55 image" width="38" />

### 13.2.5 Bảng đặc tả luồng xử lý

- a. Tiếp nhận và đồng bộ dữ liệu chấm công

| Luồng xử lý | Thành phần tham gia | Mô tả xử lý & Xử lý ngoại lệ |
| --- | --- | --- |
| 1.1–1.3. Tiếp nhận và kiểm tra sự kiện | Message Broker → HR Integration Service: consumeAttendanceEvent() ↔ HR Integration DB: checkEventStatus() | • HR Integration Service tiếp nhận sự kiện chấm công từ Message Broker và kiểm tra trạng thái xử lý theo eventId. • Nếu sự kiện đã được đồng bộ thành công, hệ thống bỏ qua việc xử lý lại và gửi ACK_DUPLICATE để xác nhận sự kiện trùng lặp. |
| 1.4–1.6. Chuẩn bị và gửi dữ liệu | HR Integration Service ↔ HR Integration DB: saveProcessingStatus() → HR/Payroll System: submitAttendance() | • Hệ thống ghi nhận trạng thái đang xử lý, kiểm tra tính hợp lệ và chuyển đổi dữ liệu theo hợp đồng API của HR/ Payroll. • Sau đó, HR Integration Service gửi dữ liệu đến hệ thống đích kèm eventId để hỗ trợ nhận diện giao dịch và xử lý trùng lặp. |
| 1.6.1–1.8. Xử lý khi gửi thành công | HR/Payroll System → HR Integration Service: success() → HR Integration DB: updateStatus(SYNCED) → Message Broker: ACK_PROCESSED | • Khi HR/Payroll xác nhận tiếp nhận dữ liệu thành công, HR Integration Service cập nhật trạng thái sự kiện thành SYNCED và gửi ACK_PROCESSED đến Message Broker để xác nhận hoàn tất xử lý. |
| 1.9–1.12. Xử lý lỗi dữ liệu | HR/Payroll System → HR Integration Service: rejection() → HR Integration DB: saveRejectedStatus() → Monitoring & Alerting: emitSyncFailure() → Message Broker: ACK_REJECTED | • Khi HR/Payroll từ chối dữ liệu do lỗi nghiệp vụ, hệ thống lưu trạng thái bị từ chối cùng nguyên nhân cụ thể, đồng thời phát cảnh báo để hỗ trợ kiểm tra và hiệu chỉnh. • Sau đó, hệ thống gửi ACK_REJECTED nhằm tránh tự động thử lại những yêu cầu không thể thành công nếu chưa sửa dữ liệu. |
| 1.13–1.16. Xử lý lỗi tạm thời | HR/Payroll System → HR Integration Service: timeout / unavailable → HR Integration DB: updateStatus(FAILED_RETRY_LATER) → Monitoring & Alerting /DLQ: deferOrDeadLetter() | • Khi xảy ra timeout hoặc HR/Payroll tạm thời không khả dụng, hệ thống cập nhật trạng thái thành FAILED_RETRY_LATER và thực hiện thử lại theo chính sách đã cấu hình. • Các lỗi cần theo dõi được gửi đến Monitoring & Alerting. Nếu vượt quá giới hạn thử lại, sự kiện được chuyển vào Dead Letter Queue (DLQ) để phục vụ điều tra và xử lý tiếp. |

- b. Đối soát dữ liệu và chốt kỳ công

| Luồng xử lý | Thành phần tham gia | Mô tả xử lý & Xử lý ngoại lệ |
| --- | --- | --- |
| 2.1–2.4. Đối soát dữ liệu chấm công | HR Integration Service → Attendance Service: getCanonicalAttendance(...) → HR Integration DB: getSyncStatuses(...) → HR Integration Service: reconcileAndBuildBatch(...) | • Khi bắt đầu chốt kỳ, HR Integration Service lấy dữ liệu chấm công chuẩn từ Attendance Service và trạng thái đồng bộ từ HR Integration DB. • Hệ thống đối chiếu các dữ liệu này để xác định bản ghi còn thiếu, chưa đồng bộ thành công hoặc có sai lệch cần xử lý. |
| 2.5–2.6. Gửi dữ liệu chốt kỳ | HR Integration Service → HR/Payroll System: submitPayrollBatch(...) → HR Integration DB: saveClosingStatus(SYNCED) | • Sau khi dữ liệu được đối soát đầy đủ và đáp ứng các điều kiện chốt kỳ, HR Integration Service gửi gói dữ liệu tổng hợp đến HR/Payroll. • Khi hệ thống đích xác nhận tiếp nhận thành công, dịch vụ lưu trạng thái chốt kỳ tương ứng. |

<img src="images/page-056-image-01.png" alt="Page 56 image" width="38" />

| Luồng xử lý | Thành phần tham gia | Mô tả xử lý & Xử lý ngoại lệ |
| --- | --- | --- |
| 2.7–2.12. Xử lý sai lệch và đồng bộ bổ sung | HR Integration Service: identifyMissingOrFailedRecords() → HR/ Payroll System: submitCorrectionBatch(...) → HR Integration DB / Monitoring & Alerting | • Đối với các bản ghi thiếu hoặc chưa đồng bộ thành công, hệ thống xác định dữ liệu cần bổ sung và gửi lại đến HR/ Payroll. • Nếu tiếp tục xảy ra lỗi hoặc timeout, hệ thống ghi nhận trạng thái chờ thử lại, phát cảnh báo và tiếp tục xử lý theo chính sách retry nhằm khắc phục sai lệch trước thời hạn chốt kỳ. |
| 2.13. Tổng hợp kết quả đối soát | HR Integration Service → Monitoring & Alerting: publishReconciliationSummary(...) | • HR Integration Service tổng hợp kết quả đối soát, bao gồm trạng thái xử lý và các sai lệch còn tồn đọng, sau đó gửi thông tin đến Monitoring & Alerting để theo dõi. • Kết quả được lưu vết nhằm hỗ trợ kiểm tra, truy xuất và kiểm toán khi cần thiết. |

<img src="images/page-057-image-01.png" alt="Page 57 image" width="30" />

# 14

Quy trình từng bước xây dựng Kiến trúc Vật lý và Sơ đồ triển khai 14.1

Bước 1 \- Xác định các ràng buộc vật lý, vị trí địa lý và đặc tính \- AC

Trước khi bố trí máy chủ và thiết bị, kiến trúc sư thu thập toàn bộ các thông số vật lý bất biến từ bài toán của TMC: 14.1.1 Phân bố địa lý:

- Central Site: Đặt tại trụ sở chính, nơi vận hành hệ thống HR/Payroll System hiện hữu và là điểm truy cập tập trung cho quản trị viên, nhân sự, quản lý và ban giám đốc.

- Factory Site 1 \- Nhà máy Bình Dương: Quy mô 800 nhân viên, tải đỉnh giờ đổi ca đạt 800 giao dịch trong 30 phút ( 27 giao dịch/phút trung bình, nhưng thực tế dồn cục bộ trong 5-10 phút đầu ca). Có sẵn hệ thống Turnstile Control System điều khiển các cổng xoay vật lý.

- Factory Site 2 \- Nhà máy Long An: Quy mô 600 nhân viên, tải đỉnh giờ đổi ca đạt 600-800 (do nhân viên có thể di chuyển giữa các nhà máy theo giả định A-06) giao dịch trong 30 phút và có thể dồn tại các phút đầu ca tương tự nhà máy Bình Dương. Có sẵn hệ thống Turnstile Control System điều khiển các cổng xoay vật lý.

14.1.2 Các đặc tính kiến trúc (Architecture Characteristics) định kiến trúc:

- AC-01 (Performance): Thời gian phản hồi tại kiosk ≤ 2 giây (mục tiêu thiết kế: p95 ≤ 1,0s và p99 ≤ 2,0s).

- AC-02 (Availability tại cổng ≥ 99,9%): Đứt cáp Internet/WAN giữa nhà máy và HQ hoặc hệ thống HR/Payroll bảo trì không được làm gián đoạn việc quét QR mở cổng tại nhà máy.

- AC-03 (Data Integrity & Durability): RPO = 0 cho mọi sự kiện chấm công đã được xác nhận mở cổng, không mất và không trùng bản ghi.

- AC-07 (Interoperability): Gọi API Turnstile trong thời gian cho phép là 2 giây và đồng bộ dữ liệu sang HR/Payroll hoàn tất trước 12:00 trưa ngày 1 hằng tháng.

### 14.2

Bước 2 \- Quyết định mô hình phân bổ địa lý \- Kiến trúc lai giữa trung tâm và biên

Từ Bước 1, ta đánh giá 3 phương án phân bổ vật lý:

- Phương án A (100% Cloud và Centralized): Đặt toàn bộ các microservice tại Central Site. Kiosk tại Bình Dương và Long An gọi qua WAN về Central Site, sau đó Central Site gọi ngược qua VPN về Turnstile Control System tại nhà máy để mở cổng. → Loại, vì một lượt quét phải đi qua 2 vòng WAN (Kiosk → Central Site → Turnstile tại nhà máy), độ trễ cao và chỉ cần mạng WAN của nhà máy chập chờn vào giờ giao ca là toàn bộ 800 công nhân bị ùn tắc trước cổng (vi phạm AC-01 và AC-02).

- Phương án B (100% Decentralized và Isolated Factory): Mỗi nhà máy vận hành độc lập hoàn toàn, không có hệ thống trung tâm. → Loại, vì nhân viên TMC có thể di chuyển giữa hai nhà máy (giả định A-06, A-07), HR Staff và Executive Management cần quản lý và xem báo cáo tập trung trên cả 2 nhà máy, đồng thời hệ thống HR/Payroll nằm tại Central Site.

- Phương án C (Hybrid Central \- Edge Topology): – Đặt tại Factory Edge (Bình Dương và Long An): Toàn bộ các thành phần nằm trên Critical Path (≤ 2 giây) gồm Edge Gateway, Attendance Service (Edge Instance), QR Verifier, Gate Station

<img src="images/page-058-image-01.png" alt="Page 58 image" width="30" />

Service, Turnstile Adapter, Redis, PostgreSQL (Edge) và Outbox Publisher. Mọi giao dịch quét QR và mở cổng xoay diễn ra 100% trong mạng LAN nội bộ nhà máy. – Đặt tại Central Site (HQ): Các thành phần quản trị tập trung và hậu xử lý bất đồng bộ (Non-Critical Path) gồm API Gateway, Identity Service, User/QR Service, Reporting Service (Read Model), Audit/Admin Service, HR Integration Service và cụm Message Broker. → Chọn phương án này.

### 14.3 Bước 3 \- Ánh xạ các thành phần Logic sang nút vật lý và đơn vị triển khai

Thực hiện ánh xạ từng Microservice (ADR-01), cơ sở dữ liệu (ADR-10) và thành phần tích hợp (ADR-04, ADR-06, ADR-07) lên các nút phần cứng/ảo hóa:

- Tại mỗi cổng nhà máy: Triển khai QR Scanner gắn với Kiosk (Industrial PC / Tablet) chạy ứng dụng Kiosk App (có cơ chế verify chữ ký offline và SQLite mã hóa cục bộ).

- Tại phòng trực bảo vệ: Triển khai máy tính PC chạy giao diện web /security/* phục vụ giám sát cổng và mở cổng thủ công (UC-05, UC-06).

- Tại phòng máy chủ nhà máy (Factory Server Room \- Edge Cluster): Triển khai 2 máy chủ vật lý hoặc ảo hóa Edge Node A (Active) và Edge Node B (Warm Standby) chạy các container của khối xử lý biên cùng PostgreSQL và Redis.

- Tại trung tâm dữ liệu (Central Site): Triển khai trên hạ tầng Kubernetes Cluster chia theo các vùng mạng bảo mật, kết nối tới cụm Message Broker và PostgreSQL riêng cho từng service.

### 14.4 Bước 4 \- Thiết kế phân vùng mạng và giao thức mạng (Protocol)

Áp dụng nguyên lý DP-05 (Least Privilege & Defense in Depth) để cô lập lưu lượng mạng:

- Tại Central Site: Tách thành 3 phân vùng gồm DMZ (Reverse Proxy + WAF đón lưu lượng HTTPS từ Internet), Application Zone (chứa Kubernetes workloads và Message Broker), Data & Security Zone (chứa PostgreSQL, Vault/KMS, Backup \- không kết nối trực tiếp ra Internet), và kết nối nội bộ sang HQ Network (nơi đặt HR/Payroll System).

- Tại mỗi Factory Site: Chia mạng LAN nhà máy thành 4 phân vùng VLAN: – Gate Zone \- VLAN Kiosk: Chứa các Kiosk và QR Scanner tại các cổng. – Security Booth VLAN: Chứa máy tính của bảo vệ. – Turnstile VLAN (OT): Mạng công nghiệp (Operational Technology) chứa Turnstile Control System và các Turnstile Controller hiện hữu, chỉ cho phép Turnstile Adapter từ Server Room gọi vào.

– Server Room \- VLAN Server: Chứa Edge Node A, Edge Node B, Core Switch và Firewall.

- Kết nối Liên trạm (WAN Site-to-Site): Sử dụng đường hầm IPsec VPN chính (cáp quang ISP 1) kết hợp đường dự phòng (ISP thứ 2 / 4G LTE Failover), truyền tải sự kiện qua giao thức AMQPS (cổng 5671) và đồng bộ cấu hình/danh sách thu hồi QR qua HTTPS (cổng 443).

14.5 Bước 5 \- Thiết kế cơ chế sẵn sàng cao (High Availability), sao chép dữ liệu (Replication) và chịu lỗi offline đa tầng (Multi-Tier Offline Resilience) cho hệ thống

Để thỏa mãn đồng thời AC-02 (Availability ≥ 99,9%), AC-03 (RPO = 0) và AC-10 (RTO trung tâm ≤ 4 giờ, RTO node biên ≤ 30 phút), kiến trúc vật lý được thiết kế với 3 lớp phòng vệ sự cố:

- Lớp 1 \- Trường hợp đứt kết nối WAN giữa nhà máy và Central Site: – Luồng quét QR tại cổng (Kiosk ↔ Edge Node A ↔ Turnstile Control System) chạy hoàn toàn trên mạng LAN nên không bị ảnh hưởng.

<img src="images/page-059-image-01.png" alt="Page 59 image" width="30" />

– Các sự kiện chấm công được ghi bền vững vào bảng attendance_transactions và outbox_events trên PostgreSQL primary tại node Edge (ADR-04). Khi WAN khôi phục, Outbox Publisher tự động đẩy tuần tự lên Message Broker tại Central Site.

- Lớp 2 \- Trường hợp sự cố phần cứng tại Edge Node A của nhà máy: – PostgreSQL primary trên Edge Node A thực hiện sao chép luồng đồng bộ sang PostgreSQL sync replica trên Edge Node B (Warm Standby) qua mạng LAN tốc độ cao → bảo đảm RPO = 0 (mọi giao dịch đã xác nhận tại Kiosk đều đã nằm trên cả 2 node).

– Khi Edge Node A hỏng, Edge Node B kích hoạt bộ container Edge Stack và chuyển quyền nội bộ trong vòng ≤ 30 phút (AC-10).

- Lớp 3 \- Trường hợp đứt mạng LAN nội bộ giữa Kiosk và Phòng Server nhà máy: – Kiosk App tự động chuyển sang chế độ offline: dùng khóa công khai (JWKS) và danh sách thu hồi (revocation list) đã được cache cục bộ để xác thực chữ ký số và hạn của mã QR. – Nếu hợp lệ, Kiosk lưu giao dịch vào offline Queue (SQLite mã hóa AES-256) trên ổ cứng Kiosk và kích hoạt mở cổng qua đường dự phòng local gate link (Relay / RS-485 / TCP) hoặc thông qua Bảo vệ (UC-06). Khi LAN phục hồi, Kiosk tự động xả hàng đợi về Edge Node kèm event_id để chống trùng (ADR-05).

### 14.6 Bước 6 \- Định cỡ phần cứng và hoạch định dung lượng

Tính toán cấu hình máy chủ, dung lượng lưu trữ và băng thông mạng dựa trên khối lượng giao dịch thực tế của Bình Dương (∼800 NV) và Long An (∼600 NV) sẽ trình bày chi tiết tại Mục 4.2.4.

### 14.7 Bước 7: Kiểm chứng kiến trúc vật lý với các fitness Functions

Rà soát chéo toàn bộ sơ đồ triển khai với bảng AC-01..AC-12 và ADR-01..ADR-10 để bảo đảm không có điểm nghẽn cổ chai (Single Point of Failure) trên đường đi của lượt quét QR và đáp ứng mọi yêu cầu bảo mật/tuân thủ.

<img src="images/page-060-image-01.png" alt="Page 60 image" width="30" />

<table>
<tbody>
<tr><td>15</td><td>Thiết kế chi tiết sơ đồ triển khai (Deployment View)</td></tr>
<tr><td>15.1</td><td>Sơ đồ triển khai Deployment View tổng quan</td></tr>
</tbody>
</table>

<img src="images/page-060-image-02.png" alt="Page 60 image" width="639" />

*Hình 15.1.1 Sơ đồ triển khai Deployment View tổng quan*

### 15.1.1 Khối người dùng truy cập từ Internet:

- Employee: Sử dụng smartphone cá nhân truy cập Mobile App qua giao thức HTTPS (TLS 1.2+) để nhận và hiển thị mã QR động và tra cứu lịch sử chấm công cá nhân.

- HR Staff, Production Supervisor, System Administrator, Executive Management: Truy cập Web Portal quản trị báo cáo qua HTTPS (yêu cầu MFA bắt buộc đối với System Administrator và HR Staff theo NFR-03, AC-04).

### 15.1.2 Khối Central site:

- Vùng DMZ: Đặt Reverse Proxy + WAF (Web Application Firewall) làm điểm tiếp nhận duy nhất từ Internet, thực hiện chấm dứt TLS, chống tấn công DDoS/SQLi/XSS và lọc lưu lượng trước khi chuyển vào vùng trong.

- Vùng Application Zone (Kubernetes Cluster):

1\. Vận hành các dịch vụ trung tâm gồm:

- API Gateway

- Identity Service (OAuth2/OIDC, cấp phát JWT và khóa ký QR)

- User/QR Service (quản lý hồ sơ nhân viên và vòng đời mã QR)

- Reporting Service (cập nhật và truy vấn Read Model báo cáo)

- Audit/Admin Service (ghi nhận nhật ký kiểm toán append-only và quản lý cấu hình trạm)

- HR Integration Service (điều phối đồng bộ và đối soát dữ liệu với HR/Payroll)

<img src="images/page-061-image-01.png" alt="Page 61 image" width="30" />

<!-- pdf-list-break -->

2. Cụm Message Broker (Cluster) tiếp nhận các sự kiện chấm công từ 2 nhà máy đẩy về qua đường hầm VPN và phân phối bất đồng bộ cho Reporting Service, Audit/Admin Service và HR Integration Service.

<!-- pdf-list-break -->

- Vùng Data & Security Zone: – PostgreSQL (DB riêng từng service): Tuân thủ nguyên tắc Database per Service (ADR-10) \- mỗi microservice sở hữu schema và instance độc lập, tuyệt đối không truy cập chéo DB. Dữ liệu cá nhân (PII) được mã hóa ở mức lưu trữ bằng các thuật toán mã hoá như AES-256. – Vault/KMS, Backup: Quản lý tập trung khóa mã hóa, cặp khóa bất đối xứng ký mã QR (ECDSA P-256 / Ed25519), chứng chỉ mTLS cho các Kiosk/Edge Node, và hệ thống sao lưu tự động hằng ngày kèm lưu trữ WAL .

### 15.1.3 Khối HR/Payroll:

Chứa HR/Payroll System hiện hữu của công ty TMC. Chỉ có duy nhất HR Integration Service từ Application Zone được phép giao tiếp với HR/Payroll System qua giao thức HTTPS/REST theo hợp đồng API đã có.

### 15.1.4 Khối 2 nhà máy \- Factory Bình Dương và Long An:

- Mỗi nhà máy là một đơn vị vận hành tự chủ gồm:

  - Cụm thiết bị tại cổng: Gate Stations (Kiosk + QR Scanner + Turnstile Controller) và máy tính bảo vệ Guard Console.

  - Cụm máy chủ biên: Edge Cluster gồm Node A (active) + Node B (standby) giao tiếp với Kiosk và Guard Console qua giao thức HTTPS của mạng LAN.

  - Hệ thống cổng xoay hiện hữu: Turnstile Control System (đã có sẵn), nhận lệnh mở cổng từ Edge Cluster qua HTTPS/REST (Turnstile API) trong mạng LAN nhà máy.

- Liên kết WAN site-to-site: Kết nối mỗi Edge Cluster về Central Site qua đường hầm IPsec VPN (chạy trên đường truyền Internet cáp quang chính + đường dự phòng ISP thứ hai / 4G LTE), sử dụng giao thức AMQPS để đẩy sự kiện từ Outbox về Message Broker và HTTPS để kéo cấu hình/danh sách thu hồi QR.

<img src="images/page-062-image-01.png" alt="Page 62 image" width="30" />

### 15.2 Sơ đồ triển khai Deployment View chi tiết tại một nhà máy

<img src="images/page-062-image-02.png" alt="Page 62 image" width="639" />

*Hình 15.2.1 Sơ đồ triển khai chi tiết tại một nhà máy*

Phân tích chi tiết các phân vùng và thành phần bên trong mỗi nhà máy: Phân tích chi tiết các phân vùng và thành phần bên trong mỗi nhà máy:

1\. Phân vùng cổng ra vào \- gate zone \- VLAN Kiosk (PoE) × N cổng:

<!-- pdf-list-break -->

- Số lượng trạm cổng:

  - Tại Nhà máy Bình Dương (∼800 NV): Triển khai N=6 trạm cổng, gồm 4 cổng một chiều (đổi chiều linh hoạt theo ca vào/ra) và 2 cổng hai chiều. Với 800 lượt/30 phút, mỗi cổng chỉ xử lý trung bình ∼ 4,5 lượt/phút (ngay cả khi dồn tải gấp 5 lần vào 5 phút cao điểm thì mỗi cổng chỉ xử lý ∼1 lượt mỗi 2,5 giây hoàn toàn dư địa so với ngưỡng ≤ 2 giây).

  - Tại Nhà máy Long An (∼600 NV): Triển khai N=4 trạm cổng.

- Bên trong mỗi Gate Station 01..N:

  - QR Scanner (USB HID / serial): Đầu đọc mã vạch QR công nghiệp tốc độ quét cao (≤50 ms), truyền chuỗi token QR trực tiếp vào Kiosk qua cáp USB HID hoặc RS-232/Serial.

  - Kiosk (tablet / industrial PC) cấp nguồn và mạng qua cáp PoE (Power over Ethernet) nối vào switch có lưu điện UPS. Trên Kiosk cài đặt ứng dụng Kiosk App với 3 mô-đun cốt lõi:

    1. Offline Token Verifier: Kiểm tra sơ bộ cấu trúc, chữ ký số và thời hạn của token QR bằng khóa công khai đã cache.

    2. Local Cache: Lưu trữ khóa công khai (JWKS), cấu hình trạm cổng và danh sách mã QR/nhân viên bị thu hồi (revocation list).

    3. Offline Queue (SQLite, mã hóa AES-256): Lưu bền vững các sự kiện quét QR kèm event_id (UUID v7 theo thời gian) khi mất kết nối tới Server Room.

  - Turnstile Controller (thiết bị có sẵn): Bộ vi điều khiển cơ-điện tử gắn trực tiếp trong thân cổng xoay. Trong điều kiện bình thường, thiết bị nhận lệnh mở khóa từ Turnstile Control System hiện hữu, trong trường hợp khẩn cấp/offline toàn mạng LAN, có thể nhận tín hiệu qua đường Local Gate Link (relay / RS-485 / TCP) hoặc mở bởi bảo vệ.

<img src="images/page-063-image-01.png" alt="Page 63 image" width="30" />

*2\. Phân vùng của trạm bảo vệ \- Security Booth:*

- Đặt máy tính Guard Console (PC) chạy ứng dụng Web UI /security/*

- Kết nối tới Edge Gateway qua HTTPS với quyền hạn bị giới hạn nghiêm ngặt theo vai trò RBAC: Security Guard và phạm vi cổng được phân công.

- Nhận cảnh báo thời gian thực (qua WebSocket/SSE từ Edge Gateway) khi có vi phạm Anti-passback, quét QR đã dùng nhiều lần hoặc mất tín hiệu Kiosk.

3\. Phân vùng mạng cổng xoay \- Turnstile VLAN (OT):

<!-- pdf-list-break -->

- Chứa máy chủ/bộ điều khiển trung tâm Turnstile Control System (có sẵn \- API REST) của nhà cung cấp thiết bị cổng xoay.

- Được tách riêng vào một VLAN OT (Operational Technology) để bảo mật: các thiết bị văn phòng hoặc Kiosk bình thường không thể gọi trực tiếp vào API này, chỉ có IP của Edge Node A/B (thông qua Turnstile Adapter) được Firewall cho phép gọi HTTPS/REST (LAN) POST /gates/{id}/open.

<!-- pdf-list-break -->

4. Phân vùng phòng máy chủ nhà máy \- server room \- VLAN Server Edge Node A \- ACTIVE (4 vCPU / 16 GB RAM / SSD RAID1):

   - Edge Gateway: Điểm vào duy nhất tại biên, xác thực chứng chỉ mTLS của từng Kiosk (POST /kiosk/v1/scans), kiểm tra JWT của Guard Console, và giới hạn tần suất gọi chống tấn công làm nghẽn cổng.

   - Attendance Service: Điều phối luồng nghiệp vụ chính, nhận yêu cầu quét từ Edge Gateway, gọi QR Verifier, kiểm tra trạng thái trên Redis, kiểm tra cấu hình cổng qua Gate Station Service, gọi Turnstile Adapter mở cổng, và ghi giao dịch vào PostgreSQL primary.

   - QR Verifier (JWKS + trạng thái QR): Xác thực chữ ký số bất đối xứng của mã QR động/tĩnh, kiểm tra thời gian hiệu lực (exp, iat) và đối chiếu danh sách thu hồi (revocation list).

   - Redis: Lưu trữ in-memory tốc độ cao (≤2 ms) cho 2 tác vụ:

     1. Khóa nguyên tử (SET NX EX) trạng thái mã QR một lần: chuyển từ chưa dùng sang RESERVED trong khi chờ gọi Turnstile API, rồi sang USED sau khi mở cổng thành công (nếu Turnstile API lỗi/timeout thì giải phóng khóa để nhân viên quét lại)

     2. Lưu trạng thái vào/ra gần nhất (last_direction, last_scan_time, last_gate_id) của từng nhân viên tại nhà máy để kiểm tra quy tắc Anti-passback và chống quét lặp liên tiếp tại cùng Kiosk.

   - Gate Station Service: Quản lý danh mục cổng, hướng cổng (vào / ra / hai chiều), trạng thái bảo trì và theo dõi tín hiệu Heartbeat từ các Kiosk.

```text
- Turnstile Adapter: Cài đặt lớp Anti-Corruption Layer, chuyển đổi lệnh miền nội bộ sang lời gọi
  HTTPS/REST (LAN) POST /gates/{id}/open của Turnstile Control System.
```

- PostgreSQL primary (+ outbox, audit local): Lưu trữ bền vững trên ổ cứng SSD bảng attendance_transactions (system of record tại biên), bảng outbox_events và bảng local_audit_logs.

- Outbox Publisher: Tiến trình chạy nền đọc các bản ghi PENDING trong bảng outbox_events từ PostgreSQL primary và đẩy qua WAN Edge tới Message Broker tại Central Site bằng giao thức AMQPS 5671 (publish attendance.* events).

- Edge Agent (đồng bộ cấu hình, revocation): Tiến trình đồng bộ hai chiều với Central Site qua HTTPS 443: kéo danh sách nhân viên đủ quyền, khóa công khai JWKS, danh sách thu hồi QR (revocation list) và cấu hình trạm cổng mới nhất từ User/QR Service & Identity Service về lưu tại Edge Node.

Edge Node B \- STANDBY (warm, cùng cấu hình: 4 vCPU / 16 GB RAM / SSD RAID1):

- Chạy PostgreSQL sync replica nhận dữ liệu theo cơ chế streaming replication (Synchronous) từ PostgreSQL primary của Node A → cam kết 0 mất dữ liệu (RPO = 0) ngay cả khi ổ cứng hoặc bo mạch của Node A hỏng đột ngột.

<img src="images/page-064-image-01.png" alt="Page 64 image" width="30" />

- Toàn bộ Edge stack được cài đặt sẵn dưới dạng container image (tắt, sẵn sàng khởi động ≤ 30 phút), tự động hoặc bán tự động tiếp quản địa chỉ Virtual IP (VIP) của phòng máy khi Node A gặp sự cố.

### 5. Phân vùng biên mạng diện rộng \- WAN Edge:

Thiết bị Router/Firewall chuyên dụng cấu hình Dual-WAN: đường truyền chính dùng IPsec VPN Site-to-Site trên cáp quang doanh nghiệp và đường truyền phụ dùng 4G/5G LTE Enterprise tự động chuyển mạch (Auto-Failover) trong vòng \<5 giây khi đứt cáp quang.

<img src="images/page-065-image-01.png" alt="Page 65 image" width="30" />

<table>
<tbody>
<tr><td>16</td><td>Thiết kế chi tiết tích hợp API</td></tr>
<tr><td>16.1</td><td>Kiến trúc tích hợp tổng thể</td></tr>
</tbody>
</table>

Tuân thủ ADR-06 (Turnstile Adapter / Anti-Corruption Layer), ADR-07 (HR Integration Service) và các nguyên tắc SOLID (SRP, OCP, LSP, ISP, DIP), toàn bộ giao tiếp giữa lõi nghiệp vụ của TMC và các hệ thống bên ngoài được thiết kế theo mẫu Hexagonal Architecture:

<img src="images/page-065-image-02.png" alt="Page 65 image" width="639" />

*Hình 16.1.1 Kiến trúc tích hợp tổng thể theo mô hình Hexagonal*

- Đảo ngược phụ thuộc (DIP): AttendanceApplicationService và HrReconciliationUseCase chỉ gọi qua interface ITurnstileControlPort và IHrPayrollIntegrationPort thuộc sở hữu của tầng Application, hoàn toàn không biết về mã trạng thái HTTP, cấu trúc JSON riêng của nhà cung cấp cổng xoay hay hệ thống HR/Payroll.

- Đóng để sửa đổi, Mở để mở rộng (OCP) & Thay thế Liskov (LSP): Khi nhà máy Bình Dương hoặc Long An nâng cấp phần cứng cổng xoay sang hãng mới, hoặc khi chạy kiểm thử tải (Load Testing 800 lượt/30 phút) bằng SimulatedTurnstileAdapter, chỉ cần nạp lớp Adapter tương ứng qua cấu hình (DP-10 Configuration over Code) mà không sửa bất kỳ dòng mã nghiệp vụ nào.

### 16.2 Thiết kế API nội bộ tại biên giữa Kiosk App và Edge Gateway

### Bảng 16.2.1: Đặc tả các API nội bộ giữa Kiosk App và Edge Gateway

<img src="images/page-066-image-01.png" alt="Page 66 image" width="30" />

| STT | Method và Endpoint | Người gọi (Caller) | Mô tả chức năng | Ngân sách thời gian (SLA) |
| --- | --- | --- | --- | --- |
| 1 | POST /kiosk/v1/scans | Kiosk App (mTLS) | Gửi sự kiện quét QR thời gian thực để xác thực, mở cổng xoay và ghi nhận chấm công. | ≤1.500 ms (End- to-end tại Kiosk ≤ 2.000 ms) |
| 2 | POST /kiosk/v1/scans/offline-batch | Kiosk App (mTLS) | Đẩy lô sự kiện chấm công từ hàng đợi SQLite cục bộ lên Edge Node sau khi mạng LAN khôi phục. | ≤5.000 ms / lô 100 bản ghi |
| 3 | GET /kiosk/v1/sync-bundle | Kiosk App (mTLS) | Tải khóa công khai JWKS, cấu hình cổng và danh sách mã QR bị thu hồi (revocation_list delta) để phục vụ xác thực offline. | Chu kỳ mỗi 30 giây (hoặc nhận push) |
| 4 | POST /kiosk/v1/heartbeats | Kiosk App (mTLS) | Gửi tín hiệu sức khỏe định kỳ (trạng thái đầu đọc QR, dung lượng hàng đợi offline, phiên bản phần mềm). | Mỗi 15 giây (Cảnh báo nếu mất tín hiệu) |
| 5 | POST /security/v1/gates/ {gate_id}/manual-open | Guard Console (JWT RBAC) | Gửi tín hiệu sức khỏe định kỳ (trạng thái đầu đọc QR, dung lượng hàng đợi offline, phiên bản phần mềm). | ≤ 2.000 ms |

### Đặc tả chi tiết POST /kiosk/v1/scans:

1. a) Request Headers:

   - Content-Type: application/json

   - Idempotency-Key: 01928a7b-4c1e-7f00-8a11-39b0d82e1001 (UUID v7 sinh duy nhất cho mỗi lượt quét tại Kiosk)

   - X-Kiosk-Id: BD-KIOSK-01 (khớp với Subject CN trong chứng chỉ mTLS của Kiosk)

```text
- X-Correlation-Id: corr-bd-20261017-064512-001
```

- b) Request Body (application/json):

<img src="images/page-066-image-02.png" alt="Page 66 image" width="620" />

Để bảo vệ dữ liệu cá nhân theo Privacy by Design (ADR-09, AC-06, NFR-05): Chuỗi qr_token là một token ký số ngắn hạn (JWS sử dụng thuật toán ECDSA P-256 ES256, hiệu lực 30 giây), chỉ chứa jti (mã định danh token một lần), sub (UUID nội bộ của nhân viên, tuyệt đối không chứa họ tên, số CCCD, số điện thoại hay PII đọc được), và fac (danh sách mã nhà máy được phép vào).

<img src="images/page-067-image-01.png" alt="Page 67 image" width="30" />

- c) Response Body (200 OK \- Hợp lệ và cổng đã mở):

<img src="images/page-067-image-02.png" alt="Page 67 image" width="620" />

- d) Response Body (422 Unprocessable Entity / 403 Forbidden – Từ chối mở cổng):

<img src="images/page-067-image-03.png" alt="Page 67 image" width="620" />

*Các mã reason_code chuẩn hóa gồm:*

- QR_EXPIRED: Mã QR đã hết hạn

- QR_ALREADY_USED: Mã QR đã được sử dụng

- QR_REVOKED: Mã QR đã bị thu hồi

- INVALID_SIGNATURE: Chữ ký số không hợp lệ

- EMPLOYEE_INACTIVE: Nhân viên không còn hoạt động

- ANTI_PASSBACK_VIOLATION: Vi phạm qui tắc anti-passback (chống quay vòng mã thẻ)

- GATE_IN_MAINTENANCE: Cổng đang bảo trì

- TURNSTILE_TIMEOUT_RETRY_SCAN: Cửa kiểm soát đã hết thời gian chờ

<table>
<tbody>
<tr><td>16.3</td><td>Thiết kế API tích hợp Turnstile Control System</td></tr>
<tr><td>16.3.1</td><td>Ngữ cảnh tích hợp và ràng buộc kỹ thuật</td></tr>
</tbody>
</table>

- Hệ thống đích: Turnstile Control System hiện hữu tại từng nhà máy (Bình Dương và Long An), đặt trong phân vùng Turnstile VLAN (OT).

- Thành phần gọi phía TMC: Duy nhất mô-đun Turnstile Adapter nằm trên Edge Node A / B tại nhà máy đó.

- Tính chất giao tiếp: Đồng bộ vì hệ thống chỉ được phép báo “Mời qua cổng” và ghi nhận thành công lượt chấm công sau khi Turnstile Control System xác nhận đã mở khóa cổng xoay.

- Ngân sách độ trễ (Latency Budget): Để tổng thời gian phản hồi tại Kiosk ≤2.000 ms, ngân sách thời gian cấp phát tối đa cho lời gọi Turnstile API là ≤800 ms (connection timeout = 200 ms, Read/Response Timeout = 800 ms).)

<img src="images/page-068-image-01.png" alt="Page 68 image" width="38" />

*16.3.2 Danh mục endpoint của Turnstile Control System API*

### Bảng 16.3.1: Danh mục API Turnstile Control System

| STT | Method và Endpoint (Trên Turnstile Control System) | Phương thức trong ITurnstileControlPort | Mô tả nghiệp vụ & Use Case liên quan |
| --- | --- | --- | --- |
| 1 | POST /gates/{gate_id}/open | openGate(OpenGateRequest) | Mở khóa cổng xoay {gate_id} cho đúng 1 lượt người đi qua theo chiều chỉ định (ENTRY hoặc EXIT). Áp dụng cho UC-08 (Quét QR) và UC-06 (Bảo vệ mở cổng thủ công). |
| 2 | POST /gates/{gate_id}/close | closeGate(CloseGateRequest) | Đóng/khóa cưỡng bức cánh cổng xoay hoặc hủy lệnh mở khi phát hiện cảnh báo an ninh hoặc chuyển sang chế độ bảo trì. |
| 3 | GET /gates/{gate_id}/status | checkGateHealth(gateId) | Kiểm tra kết nối, trạng thái cơ khí (ONLINE, JAMMED, MAINTENANCE, OFFLINE) và bộ đếm cảm biến quang học. Áp dụng khi đăng ký trạm cổng (UC-04 Bước 5) và giám sát thiết bị (UC-05). |
| 4 | POST /webhooks/gate-events (Callback về Edge Gateway) | onPhysicalPassCompleted (GatePassEvent) | (Tùy chọn nếu phần cứng hỗ trợ) Turnstile Control System gửi sự kiện báo cánh cổng đã xoay xong 1 vòng hoặc hết thời gian chờ mà nhân viên không đi qua (PASS_COMPLETED / PASS_TIMEOUT). |

### 16.3.3 Đặc tả API contract cho POST /gates/{gate_id}/open

1\. Url mẫu cho nhà máy ở Bình Dương : https://turnstile-bd.tmc.internal/gates/BD-GATE-01/open

2\. Request Header :

- Authorization: Bearer \<TURNSTILE_EDGE_API_TOKEN\> (kết hợp chứng chỉ mTLS nội bộ)

- Idempotency-key: cmd-ts-01928a7b-4c1e-7f00-8a11-39b0d82e1001 (bắt buộc để đảm bảo nếu gói tin mạng retry thì cổng xoay không mở 2 lần)

- X-Correlation-Id: corr-bd-20261017-064512-001

- Content-Type: application/json

3\. Request Payload (application/json)

<img src="images/page-068-image-02.png" alt="Page 68 image" width="783" />

<img src="images/page-069-image-01.png" alt="Page 69 image" width="38" />

(Khi Bảo vệ mở cổng thủ công theo UC-06, trường trigger_source mang giá trị "MANUAL_GUARD_OVERRIDE" và operator_id chứa UUID của Security Guard)

4\. Response Payload \- Thành công (200 OK)

<img src="images/page-069-image-02.png" alt="Page 69 image" width="783" />

5\. Response Payload \- lỗi thiết bị / kẹt cơ khí (409 Conflict hoặc 503 Service Unavailable):

<img src="images/page-069-image-03.png" alt="Page 69 image" width="783" />

*Bảng 16.3.2: Bảng ánh xạ lỗi và chiến lược chịu lỗi Turnstile*

| Tình huống / HTTP Status từ Turnstile API | Mã nội bộ tại Turnstile Adapter | Xử lý đối với trạng thái QR Token (Redis) | Trạng thái bản ghi Attendance | Hành động tại Kiosk & Cảnh báo vận hành |
| --- | --- | --- | --- | --- |
| 200 OK (result: UNLOCKED) | GATE_OPENED | Chuyển từ RESERVED → USED (vô hiệu hóa vĩnh viễn token một lần). | CONFIRMED (Ghi vào DB + Outbox trong 1 transaction). | Kiosk hiển thị đèn xanh + âm báo “Mời qua cổng” trong ≤ 2 giây. |
| 409 Conflict / 503 (MECHANICAL_ARM_ JAMMED, CONTROLLER_ OFFLINE) | GATE_hardware_ error | Giải phóng khóa RESERVED → ACTIVE (nhân viên chưa mất lượt QR). | Ghi sự kiện kỹ thuật GATE_FAULT (không tính là đã vào/ra). | Kiosk báo “Cổng đang bảo trì/lỗi, vui lòng chuyển sang làn bên cạnh hoặc gặp bảo vệ”, đẩy cảnh báo tức thì lên Guard Console. |

<img src="images/page-070-image-01.png" alt="Page 70 image" width="38" />

| Tình huống / HTTP Status từ Turnstile API | Mã nội bộ tại Turnstile Adapter | Xử lý đối với trạng thái QR Token (Redis) | Trạng thái bản ghi Attendance | Hành động tại Kiosk & Cảnh báo vận hành |
| --- | --- | --- | --- | --- |
| Lỗi kết nối TCP sớm (connection refused / reset \< 200 ms) | network_ connect_error | Giữ RESERVED trong khi thực hiện 1 lần Retry tức thì (kèm cùng Idempotency- Key). | Nếu retry thành công → CONFIRMED, nếu vẫn lỗi → xử lý như Timeout. | Tránh thất bại giả do rớt gói tin ARP/TCP thoáng qua trong LAN. |
| 504 Gateway Timeout / Quá thời gian đọc (\> 800 ms) | gate_actuation_ unknown | Giải phóng khóa RESERVED → ACTIVE để nhân viên có thể quét lại | Ghi bản ghi trạng thái UNKNOWN ("chưa xác định") để đối soát. | Kiosk không báo thành công mà hiển thị "Hệ thống cổng phản hồi chậm, vui lòng quét lại", đồng thời Edge Service gọi ngầm GET /gates/{id}/status để xác minh kết quả thực tế. |

<img src="images/page-071-image-01.png" alt="Page 71 image" width="30" />

6\. Luồng quét QR, điều khiển cổng xoay và ghi outbox tại nhà máy

<img src="images/page-071-image-02.png" alt="Page 71 image" width="620" />

*Hình 16.3.1 Sequence Diagram luồng quét QR tại biên (Edge)*

<table>
<tbody>
<tr><td>16.4</td><td>Thiết kế chi tiết API tích hợp HR/Payroll System</td></tr>
<tr><td>16.4.1</td><td>Ngữ cảnh tích hợp và ràng buộc SLA</td></tr>
</tbody>
</table>

- Hệ thống đích: HR/Payroll System hiện hữu đặt tại HQ Network (Central Site).

- Thành phần tích hợp phía TMC: Duy nhất HR Integration Service đặt tại Central Site (Application Zone).

- Ràng buộc nghiệp vụ cốt lõi:

  1. Dữ liệu chấm công của cả 2 nhà máy (Bình Dương và Long An) phải được truyền qua API sang HR/Payroll System và sẵn sàng để đối soát trước 12:00 trưa ngày 1 hằng tháng (mục tiêu SLA nội bộ của hệ thống TMC: hoàn tất đồng bộ tự động trước 08:00 sáng ngày 1 hằng tháng, dành khoảng đệm 4 tiếng từ 08:00 \- 12:00 cho HR Staff xử lý các bản ghi ngoại lệ nếu có).

  2. TMC chỉ xuất/đồng bộ dữ liệu chấm công đã xác nhận, tuyệt đối không thực hiện tính lương, thuế hay phụ cấp.

  3. Hệ thống HR/Payroll System bảo trì hoặc quá tải không được ảnh hưởng đến hoạt động quét QR tại 2 nhà máy.

<img src="images/page-072-image-01.png" alt="Page 72 image" width="38" />

*16.4.2 Danh mục các API tích hợp giữa TMC và HR/Payroll System*

### Bảng 16.4.1: Danh mục API tích hợp HR/Payroll

| Chiều dữ liệu | Method & Endpoint | Hệ thống cung cấp (Provider) | Hệ thống gọi (Consumer) | Mô tả chức năng & Use Case liên quan |
| --- | --- | --- | --- | --- |
| Chiều 1: Outbound (TMC → HR/Payroll ) | POST /api/v1/attendance- batches | HR/Payroll System | HR Integration Service (TMC) | Gửi một lô dữ liệu chấm công của một nhà máy trong kỳ đối soát sang HR/Payroll. |
| Chiều 1: Outbound (TMC → HR/ Payroll) | GET /api/v1/attendance- batches/{batch_id}/status | HR/Payroll System | HR Integration Service(TMC) | Truy vấn kết quả xử lý của một lô khi lời gọi POST trước đó bị timeout/rớt mạng. |
| Chiều 1: Outbound (TMC → HR/ Payroll) | POST /api/v1/attendance- reconciliations | HR/Payroll System | HR Integration Service(TMC) | Gửi biên bản chốt đối soát tháng xác nhận tổng số nhân viên và tổng số bản ghi khớp 100% trước hạn 12:00 ngày 1. |
| Chiều 2: Inbound (HR/Payroll → TMC) | POST /hr- integration/v1/web hooks/employee-events (hoặc TMC gọi GET /api/v1/employees) | HR Integration Service(TMC) / HR/Payroll | HR/Payroll System/ TMC | Đồng bộ danh sách nhân viên mới, cập nhật phòng ban/ca làm việc, hoặc thông báo nhân viên nghỉ việc để TMC tự động thu hồi QR. |

### 16.4.3 Đặc tả API Contract cho POST /api/v1/attendance-batches

1\. Url mẫu : https://hr-payroll.tmc.internal/api/v1/attendance-batches

2\. Request Header :

- Authorization: Bearer \<TURNSTILE_EDGE_API_TOKEN\> (kết hợp chứng chỉ mTLS nội bộ)

- Idempotency-key: cmd-ts-01928a7b-4c1e-7f00-8a11-39b0d82e1001 (bắt buộc để đảm bảo nếu gói tin mạng retry thì cổng xoay không mở 2 lần)

```text
- X-Correlation-Id: corr-bd-20261017-064512-001
```

- Content-Type: application/json

3\. Request Payload (application/json)

<img src="images/page-073-image-01.png" alt="Page 73 image" width="30" />

<img src="images/page-073-image-02.png" alt="Page 73 image" width="620" />

4\. Response Payload \- Tiếp nhận thành công toàn bộ (200 OK)

<img src="images/page-073-image-03.png" alt="Page 73 image" width="620" />

5. Response Payload \- Lỗi một phần bản ghi (207 Multi-Status hoặc 422 Unprocessable Entity với danh sách lỗi chi tiết)

<img src="images/page-074-image-01.png" alt="Page 74 image" width="30" />

<img src="images/page-074-image-02.png" alt="Page 74 image" width="618" />

<img src="images/page-075-image-01.png" alt="Page 75 image" width="30" />

# 17 Tài liệu tham khảo

\[1\] M. Richards and N. Ford, Fundamentals of Software Architecture: An Engineering Approach. O’Reilly Media, 2020.

\[2\] R. C. Martin, Clean Architecture: A Craftsman’s Guide to Software Structure and Design. Prentice Hall, 2017.

\[3\] L. Bass, P. Clements and R. Kazman, Software Architecture in Practice, 4th ed. Addison-Wesley,

2021.

\[4\] M. Nygard, “Documenting Architecture Decisions,” Cognitect Blog, 2011.

\[5\] A. Cockburn, “Hexagonal Architecture (Ports and Adapters),” 2005.

\[6\] C. Richardson, Microservices Patterns (Transactional Outbox, Idempotent Consumer). Manning, 2018.

\[7\] Chính phủ Việt Nam, Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân, 2023.

\[8\] Quốc hội Việt Nam, Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15, 2025.