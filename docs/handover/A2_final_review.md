# Rà soát cuối A2 — Gate Station

**Ngày:** 23/11/2026
**Kết luận:** Chưa thể đóng A2. A-01–A-06 đã có implementation và test xanh; A-09 được hoãn đến sau khi A1 hoàn tất. Còn một rủi ro A-02 liên quan kết quả `DUPLICATE` cần phối hợp Team B.

## Trạng thái và file liên quan

| Mã | Trạng thái | File chính / bằng chứng |
| --- | --- | --- |
| A-01 — Redis AOF | Đã làm, IT xanh | [RedisAntiPassbackStore.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/adapter/out/redis/RedisAntiPassbackStore.java), [RedisAdaptersIT.java](../../services/gate-station/src/test/java/vn/tmc/gatestation/adapter/it/RedisAdaptersIT.java) kiểm tra khóa `USED` còn sau restart. |
| A-02 — Anti-passback sau batch offline | Đã làm, IT xanh; cần xử lý rủi ro `DUPLICATE` | [KioskSupportService.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/application/usecase/KioskSupportService.java), [KioskSupportServiceTest.java](../../services/gate-station/src/test/java/vn/tmc/gatestation/application/KioskSupportServiceTest.java): cập nhật lượt `ACCEPTED`/`OFFLINE_OPENED`, không cho dữ liệu cũ ghi đè lượt mới; có test thứ tự thời gian, anomaly và attempt trùng. |
| A-03 — Khôi phục anti-passback từ Attendance | Đã làm; unit/HTTP test xanh | [RecordScanUseCase.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/application/usecase/RecordScanUseCase.java), [KioskApiTest.java](../../services/gate-station/src/test/java/vn/tmc/gatestation/adapter/KioskApiTest.java): thiếu `last` thì đọc Attendance; dependency lỗi trả 503 và không mở cổng. Chưa kiểm I-1 với Attendance thật. |
| A-05 — Turnstile simulator | Đã làm; unit/contract IT xanh | [GateState.java](../../simulators/turnstile-simulator/src/main/java/vn/tmc/turnstilesim/GateState.java), [TurnstileController.java](../../simulators/turnstile-simulator/src/main/java/vn/tmc/turnstilesim/TurnstileController.java), [TurnstileSimulatorTest.java](../../simulators/turnstile-simulator/src/test/java/vn/tmc/turnstilesim/TurnstileSimulatorTest.java), [TurnstileSimulatorContractIT.java](../../services/gate-station/src/test/java/vn/tmc/gatestation/adapter/it/TurnstileSimulatorContractIT.java). Kiểm tra idempotency, replay status, timeout/status và validation. |
| A-06 — Replay giữ direction | Đã làm; unit test xanh | [RecordScanUseCase.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/application/usecase/RecordScanUseCase.java), [RecordScanUseCaseTest.java](../../services/gate-station/src/test/java/vn/tmc/gatestation/application/RecordScanUseCaseTest.java). |
| A-07, A-08 | Cắt theo backlog | Không tính là việc A2 còn nợ; xem [WORK-BREAKDOWN.md](../team/WORK-BREAKDOWN.md). |
| A-09 — Kiểm thử tải | **Hoãn** | Chưa đưa bài đo và kết quả A-09 vào phạm vi hiện tại; sẽ làm lại sau khi A1 hoàn tất. |

### Các thay đổi hỗ trợ liên quan

- Retry cùng `attemptId` được phân biệt với attempt khác qua [ITokenReservation.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/application/port/out/ITokenReservation.java), [RedisTokenReservation.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/adapter/out/redis/RedisTokenReservation.java) và [InMemoryTokenReservation.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/adapter/out/inmem/InMemoryTokenReservation.java); flow nằm trong `RecordScanUseCase.java` ở trên.
- Port/adapters anti-passback gồm [IAntiPassbackStore.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/application/port/out/IAntiPassbackStore.java), [InMemoryAntiPassbackStore.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/adapter/out/inmem/InMemoryAntiPassbackStore.java) và [FakeAttendanceGateway.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/adapter/out/inmem/FakeAttendanceGateway.java).
- Contract simulator được cập nhật tại [turnstile.openapi.yaml](../api/turnstile.openapi.yaml); ghi chú triển khai liên quan A-02/A-03/A-06 được cập nhật tại [implementation-notes.md](../architecture/implementation-notes.md).

## Phần A1 cần phối hợp

- **Wiring dùng chung:** [GateStationConfig.java](../../services/gate-station/src/main/java/vn/tmc/gatestation/bootstrap/GateStationConfig.java) phải inject `IAntiPassbackStore` vào `KioskSupportService` để A-02 cập nhật cùng store mà quét online sử dụng. A2 chỉ sửa hunk `kioskSupport`; nếu A1 đang sửa file thì ghép hunk và nhờ A1 review.
- **Dependency test:** [build.gradle.kts](../../services/gate-station/build.gradle.kts) cần `testImplementation(project(":simulators:turnstile-simulator"))` cho contract IT A-05. A1 giữ file build theo phân công; A2 gửi yêu cầu và không ghi đè thay đổi khác.
- **A-09:** phần đo chung A1+A2, hoãn đến sau khi A1 hoàn tất; khi bắt đầu cần thống nhất profile, warm-up và tiêu chí.

Quyền sở hữu và quy tắc file chung theo [team-a.md](team-a.md). Nội dung trên không kết luận các story A1 khác (A-04/A-11/A-12) đã hoàn tất.

## Chạy kiểm tra cuối

JDK 21 và Docker Engine 28.5.1 khả dụng. Chạy lại, không dùng Gradle cache:

```powershell
. .\.local\use-jdk21.ps1
.\gradlew.bat --rerun-tasks :services:gate-station:test :services:gate-station:integrationTest :simulators:turnstile-simulator:test spotlessCheck
```

**PASS:** 89 test, 0 failure/error/skip; 12 integration test (`GateStoreIT` 2, `RedisAdaptersIT` 9, `TurnstileSimulatorContractIT` 1); `spotlessCheck` pass.

Không chạy full HTTP E2E trong lượt cuối: các cổng 8081, 8082, 8083, 8090 đều không lắng nghe. Vì vậy I-1 (Attendance thật) và I-7 (stack Docker đầy đủ) còn chưa xác minh. A-09 được hoãn theo kế hoạch đến sau khi A1 hoàn tất.

## Còn mở

1. **A-09:** thực hiện sau khi A1 hoàn tất; trước khi đo cần thống nhất profile và tiêu chí với A1.
2. **A-02 / `DUPLICATE`:** Attendance trả `DUPLICATE` khi attempt đã tồn tại, nhưng không cho biết kết quả gốc là lượt được tính, bị từ chối hay anomaly. Xem [IngestOfflineBatchUseCase.java](../../services/attendance/src/main/java/vn/tmc/attendance/application/usecase/IngestOfflineBatchUseCase.java) và [internal-attendance.openapi.yaml](../api/internal-attendance.openapi.yaml). Cần Team B xác nhận/bổ sung kết quả canonical; sau đó A2 tiêu thụ kết quả và test retry sau crash.
3. **I-1:** kiểm tra Gate Station gọi `last-direction` thật khi Redis mất `last`.

**Tin nhắn gợi ý cho A1:** “A2 đã chạy lại 89 test và `spotlessCheck` pass. A-01–A-06 có implementation/test. Nhờ A1 review hunk `kioskSupport` trong `GateStationConfig.java` và dependency simulator trong `build.gradle.kts`; A2 sẽ phối hợp A1 làm A-09 sau khi A1 hoàn tất. `DUPLICATE` cần phối hợp Team B vì thuộc hợp đồng Attendance.”
