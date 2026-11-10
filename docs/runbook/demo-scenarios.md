# Kịch bản demo (bằng chứng cho báo cáo)

Mỗi kịch bản là một script tự kiểm: in `OK` cho từng khẳng định và `FAIL` rồi thoát mã khác 0 nếu sai. **Đã được chạy xanh trên stack `local`** (4 tiến trình thật, HTTP thật, xác thực JWS thật giữa các service). Đính kèm đầu ra của chúng vào báo cáo.

## Chuẩn bị

```bash
make local-up        # lần đầu sẽ build jar và công cụ mô phỏng
```

Trạng thái ở trong bộ nhớ và mỗi demo dùng một nhân viên mẫu riêng (`...0001` online, `...0004` offline, `...0005` timeout). **Muốn chạy lại một demo: `make local-restart`.**

## 1. `make demo-online`: đường thành công và các từ chối bảo vệ ADR-12

| Bước | Điều xảy ra | Khẳng định |
| --- | --- | --- |
| 1 | Điện thoại giả lập sinh khóa P-256 và đăng ký khóa công khai (khóa riêng không rời `device-sim`) | `registered deviceId=...` |
| 2 | Kiosk kéo gói đồng bộ (cổng, khóa thiết bị) | `synced` |
| 3 | Quét token 30 giây ở cổng vào `BD-GATE-01` | `ALLOWED`, `mode=ONLINE` |
| 4 | Quét **lại cùng QR** (như ảnh chụp màn hình) ở cổng ra | `QR_ALREADY_USED`, cổng đóng |
| 5 | Token sống 24 giờ (giống QR tĩnh) | `MALFORMED_TOKEN` |
| 6 | Token chứa trường `name` (dữ liệu cá nhân) | `MALFORMED_TOKEN` |
| 7 | Quét vào lần hai liên tiếp | `ANTI_PASSBACK_VIOLATION` |

Chứng minh: ADR-12 (replay, QR sống lâu, PII), FR-18 (anti-passback), NFR-05.

## 2. `make demo-offline`: kiosk mất kết nối (ADR-11 điểm P6, ADR-12 R2)

| Bước | Điều xảy ra | Khẳng định |
| --- | --- | --- |
| 1 | Đăng ký và đồng bộ khi còn mạng (kiosk cần có khóa) | |
| 2 | `kiosk-sim offline on`: giả lập mất kết nối | |
| 3 | Quét: kiosk tự xác thực, **ghi journal trước**, mở qua local gate link | `ALLOWED`, `mode=OFFLINE`, `[LOCAL GATE LINK] OPEN` |
| 4 | Quét lại cùng token khi vẫn offline | `QR_ALREADY_USED` (replay cache cục bộ) |
| 5 | Journal: 2 dòng, `handed_over=false` | |
| 6 | `offline off`, rồi `flush`: giao journal theo thứ tự | `handedOver=2`, cả hai `ACCEPTED` |
| 7 | `flush` lần nữa | `handedOver=0` (không gửi hai lần) |

Chứng minh: NFR-09, journal ghi trước (ADR-11 mục 2), nhập theo thứ tự (mục 5), không trùng.

**Giới hạn cần nói thật:** demo giả lập "mất mạng" bằng cờ `offline on`; việc kiosk tự rơi về offline khi Edge không trả lời (sau 2 lần thử lại cùng `attempt_id`) được chứng minh bằng test `KioskAppTest`, không bằng demo này.

## 3. `make demo-timeout`: cổng mở nhưng phản hồi bị mất (ADR-11 điểm P3)

Đây là tình huống nguy hiểm nhất: cổng đã mở mà hệ thống không biết.

| Bước | Điều xảy ra | Khẳng định |
| --- | --- | --- |
| 1 | Đăng ký, đồng bộ; ghi số lần mở cổng `BD-HW-03` | |
| 2 | Bật lỗi: turnstile **mở cổng nhưng trả lời sau 1,5 s** (giới hạn đọc của ta là 0,8 s) | |
| 3 | Quét | `PENDING`, `TURNSTILE_TIMEOUT_RETRY_SCAN` (HTTP 202) |
| 4 | Gỡ lỗi. Số lần mở cổng tăng **đúng 1** | `pass_count` tăng 1 |
| 5 | Đối soát tự chạy: attendance hỏi gate-station, gate-station hỏi `GET /gates/{id}/status` | `COMMITTED` trong vòng 30 giây |
| 6 | Số lần mở cổng vẫn tăng **đúng 1** | Không có lệnh mở thứ hai |

Chứng minh: ADR-11 (không gửi lại lệnh vật lý khi chưa rõ, đối soát), đúng tiêu chí "không mất, không trùng".

## Chạy tay để khám phá

```bash
make sims
DEV=simulators/device-simulator/build/install/device-sim/bin/device-sim
KIOSK=simulators/kiosk-simulator/build/install/kiosk-sim/bin/kiosk-sim

$DEV keygen --name emp1 --employee 0192a1c0-0000-7000-8000-000000000002
$DEV register --name emp1 --user-qr http://localhost:8083
$KIOSK sync --edge http://localhost:8081 --kiosk-id BD-KIOSK-01
$KIOSK scan --gate BD-GATE-01 --edge http://localhost:8081 --kiosk-id BD-KIOSK-01 "$($DEV token --name emp1)"
$KIOSK status --kiosk-id BD-KIOSK-01
```

Gây lỗi tại turnstile giả lập (áp dụng ngay, cho một cổng phần cứng hoặc `*`):

```bash
curl -X POST localhost:8090/admin/fault -H 'Content-Type: application/json' -d '{"gateId":"BD-HW-01","mode":"JAM"}'
# các mode: NONE, JAM (409), OFFLINE (503), GATEWAY_TIMEOUT (504), DELAY_OPENED, DELAY_NOT_OPENED (kèm "delayMs")
curl -X DELETE localhost:8090/admin/fault       # gỡ mọi lỗi
curl localhost:8090/gates/BD-HW-01/status       # xem pass_count và last_command_*
```

Xem trạng thái một lượt ở attendance: `curl localhost:8082/internal/v1/attempts/<attempt-id>`.

## Nhân viên và cổng mẫu (seed của gate-station)

| Mã | Trạng thái | Nhà máy |
| --- | --- | --- |
| `0192a1c0-0000-7000-8000-000000000001` | đang làm | BD, LA |
| `...0002` | đang làm | BD |
| `...0003` | **đã nghỉ việc** | BD |
| `...0004`, `...0005` | đang làm | BD, LA |

| Cổng | Chế độ | Phần cứng |
| --- | --- | --- |
| `BD-GATE-01` | một chiều vào | `BD-HW-01` |
| `BD-GATE-02` | một chiều ra | `BD-HW-02` |
| `BD-GATE-03` | hai chiều | `BD-HW-03` |
| `LA-GATE-01` | hai chiều | `LA-HW-01` |

## Mở thủ công bằng bảo vệ (UC-06)

```bash
curl -X POST localhost:8081/security/v1/gates/BD-GATE-01/manual-open \
  -H 'Content-Type: application/json' \
  -H 'X-Subject: 0192a1c0-0000-7000-8000-00000000b001' -H 'X-Roles: SECURITY_GUARD' -H 'X-Scope-Gates: BD-GATE-01' \
  -d '{"employee_uuid":"0192a1c0-0000-7000-8000-000000000002","direction":"ENTRY","reason":"điện thoại hết pin"}'
```

Đổi `X-Scope-Gates` thành cổng khác sẽ nhận 403. Lượt này được ghi `MANUAL`, **không** tạo công tự động cho tới khi HR xác minh (B-07).
