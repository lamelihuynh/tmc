# Cài đặt máy phát triển

## Yêu cầu

| Công cụ | Phiên bản | Kiểm tra | Cần cho |
| --- | --- | --- | --- |
| JDK | **21** (Temurin hoặc tương đương) | `java -version` (macOS: `/usr/libexec/java_home -v 21`) | Mọi thứ |
| Git | 2.30+ | `git --version` | Mọi thứ |
| Node | 20+ | `node --version` | Chỉ `apps/web-portal` |
| Docker Desktop | 24+ | `docker info` | Chỉ stack đầy đủ và `make integration` |
| `make`, `curl`, `lsof`, `python3` | có sẵn trên macOS và Linux | | Script demo |

Không cần cài Gradle: dùng `./gradlew` (Gradle 8.11). Lần chạy đầu tải phụ thuộc, mất vài phút.

Nếu máy có nhiều JDK, đặt `JAVA_HOME` về JDK 21 trước khi chạy:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
```

## Lần đầu

```bash
git clone <url-repo> tmc && cd tmc
make build          # biên dịch, chạy test đơn vị và kiến trúc cho mọi module
```

`make build` xanh là điều kiện để bắt đầu làm việc. Nếu đỏ ngay ở máy sạch, đó là lỗi của repo: mở issue, đừng sửa riêng.

## Hai chế độ chạy

| | Chế độ `local` (không Docker) | Chế độ đầy đủ (Docker) |
| --- | --- | --- |
| Lưu trữ | trong bộ nhớ, **mất khi khởi động lại** | PostgreSQL, Redis, RabbitMQ thật |
| Khi nào dùng | Phát triển hằng ngày, demo, học luồng | Kiểm thử tích hợp, kiểm chứng B-01, A-01 |
| Lệnh | `make local-up` | `make up` rồi chạy từng service |
| Đã được kiểm chứng | **Có**, 3 kịch bản demo chạy trên 4 tiến trình thật | **Chưa**: xem `docs/architecture/implementation-notes.md` mục 6 |

## Chế độ `local` (khuyên dùng khi bắt đầu)

```bash
make local-up       # tự build jar nếu chưa có, khởi động 4 tiến trình, chờ sẵn sàng
make local-status   # turnstile :8090, user-qr :8083, attendance :8082, gate-station :8081
make demo-online    # kiểm tra mọi thứ chạy được
make local-down
```

Bốn tiến trình: `turnstile-simulator`, `user-qr`, `attendance` (đều profile `local`) và `gate-station` (profile `local`, nối turnstile qua HTTP). Nhật ký ở `.local/<tên>.log`, theo dõi bằng `scripts/dev/local-stack.sh logs gate-station`.

Trạng thái nằm trong bộ nhớ, nên **chạy lại một demo cần `make local-restart`** (nhân viên mẫu của demo sẽ còn trạng thái anti-passback từ lần trước).

## Chế độ đầy đủ (Docker)

```bash
make up             # PostgreSQL edge :5432, PostgreSQL central :5433, Redis :6379, RabbitMQ :5672 (UI :15672, tài khoản tmc/tmc)
```

Chạy từng service trong terminal riêng (hoặc cấu hình IDE), **không** đặt `SPRING_PROFILES_ACTIVE`, để dùng hạ tầng thật:

```bash
./gradlew :services:user-qr:bootRun
./gradlew :services:attendance:bootRun
TURNSTILE_MODE=http ./gradlew :services:gate-station:bootRun   # cần turnstile-simulator hoặc TURNSTILE_MODE=simulated
./gradlew :simulators:turnstile-simulator:bootRun
```

Biến môi trường đáng biết (đều có mặc định cho máy phát triển):

| Biến | Mặc định | Dùng bởi |
| --- | --- | --- |
| `ATTENDANCE_DB_URL`, `_USER`, `_PASSWORD` | `jdbc:postgresql://localhost:5432/attendance_edge`, `attendance`, `attendance` | attendance |
| `USERQR_DB_URL`, `_USER`, `_PASSWORD` | `jdbc:postgresql://localhost:5433/user_qr`, `userqr`, `userqr` | user-qr |
| `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USER`, `RABBIT_PASSWORD` | `localhost`, `5672`, `tmc`, `tmc` | attendance, reporting, hr-integration, audit-admin |
| `REDIS_HOST`, `REDIS_PORT` | `localhost`, `6379` | gate-station |
| `ATTENDANCE_URL`, `USERQR_URL`, `GATE_STATION_URL` | `:8082`, `:8083`, `:8081` | gate-station, attendance |
| `TURNSTILE_MODE`, `TURNSTILE_URL`, `TURNSTILE_TOKEN` | `http`, `http://localhost:8090`, `dev-turnstile-token` | gate-station |

Mật khẩu trong compose và yml chỉ cho máy phát triển. **Không dùng lại ở nơi khác.**

## Công cụ dòng lệnh mô phỏng

```bash
make sims    # dựng device-sim và kiosk-sim vào simulators/*/build/install/*/bin
```

Dùng trực tiếp: xem `docs/runbook/demo-scenarios.md`. Hồ sơ khóa của `device-sim` lưu ở `~/.tmc-sim/devices`, journal của kiosk ở `~/.tmc-sim/<kiosk-id>.db` (khóa **giả lập**, không phải dữ liệu thật).

## Portal

```bash
cd apps/web-portal
npm ci
npm run dev        # http://localhost:5173, proxy tới các service ở cổng đã nêu trong vite.config.ts
npm run lint && npm test && npm run build
```

Thanh "DEV" ở đầu trang chọn vai trò giả lập (thay cho đăng nhập). Chỉ dùng để phát triển.

## IDE

- Mở thư mục gốc như một dự án Gradle; chọn JDK 21.
- Bật google-java-format cùng cấu hình Spotless (`make format` là chuẩn).
- Chạy `ArchitectureTest` thường xuyên: nó cho biết bạn có đặt mã sai tầng không.
