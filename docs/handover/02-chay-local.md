# Runbook: dựng server local, kiểm thử, dùng app nhân viên

Làm theo tài liệu này trong ngày 12/11. Mục tiêu của ngày đầu: mỗi người tự chạy được stack, 3 demo và app nhân viên trên máy mình.

Mọi lệnh chạy từ thư mục gốc của repo. Lệnh Gradle ghi theo kiểu macOS/Linux (`./gradlew`); trên Windows PowerShell thay bằng `.\gradlew.bat`.

## 1. Cài đặt một lần

| Cần | Phiên bản | Kiểm tra | Ghi chú |
| --- | --- | --- | --- |
| JDK | 21 | `java -version` | Đặt `JAVA_HOME` trỏ tới JDK 21. IntelliJ: *Settings > Build Tools > Gradle > Gradle JVM* chọn 21 |
| Node.js | 20 trở lên | `node -v` | Cho hai ứng dụng web |
| Git Bash (Windows) | bất kỳ | `bash --version` | Để chạy 3 script demo. macOS và Linux có sẵn bash |
| Docker Desktop | bất kỳ | `docker version` | **Không bắt buộc** cho stack local. Bắt buộc cho test tích hợp và profile mặc định |
| `make` | bất kỳ | `make -v` | Không bắt buộc. `Makefile` chỉ là tên tắt, và chỉ chạy đúng trên macOS/Linux |

## 2. Build lần đầu

```bash
./gradlew build
```

Kết quả mong đợi: `BUILD SUCCESSFUL`. Lần đầu mất vài phút vì tải thư viện.

Trên **Windows**, 20 test của `kiosk-simulator` hiện lỗi do khóa file SQLite (story D-14). Cho đến khi sửa xong, dùng:

```powershell
.\gradlew.bat build -x :simulators:kiosk-simulator:test
```

## 3. Dựng stack local (không cần Docker)

Stack local gồm 5 tiến trình chạy profile `local`: dữ liệu nằm trong bộ nhớ, các service gọi nhau bằng HTTP thật.

| Tiến trình | Cổng | Vai trò |
| --- | --- | --- |
| turnstile-simulator | 8090 | Cổng xoay giả lập |
| user-qr | 8083 | Khóa thiết bị, xác thực QR |
| attendance | 8082 | Sổ lượt quét |
| gate-station | 8081 | Điều phối lượt quét |
| identity | 8087 | Đăng nhập (cần cho app nhân viên) |

### Windows

```powershell
pwsh scripts/dev/local-stack.ps1 start     # build nếu thiếu, khởi động, chờ sẵn sàng
pwsh scripts/dev/local-stack.ps1 status
pwsh scripts/dev/local-stack.ps1 restart   # xóa sạch trạng thái trong bộ nhớ
pwsh scripts/dev/local-stack.ps1 stop
```

### macOS và Linux

```bash
make local-up          # 4 service của edge
make local-status
make local-restart
make local-down
# identity chưa nằm trong script này, chạy riêng ở một cửa sổ khác:
./gradlew :services:identity:bootRun
```

Log nằm trong thư mục `.local/` (ví dụ `.local/gate-station.log`).

### Kiểm tra stack đã sống

```bash
curl -s http://localhost:8081/kiosk/v1/sync-bundle      # danh mục cổng
curl -s http://localhost:8090/gates/BD-HW-01/status     # trạng thái cổng xoay
```

### Chạy từng service bằng tay hoặc trong IntelliJ

Khi cần debug một service, dừng nó trong stack rồi tự chạy với biến môi trường tương ứng:

| Service | Lệnh Gradle | Biến môi trường |
| --- | --- | --- |
| turnstile-simulator | `:simulators:turnstile-simulator:bootRun` | không |
| user-qr | `:services:user-qr:bootRun` | `SPRING_PROFILES_ACTIVE=local` |
| attendance | `:services:attendance:bootRun` | `SPRING_PROFILES_ACTIVE=local` |
| gate-station | `:services:gate-station:bootRun` | `SPRING_PROFILES_ACTIVE=local`, `TURNSTILE_MODE=http` |
| identity | `:services:identity:bootRun` | không |
| reporting, hr-integration, audit-admin | `:services:<tên>:bootRun` | `SPRING_PROFILES_ACTIVE=inmem` |

Trong IntelliJ: mở lớp `...Application` trong gói `bootstrap`, bấm Run, rồi thêm biến môi trường ở *Edit Configurations > Environment variables*.

## 4. Ba kịch bản demo

Cần stack đang chạy và hai công cụ dòng lệnh đã build:

```bash
./gradlew :simulators:device-simulator:installDist :simulators:kiosk-simulator:installDist
```

Chạy (Windows: mở Git Bash, bảo đảm `java` có trên `PATH`):

```bash
bash scripts/demo/online.sh     # quét hợp lệ, dùng lại QR, QR 24 giờ, QR chứa tên, anti-passback
bash scripts/demo/offline.sh    # mất mạng: kiosk tự xác thực, gửi bù, không gửi trùng
bash scripts/demo/timeout.sh    # cổng trả lời trễ: PENDING, đối soát chốt công, không mở lần hai
```

Mỗi script in `OK` hoặc `FAIL` cho từng bước và kết thúc bằng `... demo passed`.

**Chạy lại lần hai thì khởi động lại stack trước.** Trạng thái anti-passback nằm trong bộ nhớ, nên nhân viên của lần chạy trước vẫn "đang ở trong nhà máy" và bước quét đầu sẽ bị từ chối.

## 5. App nhân viên và trang kiosk

`apps/employee-app` là ứng dụng web giả lập điện thoại của nhân viên, kèm một trang giả lập kiosk.

```bash
cd apps/employee-app
npm install          # một lần
npm run dev          # http://localhost:5174
```

### 5.1. Tài khoản mẫu

Mật khẩu chung: `dev-only-change-me`. Khai báo ở `services/identity/.../bootstrap/IdentityConfig.java`; quyền vào nhà máy khai báo ở `services/gate-station/src/main/resources/application.yml`.

| Tài khoản | Nhân viên (UUID kết thúc bằng) | Tại cổng |
| --- | --- | --- |
| `nv001` | `...0001` | Được vào BD và LA |
| `nv002` | `...0002` | Chỉ được vào BD |
| `nv003` | `...0003` | Đã nghỉ việc, cổng từ chối với `EMPLOYEE_INACTIVE` |
| `nv004` | `...0004` | Được vào BD và LA |
| `nv005` | `...0005` | Được vào BD và LA |

### 5.2. Luồng kiểm thử trên một máy

1. Mở <http://localhost:5174>, đăng nhập `nv001`.
2. Bấm **Tạo khóa và đăng ký**. Trình duyệt tạo cặp khóa; khóa riêng được lưu ở dạng không xuất ra được.
3. Mã QR hiện ra, tự đổi mỗi 20 giây, mỗi mã sống 30 giây.
4. Ở mục **Thử ngay không cần máy quét**, chọn cổng rồi bấm **Gửi tới cổng**. Kết quả mong đợi: `CỔNG MỞ`.

### 5.3. Các chế độ kiểm thử trong app

| Chế độ | App làm gì | Kết quả mong đợi tại cổng |
| --- | --- | --- |
| Bình thường | QR đổi mỗi 20 giây | `ALLOWED` |
| Ảnh chụp màn hình | QR đứng yên | Gửi lần hai: `QR_ALREADY_USED`. Để quá 90 giây: `QR_EXPIRED` |
| QR sống 24 giờ | Token có hạn 24 giờ | `MALFORMED_TOKEN` |
| QR chứa họ tên | Thêm claim `name` | `MALFORMED_TOKEN` |

Các tình huống khác thử được bằng tài khoản và cổng:

| Muốn thấy | Cách làm |
| --- | --- |
| `ANTI_PASSBACK_VIOLATION` | Quét hai lần liên tiếp ở `BD-GATE-01` (cổng chỉ vào) bằng hai mã khác nhau |
| `FACTORY_NOT_PERMITTED` | Đăng nhập `nv002`, quét ở `LA-GATE-01` |
| `EMPLOYEE_INACTIVE` | Đăng nhập `nv003`, quét ở cổng bất kỳ |
| `DEVICE_REVOKED` | Bấm **Thu hồi thiết bị này**, rồi dán một token cũ vào trang kiosk |
| Vào rồi ra | Quét hai lần ở `BD-GATE-03` (cổng hai chiều): lần đầu `ENTRY`, lần sau `EXIT` |

### 5.4. Trang kiosk quét bằng webcam

Mở <http://localhost:5174/kiosk> trên máy có webcam, chọn cổng, bấm **Bật camera và bắt đầu quét**, rồi đưa QR trên điện thoại vào trước camera. Trang gửi `POST /kiosk/v1/scans` tới gate-station, đúng như một kiosk thật khi có mạng.

Không có webcam thì mở mục **Không có camera? Dán token** và dán token đã sao chép từ app.

Trang kiosk web **không có journal và không có chế độ mất mạng**. Muốn thử mất mạng thì dùng `kiosk-sim` (mục 6).

### 5.5. Dùng điện thoại thật qua mạng LAN

Trình duyệt chỉ cho dùng WebCrypto và camera trên HTTPS hoặc `localhost`, nên điện thoại phải vào bằng HTTPS:

```bash
npm run dev:lan      # in ra địa chỉ dạng https://192.168.x.x:5174
```

1. Điện thoại và máy tính cùng mạng Wi-Fi.
2. Trên điện thoại mở địa chỉ `https://<IP máy tính>:5174`. Trình duyệt cảnh báo chứng chỉ tự ký; chọn tiếp tục.
3. Nếu không vào được, cho phép Node.js qua tường lửa của máy tính (Windows hỏi ở lần chạy đầu).
4. Trên máy tính mở `https://localhost:5174/kiosk` để quét.

Điện thoại chỉ nói chuyện với địa chỉ này; Vite chuyển tiếp tới các service (`vite.config.ts`), nên không cần mở cổng 8081, 8083, 8087 ra mạng.

### 5.6. Giới hạn cần biết

- Đăng nhập chưa cấp JWT. App nhận `subject` rồi gửi lại qua header `X-Subject`, giống cách `web-portal` và `device-simulator` đang làm. Việc thay bằng JWT là story C-04 và D-13.
- Ở profile `local`, `user-qr` lưu thiết bị trong bộ nhớ. Khởi động lại stack thì server quên thiết bị, cổng trả `DEVICE_UNKNOWN`; app sẽ hiện nút **Đăng ký lại thiết bị**.
- Đồng hồ của điện thoại lệch quá 60 giây so với máy chủ thì bị `CLOCK_SKEW_EXCEEDED` hoặc `QR_EXPIRED`.

### 5.7. Tạo lại ảnh chụp và GIF trong README

Ảnh trong `docs/screenshots/` được chụp tự động bằng Chrome chạy ngầm, từ app thật với stack thật. Khi giao diện đổi, tạo lại:

```bash
# cần stack local vừa khởi động lại (trạng thái sạch) và app đang chạy ở cổng 5174
cd scripts/docs
npm install          # một lần
node capture-screenshots.mjs
```

Script dừng với lỗi nếu một bước của luồng không cho kết quả mong đợi, nên nó cũng dùng được như một bài kiểm thử giao diện từ đầu đến cuối.

Ảnh của web portal (bảo vệ, quản đốc, HR) cần sự kiện đi qua RabbitMQ, nên phải chạy trên hạ tầng thật (mục 9) với cả 8 service ở profile mặc định, cùng `npm run dev` trong `apps/web-portal`:

```bash
cd scripts/docs
node capture-portal.mjs      # ảnh 09, 10, 11
node render-gantt.mjs        # vẽ lại docs/handover/gantt.png từ 03-gantt.md
```

## 6. Kiosk dòng lệnh (có journal và chế độ mất mạng)

```bash
K=simulators/kiosk-simulator/build/install/kiosk-sim/bin/kiosk-sim
ARGS="--edge http://localhost:8081 --db /tmp/kiosk.db --kiosk-id BD-KIOSK-01"

$K sync $ARGS                              # lấy danh mục cổng và khóa thiết bị
$K scan --gate BD-GATE-03 "<token>" $ARGS  # quét; mất mạng thì tự chuyển offline
$K offline on $ARGS                        # ép mất mạng
$K flush $ARGS                             # gửi journal lên khi có mạng lại
$K status $ARGS                            # xem journal
```

`<token>` lấy từ nút **Sao chép token** trong app, hoặc từ `device-sim token`. Token chỉ sống 30 giây, nên dán ngay.

## 7. Chèn lỗi cổng xoay

```bash
# cổng mở nhưng trả lời sau 1,5 giây (ngưỡng chờ của gate-station là 0,8 giây)
curl -X POST http://localhost:8090/admin/fault -H 'Content-Type: application/json' \
  -d '{"gateId":"BD-HW-03","mode":"DELAY_OPENED","delayMs":1500}'

curl -X DELETE http://localhost:8090/admin/fault      # bỏ mọi lỗi
```

| `mode` | Cổng làm gì | Kiosk nhận |
| --- | --- | --- |
| `JAM` | Kẹt, không mở (409) | `DENIED`, `GATE_HARDWARE_ERROR`, token được nhả |
| `OFFLINE` | Bộ điều khiển tắt (503) | `DENIED`, `GATE_HARDWARE_ERROR` |
| `GATEWAY_TIMEOUT` | 504 | `PENDING` |
| `DELAY_OPENED` | Mở nhưng trả lời trễ | `PENDING`, sau đó đối soát chốt `COMMITTED` |
| `DELAY_NOT_OPENED` | Không mở, trả lời trễ | `PENDING`, sau đó đối soát chốt `GATE_FAILED` |

`gateId` ở đây là mã phần cứng (`BD-HW-03`), không phải mã cổng của TMC (`BD-GATE-03`).

Xem trạng thái một lượt quét: `curl -s http://localhost:8082/internal/v1/attempts/<attempt_id>`.

## 8. Chạy test

| Muốn | Lệnh | Cần Docker |
| --- | --- | --- |
| Mọi test đơn vị và kiến trúc | `./gradlew test` | Không |
| Test của một service | `./gradlew :services:attendance:test` | Không |
| Một lớp test | `./gradlew :services:gate-station:test --tests '*RecordScanUseCaseTest'` | Không |
| Chỉ luật kiến trúc | `./gradlew test --tests '*ArchitectureTest'` | Không |
| Test tích hợp (thẻ `integration`) | `./gradlew integrationTest` | Có |
| Test tích hợp của một service | `./gradlew :services:attendance:integrationTest` | Có |
| Kiểm định dạng mã | `./gradlew spotlessCheck` | Không |
| Tự sửa định dạng | `./gradlew spotlessApply` | Không |
| App nhân viên | `cd apps/employee-app && npm test && npm run lint && npm run build` | Không |
| Web portal | `cd apps/web-portal && npm test && npm run lint && npm run build` | Không |

Báo cáo test dạng HTML: `<module>/build/reports/tests/test/index.html`.

Trước khi mở PR, chạy tối thiểu: `./gradlew spotlessApply test`.

## 9. Hạ tầng thật bằng Docker

```bash
docker compose -f docker/docker-compose.yml up -d      # PostgreSQL edge :5432, PostgreSQL central :5433, Redis :6379, RabbitMQ :5672
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml down       # dừng, giữ dữ liệu
docker compose -f docker/docker-compose.yml down -v    # dừng và XÓA dữ liệu
```

Giao diện quản trị RabbitMQ: <http://localhost:15672> (tài khoản `tmc` / `tmc`).

Chạy service với hạ tầng thật: bỏ biến `SPRING_PROFILES_ACTIVE` (profile mặc định). Thứ tự khởi động: hạ tầng, `user-qr`, `attendance`, `gate-station`, rồi các consumer.

Test tích hợp dùng Testcontainers nên tự dựng container riêng; chỉ cần Docker đang chạy, không cần `docker compose up`.

## 10. Xử lý sự cố

| Triệu chứng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| `cannot find symbol ... port.out` khi build | Bản repo cũ thiếu thư mục `adapter/out`, `port/out` do `.gitignore` chặn `out/` | Kéo bản mới nhất; `.gitignore` phải ghi `/out/`, không phải `out/` |
| `Unsupported class file major version` hoặc lỗi toolchain | Gradle chạy bằng JDK khác 21 | Đặt lại `JAVA_HOME`; trong IntelliJ chọn lại Gradle JVM |
| `Port 8081 was already in use` | Stack cũ còn chạy | `local-stack.ps1 stop` hoặc `make local-down` |
| `local-stack.ps1 start` không trả lại dấu nhắc | Đầu ra của script bị nối qua pipe (ví dụ `| tail`), các tiến trình Java giữ pipe mở | Chạy script trực tiếp, không nối pipe; stack vẫn lên bình thường |
| Demo `FAIL` ngay bước quét đầu với `ANTI_PASSBACK_VIOLATION` | Chạy demo lần hai mà chưa xóa trạng thái | Khởi động lại stack |
| App báo `DEVICE_UNKNOWN` | Stack vừa khởi động lại, `user-qr` quên thiết bị | Bấm **Đăng ký lại thiết bị** |
| App báo "Trình duyệt chặn WebCrypto" | Mở bằng `http://<IP>` thay vì HTTPS | Dùng `npm run dev:lan` và địa chỉ `https://` |
| App báo không kết nối được dịch vụ đăng nhập | `identity` chưa chạy | Khởi động `identity` (cổng 8087) |
| Kết quả quét là `LỖI HỆ THỐNG`, mã `DEPENDENCY_UNAVAILABLE` | `attendance` hoặc `user-qr` đang tắt | Xem `.local/*.log`, khởi động lại service đó |
| `QR_EXPIRED` dù vừa tạo mã | Đồng hồ điện thoại lệch | Bật giờ tự động trên điện thoại |
| 20 test `kiosk-simulator` đỏ trên Windows | Khóa file SQLite | Thêm `-x :simulators:kiosk-simulator:test`, theo dõi D-14 |
| `spotlessCheck` đỏ | Mã chưa định dạng | `./gradlew spotlessApply` |
| `ArchitectureTest` đỏ | Vi phạm chiều phụ thuộc (ví dụ `domain` import Spring) | Chuyển mã về đúng gói; xem [00-he-thong.md](00-he-thong.md) mục 7 |
| Test tích hợp báo không tìm thấy Docker | Docker Desktop chưa chạy | Mở Docker Desktop rồi chạy lại |

Tài liệu cũ hơn nhưng vẫn đúng cho macOS: [`docs/runbook/local-setup.md`](../runbook/local-setup.md), [`docs/runbook/troubleshooting.md`](../runbook/troubleshooting.md).
