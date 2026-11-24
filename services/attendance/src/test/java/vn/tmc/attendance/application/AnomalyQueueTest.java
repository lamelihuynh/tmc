package vn.tmc.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.attendance.application.port.in.AnomalyView;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.LocalStatus;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.OfflineRecord;
import vn.tmc.attendance.application.usecase.AttendanceQueryService;
import vn.tmc.attendance.domain.Anomaly;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.attendance.support.AttendanceFixture;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

/** B-03: HR anomaly queue query over IngestOfflineBatchUseCase-flagged attempts. */
class AnomalyQueueTest {
  private final AttendanceFixture f = new AttendanceFixture();
  private final AttendanceQueryService query =
      new AttendanceQueryService(f.attempts, f.transactions);

  private static final Principal HR =
      new Principal(UUID.randomUUID(), Set.of(Role.HR_STAFF), DataScope.all());
  private static final Principal GUARD =
      new Principal(UUID.randomUUID(), Set.of(Role.SECURITY_GUARD), DataScope.all());

  @Test
  void should_list_both_anomaly_types_and_exclude_normal_attempts() {
    var t0 = f.clock.now();
    var e2 = UUID.fromString("0192a1c0-0000-7000-8000-000000000002");
    var validEntry = Ids.uuidV7();
    var antiPassback = Ids.uuidV7();
    var reusedToken = Ids.uuidV7();

    f.offline.ingest(
        "BD-KIOSK-01",
        List.of(
            offline(validEntry, 1, AttendanceFixture.EMPLOYEE, "BD", "jti-1", Direction.ENTRY, t0),
            offline(
                antiPassback,
                2,
                AttendanceFixture.EMPLOYEE,
                "BD",
                "jti-2",
                Direction.ENTRY,
                t0.plusSeconds(30)),
            offline(reusedToken, 3, e2, "BD", "jti-1", Direction.ENTRY, t0.plusSeconds(60))));

    var all = query.anomalies(HR, null, null, null, 100);

    assertThat(all)
        .extracting(AnomalyView::attemptId, AnomalyView::anomaly)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(antiPassback, Anomaly.ANTI_PASSBACK_OFFLINE),
            org.assertj.core.groups.Tuple.tuple(reusedToken, Anomaly.TOKEN_REUSED_OFFLINE));
    assertThat(all).extracting(AnomalyView::attemptId).doesNotContain(validEntry);
  }

  @Test
  void should_filter_anomalies_by_factory_and_time_range() {
    var t1 = Instant.parse("2026-11-20T07:00:00Z");
    var t2 = Instant.parse("2026-11-20T09:00:00Z");
    var t3 = Instant.parse("2026-11-20T11:00:00Z");
    var e2 = UUID.fromString("0192a1c0-0000-7000-8000-000000000002");
    var e3 = UUID.fromString("0192a1c0-0000-7000-8000-000000000003");

    var seedBd = Ids.uuidV7();
    var bdEarly = Ids.uuidV7();
    var bdMid = Ids.uuidV7();
    var laMid = Ids.uuidV7();

    f.offline.ingest(
        "BD-KIOSK-01",
        List.of(
            offline(seedBd, 1, AttendanceFixture.EMPLOYEE, "BD", "jti-seed", Direction.ENTRY, t1),
            offline(
                bdEarly,
                2,
                AttendanceFixture.EMPLOYEE,
                "BD",
                "jti-bd-1",
                Direction.ENTRY,
                t1.plusSeconds(10)),
            offline(bdMid, 3, e2, "BD", "jti-seed", Direction.ENTRY, t2)));
    f.offline.ingest(
        "LA-KIOSK-01", List.of(offline(laMid, 1, e3, "LA", "jti-seed", Direction.ENTRY, t3)));

    assertThat(query.anomalies(HR, "BD", null, null, 100))
        .extracting(AnomalyView::attemptId)
        .containsExactly(bdEarly, bdMid);

    assertThat(query.anomalies(HR, "LA", null, null, 100))
        .extracting(AnomalyView::attemptId)
        .containsExactly(laMid);

    assertThat(query.anomalies(HR, "BD", t2, t3, 100))
        .extracting(AnomalyView::attemptId)
        .containsExactly(bdMid);
  }

  @Test
  void should_reject_non_hr_callers() {
    assertThatThrownBy(() -> query.anomalies(GUARD, null, null, null, 100))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> query.anomalies(null, null, null, null, 100))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static OfflineRecord offline(
      UUID attemptId,
      long seq,
      UUID employee,
      String factory,
      String jti,
      Direction dir,
      Instant at) {
    return new OfflineRecord(
        attemptId,
        seq,
        employee,
        factory + "-GATE-01",
        factory,
        dir,
        jti,
        at,
        LocalStatus.OFFLINE_OPENED,
        null);
  }
}
