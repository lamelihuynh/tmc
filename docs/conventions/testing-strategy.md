# Chiến lược kiểm thử

## Các tầng

| Tầng | Chạy bằng | Cần gì | Nhanh? | Kiểm điều gì | Ví dụ trong repo |
| --- | --- | --- | --- | --- | --- |
| Đơn vị (domain) | `make test` | không | rất nhanh | Bất biến, chuyển trạng thái, quy tắc | `AccessAttemptTest`, `AccessPolicyTest` |
| Use case trên adapter in-memory | `make test` | không | nhanh | Luồng nghiệp vụ và mọi nhánh lỗi, **không Spring** | `RecordScanUseCaseTest`, `Adr11ResilienceTest`, `Adr12TokenVerificationTest` |
| Kiến trúc | `make arch-test` | không | nhanh | Quy tắc phụ thuộc | `ArchitectureTest` (mỗi service một bản) |
| Lát cắt Spring (profile `inmem`) | `make test` | không | vài giây | Wiring, JSON, mã HTTP, RBAC ở biên | `KioskApiTest`, `InternalApiFlowTest` |
| Hợp đồng adapter ngoài | `make test` | không | nhanh | Ánh xạ lỗi, timeout, header | `HttpTurnstileAdapterTest` (máy chủ JDK thật) |
| Tích hợp hạ tầng | `make integration` | Docker | chậm | JDBC, Flyway, Redis, AMQP thật | `PersistenceAndBrokerIT` (thẻ `integration`) |
| Đầu-cuối cục bộ | `make local-up` rồi `make demo-*` | JDK, không Docker | vài chục giây | Nhiều service qua HTTP thật | `scripts/demo/*.sh` |
| Portal | `npm test` | Node | nhanh | Hàm thuần của portal | `api.test.ts` |

Tỷ lệ mong muốn: nhiều test use case nhanh, ít test tích hợp, đúng vài kịch bản đầu-cuối. Mọi nhánh lỗi của ADR-11 và ADR-12 phải có test **ở tầng use case** (nhanh, không phụ thuộc hạ tầng).

## Quy tắc viết test

1. **Tên:** `should_<kết quả>_when_<điều kiện>`. Đọc tên là biết yêu cầu.
2. **Cấu trúc:** sắp đặt, hành động, khẳng định, tách bằng dòng trống. Một hành vi một test.
3. **Dùng fixture, không dùng mock tràn lan.** `GateFixture`, `AttendanceFixture`, `PhoneSimulator` dựng cả ứng dụng trên adapter giả có hành vi thật. Chỉ mock thứ không có bản giả.
4. **Không `Thread.sleep`, không giờ hệ thống:** dùng `MutableClock`. Test nào cần chờ thật (ví dụ HTTP) phải có thời hạn rõ ràng.
5. **Fault injection thay vì giả định:** `SimulatedTurnstileAdapter.Fault`, `InMemoryEventBroker.setAvailable(false)`, `FakeAttendanceGateway.failRecordResult`, `FakeEdge.reachable`.
6. **Khẳng định cả điều KHÔNG xảy ra** khi điều đó là yêu cầu an toàn: `assertThat(turnstile.openCalls()).isEqualTo(1)` sau khi gửi lại; không có lệnh thứ hai.
7. **Test không phụ thuộc thứ tự chạy** và không dùng chung trạng thái giữa test.
8. Lỗi hồi quy: sửa lỗi thì thêm test **bị đỏ trước khi sửa**.

## Test bị bỏ qua (`@Disabled`)

Chỉ hợp lệ khi có đúng dạng: `@Disabled("TODO Team X (tham chiếu yêu cầu): tiêu chí kiểm tra")`. Đó là một **việc đã được giao**, không phải chỗ giấu test hỏng. Khi làm xong, bỏ `@Disabled` và viết thân test. CI không chặn test bỏ qua, nhưng M-3 yêu cầu không còn test bỏ qua mức Bắt buộc.

## Tích hợp (Testcontainers)

- Đặt `@Tag("integration")`; chúng bị loại khỏi `test` thường và chạy bằng `./gradlew integrationTest`.
- Dùng container mới cho mỗi lớp test, tạo schema bằng Flyway thật (không bằng SQL riêng của test).
- Khi Docker không bật, `make integration` sẽ thất bại ngay lúc khởi động container. Bật Docker rồi chạy lại, không sửa test.

## Điều chưa đo được (và không nên khẳng định)

| Chỉ tiêu | Tình trạng | Việc |
| --- | --- | --- |
| Độ phủ domain/application ≥ 80% (AC-09) | **Chưa đo.** Chưa cấu hình JaCoCo | Thêm vào D-02, báo số thật |
| p95 ≤ 1,0 s, p99 ≤ 2,0 s ở tải đỉnh (AC-01, NFR-06) | **Chưa đo** | A-09 |
| Hoạt động với Postgres, Redis, RabbitMQ thật | **Chưa chạy** | D-01 |

## Kiểm thử tải (A-09), gợi ý cách làm

1. Chạy `make local-up` hoặc stack Docker đầy đủ.
2. Dùng `turnstile-simulator` với độ trễ cố định nhỏ (ví dụ 100 ms) để gần thực tế.
3. 800 lượt trong 30 phút nhưng **dồn thành đợt**: ví dụ 800 lượt trong 5 phút đầu, 6 cổng.
4. Ghi p50, p95, p99 và số lượt vượt 2 giây; đính kèm lệnh chạy lại. Báo cáo chỉ viết số đã đo.
