# Quy ước viết mã

Mục tiêu: người trong team khác đọc mã của bạn mà không cần hỏi, và công cụ bắt được vi phạm kiến trúc thay cho người duyệt.

## Công cụ bắt buộc

| Việc | Lệnh | Khi nào |
| --- | --- | --- |
| Định dạng (google-java-format) | `make format` | Trước mỗi commit. CI chạy `spotlessCheck` và sẽ đỏ nếu thiếu |
| Test đơn vị và kiến trúc | `make test` | Trước mỗi PR |
| Kiểm tra kiến trúc | `make arch-test` | Khi thêm gói hoặc phụ thuộc |
| Portal | `cd apps/web-portal && npm run lint && npm test && npm run build` | Khi sửa portal |

JDK 21 (Temurin hoặc tương đương). Không dùng tính năng xem trước (preview).

## Cấu trúc gói trong mỗi service (bắt buộc)

```
vn.tmc.<service>/
  domain/        quy tắc và bất biến nghiệp vụ, không biết framework
  application/
    port/in/     use case mà bên ngoài gọi vào
    port/out/    interface mà use case cần (do application SỞ HỮU)
    usecase/     hiện thực use case, ranh giới transaction
  adapter/
    in/rest, in/messaging, in/scheduler
    out/persistence, out/http, out/redis, out/messaging, out/inmem
  bootstrap/     @Configuration: nơi DUY NHẤT nối port với adapter
```

Quy tắc phụ thuộc và cách kiểm tra: `docs/architecture/module-map.md`. `ArchitectureTest` đỏ nghĩa là bạn đặt mã sai chỗ, **không** phải test cần sửa.

## Mười điều phải nhớ

1. **`domain` và `application` không có chú thích Spring** (`@Service`, `@Component`, `@Autowired`, `@Transactional`...). Nối bằng `bootstrap`. Ranh giới transaction đi qua `ITransactionRunner`.
2. **Port do tầng dùng nó định nghĩa.** Interface `ITurnstileControlPort` nằm ở `gate-station/application/port/out`, không phải ở gói của adapter.
3. **Không gọi `Instant.now()`, `UUID.randomUUID()` trong nghiệp vụ.** Dùng `IClock` và `Ids.uuidV7()` để test điều khiển được thời gian và định danh sắp xếp theo thời gian.
4. **Kết quả nghiệp vụ lường trước được là giá trị** (enum, record, `Result`), **không phải ngoại lệ.** Ví dụ từ chối QR trả `ReasonCode`, không ném. Ngoại lệ dành cho điều bất ngờ (hỏng hạ tầng, vi phạm bất biến).
5. **Fail-secure (DP-04):** không chắc chắn thì từ chối, không bao giờ mở cổng. Ví dụ: không có khóa thiết bị đã đồng bộ thì từ chối, không cho qua.
6. **Idempotent theo mặc định (DP-02):** mọi lệnh ghi có khóa định danh ổn định và nhận lại cùng khóa thì trả kết quả cũ. Lệnh gửi ra ngoài mang `Idempotency-Key`.
7. **Không bao giờ gửi lại lệnh vật lý khi chưa biết kết quả.** `TIMEOUT` là "chưa rõ"; hỏi trạng thái, không gửi lại.
8. **Không đưa dữ liệu cá nhân vào log, sự kiện, token, thông báo lỗi.** Chỉ UUID nội bộ. Không log token thô hay `jti` ở mức INFO.
9. **Quyền kiểm tra ở service**, bằng `Rbac.require*` trong use case, kể cả khi gateway đã kiểm (ADR-08). Trả cùng một lỗi cho "không tồn tại" và "không phải của bạn".
10. **Mỗi service chỉ ghi dữ liệu của mình** (ADR-10). Không đọc bảng của service khác, không import lớp của service khác.

## Đặt tên

| Thứ | Quy tắc | Ví dụ |
| --- | --- | --- |
| Interface cổng | Tiền tố `I` | `ITurnstileControlPort`, `IAttemptRepository` |
| Use case | Động từ + đối tượng + `UseCase` hoặc `Service` | `RecordScanUseCase`, `AttendanceCommandService` |
| Adapter | Công nghệ + tên port | `HttpTurnstileAdapter`, `JdbcAttemptRepository`, `InMemoryOutboxRepository`, `RedisTokenReservation` |
| Test | `should_<kết quả>_when_<điều kiện>` | `should_deny_when_the_gate_is_in_maintenance` |
| Lớp test | `<Lớp>Test` hoặc theo chủ đề; tích hợp kết thúc bằng `IT` và có `@Tag("integration")` | `PersistenceAndBrokerIT` |
| Sự kiện | Quá khứ, PascalCase | `AttendanceConfirmed` |
| Hằng số lý do | `UPPER_SNAKE` trong `ReasonCode`, chỉ **thêm**, không đổi nghĩa | `QR_ALREADY_USED` |
| Gói | chữ thường, không gạch | `vn.tmc.gatestation` |
| Nhà máy, cổng | `BD`, `LA`; `BD-GATE-01`; cổng phần cứng `BD-HW-01` | |

## Kiểu mã

- DTO và value object là `record`. Aggregate có trạng thái là lớp `final` với hàm chuyển trạng thái, không có setter công khai.
- Hàm công khai của aggregate bảo vệ bất biến (ví dụ `AccessAttempt` ném `IllegalAttemptTransitionException`).
- Không dùng `null` làm giá trị nghiệp vụ nếu có thể dùng `Optional`/giá trị rỗng. Nơi `null` được phép (ví dụ `employeeUuid` khi token hỏng) phải ghi trong Javadoc của record.
- Không dùng trường tĩnh có thể thay đổi. Không `Thread.sleep` trong mã sản xuất ngoài adapter tường minh (kiosk-sim có `Sleeper` để thay trong test).
- Chú thích giải thích **vì sao** và tham chiếu ADR, FR, mục M1; không chú thích điều mã đã nói.
- Nhật ký: SLF4J, có correlation id, không PII. `warn` cho điều bất thường hệ thống tự xử lý được, `error` cho điều cần người xem.

## Lỗi thường gặp (đã gặp khi dựng scaffold)

| Lỗi | Hậu quả | Cách đúng |
| --- | --- | --- |
| Coi `TIMEOUT` của cổng là "chưa mở" và gửi lại | Mở cổng hai lần | Ghi `GATE_UNKNOWN`, đối soát bằng `queryCommand` |
| Đánh dấu token "đã dùng" ngay khi xác thực | Không nhả được khi cổng lỗi | Đánh dấu ở `ITokenReservation` sau khi biết kết quả |
| Để Spring bean cho lớp nghiệp vụ | `ArchitectureTest` đỏ, test cần khởi động Spring | Nối ở `bootstrap` |
| Test dùng `Thread.sleep` hoặc giờ hệ thống | Test chập chờn | Đồng hồ giả (`MutableClock`) |
| Thêm giá trị `ReasonCode` bằng cách đổi nghĩa giá trị cũ | Kiosk cũ hiểu sai | Chỉ thêm giá trị mới |
