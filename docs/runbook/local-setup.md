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
| Đã được kiểm chứng | **Có**, 3 kịch bản demo chạy trên 5 tiến trình thật | **Một phần** (16/11): 7 service khởi động trên hạ tầng thật, đăng nhập JWT, gói đồng bộ, sự kiện qua RabbitMQ; 3 demo chưa chạy trên stack này. Chi tiết: `troubleshooting.md`, mục Kết quả D-01 |

## Chế độ `local` (khuyên dùng khi bắt đầu)

```bash
make local-up       # tự build jar nếu chưa có, khởi động 5 tiến trình, chờ sẵn sàng
make local-status   # turnstile :8090, user-qr :8083, attendance :8082, gate-station :8081, identity :8087
make demo-online    # kiểm tra mọi thứ chạy được
make local-down
```

Năm tiến trình: `turnstile-simulator`, `user-qr`, `attendance`, `identity` (đều profile `local`) và `gate-station` (profile `local`, nối turnstile qua HTTP). Jar chỉ được build khi **thiếu**: sau khi kéo mã mới, chạy `./gradlew bootJar` hoặc xóa jar cũ. Nhật ký ở `.local/<tên>.log`, theo dõi bằng `scripts/dev/local-stack.sh logs gate-station`.

Trạng thái nằm trong bộ nhớ, nên **chạy lại một demo cần `make local-restart`** (nhân viên mẫu của demo sẽ còn trạng thái anti-passback từ lần trước).

## Chế độ đầy đủ (Docker)

```bash
make up             # PostgreSQL edge :5432, PostgreSQL central :5433, Redis :6379, RabbitMQ :5672 (UI :15672, tài khoản tmc/tmc)
```

Container PostgreSQL edge tạo trước ngày 16/11 thiếu database `gate_store` (ADR-14): `make nuke && make up`, hoặc
`docker exec -i tmc-postgres-edge psql -U postgres < docker/init/edge/02-gate-store.sql`.

Chạy từng service trong terminal riêng (hoặc cấu hình IDE), **không** đặt `SPRING_PROFILES_ACTIVE`, để dùng hạ tầng thật:

```bash
./gradlew :services:user-qr:bootRun
./gradlew :services:attendance:bootRun
TURNSTILE_MODE=http ./gradlew :services:gate-station:bootRun   # cần turnstile-simulator hoặc TURNSTILE_MODE=simulated
SPRING_PROFILES_ACTIVE=default,gate-store ./gradlew :services:gate-station:bootRun   # thêm database gate_store
./gradlew :services:identity:bootRun        # JWT + JWKS; user-qr cần nó để kiểm tra Bearer
./gradlew :services:reporting:bootRun       # tương tự hr-integration, audit-admin: PostgreSQL central + RabbitMQ
./gradlew :simulators:turnstile-simulator:bootRun
```

Ở profile mặc định, user-qr **không** tin header `X-Subject`: lấy token bằng
`curl -X POST localhost:8087/auth/v1/login -H 'Content-Type: application/json' -d '{"username":"nv001","password":"dev-only-change-me"}'`
rồi gửi `Authorization: Bearer <accessToken>`.

Biến môi trường đáng biết (đều có mặc định cho máy phát triển):

| Biến | Mặc định | Dùng bởi |
| --- | --- | --- |
| `ATTENDANCE_DB_URL`, `_USER`, `_PASSWORD` | `jdbc:postgresql://localhost:5432/attendance_edge`, `attendance`, `attendance` | attendance |
| `USERQR_DB_URL`, `_USER`, `_PASSWORD` | `jdbc:postgresql://localhost:5433/user_qr`, `userqr`, `userqr` | user-qr |
| `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USER`, `RABBIT_PASSWORD` | `localhost`, `5672`, `tmc`, `tmc` | attendance, user-qr, identity, reporting, hr-integration, audit-admin |
| `REPORTING_DB_*`, `HR_DB_*`, `AUDIT_DB_*` | `jdbc:postgresql://localhost:5433/{reporting,hr_integration,audit_admin}`, login cùng tên trong `docker/init/central` | reporting, hr-integration, audit-admin |
| `GATE_STORE_DB_URL`, `_USER`, `_PASSWORD` | `jdbc:postgresql://localhost:5432/gate_store`, `gatestation`, `gatestation` | gate-station (profile `gate-store`) |
| `IDENTITY_JWKS_URL`, `TMC_ALLOW_DEV_HEADERS` | `http://localhost:8087/.well-known/jwks.json`, `false` | user-qr |
| `IDENTITY_JWT_PRIVATE_JWK` | trống (tự sinh khóa, chỉ dev) | identity |
| `IDENTITY_JWT_PREVIOUS_PUBLIC_JWK` | trống (không đang xoay khóa) | identity (C-12) |
| `IDENTITY_DB_URL`, `_USER`, `_PASSWORD` | `jdbc:postgresql://localhost:5433/identity`, `identity`, `identity` | identity (outbox audit, C-11) |
| `EMPLOYEE_SYNC_ENABLED` | `true` | gate-station (làm mới danh bạ từ user-qr mỗi 30 giây) |
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
