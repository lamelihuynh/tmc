# Xử lý sự cố khi làm việc

Mỗi mục là một tình huống đã gặp khi dựng scaffold hoặc rất dễ gặp. Tìm triệu chứng, đọc nguyên nhân, làm theo cách xử lý.

## Build

| Triệu chứng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `Unsupported class file major version` hoặc Gradle không tìm thấy toolchain | Đang dùng JDK khác 21 | `export JAVA_HOME="$(/usr/libexec/java_home -v 21)"` rồi chạy lại |
| `spotlessCheck` đỏ | Mã chưa theo google-java-format | `make format`, rồi commit phần thay đổi |
| `ArchitectureTest` đỏ | Bạn import sai chiều (domain → application, application → adapter, hoặc giữa các service) | Sửa **mã**, không sửa test. Đọc `docs/architecture/module-map.md` |
| Lần đầu chạy Gradle rất lâu | Tải phụ thuộc và JDK | Chờ; những lần sau nhanh. Dùng `--offline` khi mất mạng nếu đã tải |
| Sửa Java bằng công cụ thay chuỗi mà không khớp | `spotless` đã đổi định dạng mã (ngắt dòng) | Chạy `make format` trước khi thay chuỗi, hoặc sửa tay |
| `package jakarta.validation... does not exist` | Module thiếu `spring-boot-starter-validation` | Thêm vào `build.gradle.kts` của module đó |

## Chạy stack `local`

| Triệu chứng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `make local-up` báo `already listening on <cổng>` | Tiến trình cũ còn chạy | `make local-down`; nếu vẫn còn: `lsof -iTCP:<cổng> -sTCP:LISTEN` rồi `kill <pid>` |
| Stack "không lên kịp" | Máy chậm, JVM khởi động lâu | Xem `.local/<service>.log`; thường chỉ cần chạy lại `make local-up` |
| `stack is not running` khi chạy demo | Chưa `make local-up` hoặc một service chết | `make local-status`, xem log |
| `simulators are not built` | Chưa dựng CLI | `make sims` |
| Chạy lại demo bị `ANTI_PASSBACK_VIOLATION` ở bước đầu | Trạng thái trong bộ nhớ từ lần trước | `make local-restart` |
| **Lượt quét đầu tiên trả `IN_PROGRESS` rồi thành `ALLOWED`** | JVM chưa nóng nên request đầu chậm quá 1,5 giây; kiosk gửi lại cùng `attempt_id`, Edge trả `IN_PROGRESS`, kiosk hỏi lại. **Đây là hành vi đúng:** cổng không mở hai lần | Không phải lỗi. Nếu bạn thấy cổng mở hai lần thì mới là lỗi, mở issue ngay |
| Demo `timeout` báo `pass_count` không đúng | Số đếm cộng dồn trên cùng một cổng phần cứng | Script đã so sánh theo mức tăng; nếu viết kịch bản riêng hãy làm tương tự |
| `QR_EXPIRED` ngay khi quét | Đồng hồ máy chênh quá 60 giây, hoặc chờ quá 30 giây giữa lúc sinh và quét | Sinh token ngay trước khi quét; kiểm tra đồng hồ |
| `DEVICE_UNKNOWN` | Chưa `register`, hoặc kiosk chưa `sync` sau khi đăng ký | `register` rồi `kiosk-sim sync` |
| `DEVICE_REVOKED` sau khi đăng ký lại | Đăng ký thiết bị mới tự thu hồi thiết bị cũ (có chủ đích) | Dùng token của thiết bị mới |
| Kiosk offline báo `DEVICE_UNKNOWN` dù đã đăng ký | Kiosk chưa từng đồng bộ khóa (fail-secure) | `kiosk-sim sync` khi còn mạng |
| `FAIL expected ... in: ...` trong demo | Hành vi hệ thống đổi | Đọc dòng `FAIL`, so với `docs/runbook/demo-scenarios.md`, xem log service |

## Chế độ Docker

| Triệu chứng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `Cannot connect to the Docker daemon` | Docker chưa bật | Bật Docker Desktop. `make integration` cần Docker, không thể bỏ qua |
| Cổng 5432, 5433, 6379, 5672 đã dùng | Dịch vụ cục bộ khác | Tắt dịch vụ đó hoặc đổi cổng trong `docker/docker-compose.yml` **và** biến môi trường tương ứng |
| `password authentication failed` | Dữ liệu cũ trong volume từ lần khởi tạo trước | `make nuke` rồi `make up` (xóa dữ liệu) |
| Flyway báo checksum không khớp | Đã sửa migration đã áp dụng | Không sửa migration cũ; thêm `V2__...`. Môi trường phát triển: `make nuke` |
| Service không nhận được sự kiện | Hàng đợi chưa được khai báo hoặc chưa bind | Mở `http://localhost:15672` (tmc/tmc), kiểm tra queue `tmc.reporting.attendance` và binding `attendance.*` |
| Thông điệp lặp lại | Phát ít nhất một lần là chủ đích | Consumer phải khử trùng; xem `IdempotentConsumer` |

**Ghi nhớ:** mọi thứ cần Docker **chưa được kiểm chứng** khi tạo scaffold (`docs/architecture/implementation-notes.md` mục 6). Gặp lỗi ở đó là việc của story D-01, B-01, B-02, C-01, A-01: ghi vào issue của story, kèm lệnh và nhật ký.

## Portal

| Triệu chứng | Cách xử lý |
| --- | --- |
| `npm ci` lỗi | Dùng Node 20 trở lên; xóa `node_modules` và chạy lại |
| Trang báo "Bạn không có quyền (HTTP 403)" | Đúng thiết kế: chọn vai trò và cổng phù hợp ở thanh DEV |
| Không kết nối được máy chủ | Service tương ứng chưa chạy; kiểm tra cổng trong `vite.config.ts` |

## Khi không biết nguyên nhân

1. Chạy `make local-status` và `make test`: tách "mã sai" khỏi "môi trường sai".
2. Xem log: `scripts/dev/local-stack.sh logs <service>`. Mỗi lượt quét có `X-Correlation-Id` (`corr-<attempt_id>` từ kiosk-sim).
3. Tra trạng thái lượt: `curl localhost:8082/internal/v1/attempts/<attempt-id>`.
4. Hỏi trong nhóm kèm: lệnh đã chạy, đầu ra đầy đủ, nhánh và commit. Đừng chỉ nói "không chạy".
