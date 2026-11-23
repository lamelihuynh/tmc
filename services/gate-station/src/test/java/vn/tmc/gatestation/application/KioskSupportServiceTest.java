package vn.tmc.gatestation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.tmc.gatestation.adapter.out.inmem.FakeAttendanceGateway;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryHeartbeatStore;
import vn.tmc.gatestation.adapter.out.inmem.StaticKioskSyncSource;
import vn.tmc.gatestation.application.port.in.IGateAccess.Outcome;
import vn.tmc.gatestation.application.port.in.IKioskSupport.OfflineEntry;
import vn.tmc.gatestation.application.port.in.IKioskSupport.OfflineEntryResult;
import vn.tmc.gatestation.application.port.in.IKioskSupport.OfflineOutcome;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.OfflineItem;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.OfflineItemResult;
import vn.tmc.gatestation.application.usecase.KioskSupportService;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.LastPass;
import vn.tmc.gatestation.support.GateFixture;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.kernel.ReasonCode;

class KioskSupportServiceTest {
  private final GateFixture f = new GateFixture();
  private final KioskSupportService support =
      new KioskSupportService(
          f.gates,
          new StaticKioskSyncSource(),
          new InMemoryHeartbeatStore(),
          f.attendance,
          f.antiPassback,
          f.clock);

  @Test
  void should_block_an_online_entry_after_an_accepted_offline_entry() {
    var at = f.clock.now().minusSeconds(1);
    var offline = entry(at, "OFFLINE_OPENED", null);

    var results = support.ingestOffline("BD-KIOSK-01", List.of(offline));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().outcome()).isEqualTo(OfflineOutcome.ACCEPTED);
    assertThat(f.antiPassback.last(GateFixture.E1)).contains(new LastPass(Direction.ENTRY, at));

    f.token("online", GateFixture.E1, "online-jti");
    var online = f.scan.scan(f.request("BD-GATE-01", "online"));
    assertThat(online.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(online.reason()).isEqualTo(ReasonCode.ANTI_PASSBACK_VIOLATION);
    assertThat(f.turnstile.openCalls()).isZero();
  }

  @Test
  void should_keep_the_newer_pass_when_an_older_offline_entry_is_accepted() {
    Instant recent = f.clock.now().minusSeconds(1);
    f.antiPassback.rememberIfNewer(GateFixture.E1, new LastPass(Direction.EXIT, recent));

    support.ingestOffline(
        "BD-KIOSK-01", List.of(entry(recent.minus(Duration.ofSeconds(1)), "OFFLINE_OPENED", null)));

    assertThat(f.antiPassback.last(GateFixture.E1)).contains(new LastPass(Direction.EXIT, recent));
  }

  @Test
  void should_keep_the_newest_pass_when_a_batch_is_returned_out_of_timestamp_order() {
    var olderAt = f.clock.now().minusSeconds(10);
    var older = entry(olderAt, Direction.ENTRY, "OFFLINE_OPENED", null);
    var newer = entry(olderAt.plusSeconds(5), Direction.EXIT, "OFFLINE_OPENED", null);

    support.ingestOffline("BD-KIOSK-01", List.of(newer, older));

    assertThat(f.antiPassback.last(GateFixture.E1))
        .contains(new LastPass(Direction.EXIT, newer.scannedAt()));
  }

  @Test
  void should_not_update_anti_passback_for_an_accepted_offline_denial() {
    var results =
        support.ingestOffline(
            "BD-KIOSK-01",
            List.of(entry(f.clock.now().minusSeconds(1), "OFFLINE_DENIED", ReasonCode.QR_EXPIRED)));

    assertThat(results.getFirst().outcome()).isEqualTo(OfflineOutcome.ACCEPTED);
    assertThat(f.antiPassback.last(GateFixture.E1)).isEmpty();
  }

  @Test
  void should_not_update_antipassback_when_attendance_marks_an_opened_entry_as_anomaly() {
    var rejectingAttendance =
        new FakeAttendanceGateway() {
          @Override
          public List<OfflineItemResult> ingestOffline(String kioskId, List<OfflineItem> items) {
            return items.stream()
                .map(
                    item ->
                        new OfflineItemResult(
                            item.attemptId(),
                            IAttendanceGateway.OfflineOutcome.ANOMALY,
                            "rejected"))
                .toList();
          }
        };
    var rejectingSupport = support(rejectingAttendance);
    var offline = entry(f.clock.now().minusSeconds(1), "OFFLINE_OPENED", null);

    var results = rejectingSupport.ingestOffline("BD-KIOSK-01", List.of(offline));

    assertThat(results.getFirst().outcome()).isEqualTo(OfflineOutcome.ANOMALY);
    assertThat(f.antiPassback.last(GateFixture.E1)).isEmpty();
  }

  @Test
  void should_not_turn_a_duplicate_attempt_into_an_antipassback_pass() {
    var attemptId = Ids.uuidV7();
    var denied =
        new OfflineEntry(
            attemptId,
            1,
            GateFixture.E1,
            "BD-GATE-01",
            Direction.ENTRY,
            "offline-jti",
            f.clock.now().minusSeconds(2),
            "OFFLINE_DENIED",
            ReasonCode.QR_EXPIRED);
    var duplicateOpened =
        new OfflineEntry(
            attemptId,
            2,
            GateFixture.E1,
            "BD-GATE-01",
            Direction.ENTRY,
            "offline-jti",
            f.clock.now().minusSeconds(1),
            "OFFLINE_OPENED",
            null);

    var results = support.ingestOffline("BD-KIOSK-01", List.of(denied, duplicateOpened));

    assertThat(results)
        .extracting(OfflineEntryResult::outcome)
        .containsExactly(OfflineOutcome.ACCEPTED, OfflineOutcome.DUPLICATE);
    assertThat(f.antiPassback.last(GateFixture.E1)).isEmpty();
  }

  private OfflineEntry entry(Instant at, String status, ReasonCode reason) {
    return entry(at, Direction.ENTRY, status, reason);
  }

  private OfflineEntry entry(Instant at, Direction direction, String status, ReasonCode reason) {
    return new OfflineEntry(
        Ids.uuidV7(),
        1,
        GateFixture.E1,
        "BD-GATE-01",
        direction,
        "offline-jti",
        at,
        status,
        reason);
  }

  private KioskSupportService support(IAttendanceGateway attendance) {
    return new KioskSupportService(
        f.gates,
        new StaticKioskSyncSource(),
        new InMemoryHeartbeatStore(),
        attendance,
        f.antiPassback,
        f.clock);
  }
}
