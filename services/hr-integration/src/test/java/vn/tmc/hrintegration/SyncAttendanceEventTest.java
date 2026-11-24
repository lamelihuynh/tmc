package vn.tmc.hrintegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vn.tmc.hrintegration.adapter.out.inmem.InMemorySyncLogRepository;
import vn.tmc.hrintegration.adapter.out.inmem.SimulatedHrPayrollAdapter;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.LookupResult;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.SubmitResult;
import vn.tmc.hrintegration.application.usecase.SyncAttendanceEventUseCase;
import vn.tmc.hrintegration.application.usecase.SyncMonitoringService;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;

class SyncAttendanceEventTest {
  private final InMemorySyncLogRepository log = new InMemorySyncLogRepository();
  private final SimulatedHrPayrollAdapter hr = new SimulatedHrPayrollAdapter();
  private final SyncAttendanceEventUseCase sync =
      new SyncAttendanceEventUseCase(log, hr, IClock.system());

  private EventEnvelope confirmed(UUID eventId) {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AttendanceConfirmed",
        1,
        eventId,
        1,
        Instant.now(),
        "c",
        "attendance-edge",
        "BD",
        "BD-GATE-01",
        Map.of(
            "employeeUuid", UUID.randomUUID().toString(),
            "direction", "ENTRY",
            "scannedAt", Instant.now().toString()));
  }

  @Test
  void should_mark_synced_when_hr_accepts() {
    var id = Ids.uuidV7();

    sync.handle(confirmed(id));

    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);
    assertThat(hr.received()).hasSize(1);
    assertThat(hr.received().get(0).eventId()).isEqualTo(id);
  }

  @Test
  void should_not_send_a_settled_event_again() {
    var id = Ids.uuidV7();
    sync.handle(confirmed(id));

    sync.handle(confirmed(id));

    assertThat(hr.received()).hasSize(1);
  }

  @Test
  void should_mark_failed_and_not_retry_blindly_when_hr_rejects() {
    var id = Ids.uuidV7();
    hr.respondWith(SubmitResult.REJECTED);

    sync.handle(confirmed(id));
    sync.handle(confirmed(id));

    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.FAILED);
    assertThat(hr.received()).hasSize(1);
  }

  @Test
  void should_mark_unknown_not_failed_when_hr_does_not_answer() {
    var id = Ids.uuidV7();
    hr.respondWith(SubmitResult.TIMEOUT);

    sync.handle(confirmed(id));

    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.UNKNOWN);
  }

  @Test
  void should_ignore_events_that_are_not_payroll_data() {
    sync.handle(
        new EventEnvelope(
            Ids.uuidV7(),
            "GateResultRecorded",
            1,
            Ids.uuidV7(),
            1,
            Instant.now(),
            "c",
            "s",
            "BD",
            "G",
            Map.of()));

    assertThat(hr.received()).isEmpty();
  }

  @Test
  void should_let_only_a_system_admin_watch_the_sync_state() {
    var monitoring = new SyncMonitoringService(log);
    var admin = new Principal(UUID.randomUUID(), Set.of(Role.SYSTEM_ADMIN), DataScope.all());
    var hrStaff = new Principal(UUID.randomUUID(), Set.of(Role.HR_STAFF), DataScope.all());

    assertThat(monitoring.byStatus(admin, SyncStatus.SYNCED)).isEmpty();
    assertThatThrownBy(() -> monitoring.byStatus(hrStaff, SyncStatus.SYNCED))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void should_look_up_the_result_before_resending_an_unknown_event() {
    var event = confirmed(Ids.uuidV7());
    hr.respondWith(SubmitResult.TIMEOUT);
    sync.handle(event);

    assertThat(log.find(event.eventId()).orElseThrow().status()).isEqualTo(SyncStatus.UNKNOWN);
    assertThat(hr.received()).hasSize(1);

    hr.respondToLookupWith(LookupResult.ACCEPTED);
    sync.handle(event);

    assertThat(hr.received()).hasSize(1);
    assertThat(log.find(event.eventId()).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);
  }

  @Test
  void should_resend_with_the_same_event_id_when_hr_confirms_not_found() {
    var event = confirmed(Ids.uuidV7());
    hr.respondWith(SubmitResult.TIMEOUT);
    sync.handle(event);

    assertThat(log.find(event.eventId()).orElseThrow().status()).isEqualTo(SyncStatus.UNKNOWN);
    assertThat(hr.received()).hasSize(1);

    hr.respondToLookupWith(LookupResult.NOT_FOUND);
    hr.respondWith(SubmitResult.ACCEPTED);
    sync.handle(event);

    assertThat(hr.received()).hasSize(2);
    assertThat(hr.received().get(0).eventId()).isEqualTo(event.eventId());
    assertThat(hr.received().get(1).eventId()).isEqualTo(event.eventId());
    assertThat(log.find(event.eventId()).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);
  }

  @Test
  void should_keep_unknown_without_resending_when_lookup_is_inconclusive() {
    var event = confirmed(Ids.uuidV7());
    hr.respondWith(SubmitResult.TIMEOUT);
    sync.handle(event);

    hr.respondToLookupWith(LookupResult.UNKNOWN);
    sync.handle(event);

    assertThat(hr.received()).hasSize(1);
    assertThat(log.find(event.eventId()).orElseThrow().status()).isEqualTo(SyncStatus.UNKNOWN);
  }

  @Test
  void should_send_a_correction_once_after_the_original_event_was_synced() {
    var original = confirmed(Ids.uuidV7());
    var eventId = original.eventId();
    var correctionId = Ids.uuidV7();
    var correctedAt = Instant.parse("2026-11-24T10:00:00Z");

    sync.handle(original);
    assertThat(log.find(eventId).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);

    var correction =
        new EventEnvelope(
            Ids.uuidV7(),
            "AttendanceCorrected",
            1,
            eventId,
            2,
            correctedAt.plusSeconds(60),
            "c",
            "attendance-edge",
            "BD",
            "BD-GATE-01",
            Map.of(
                "status", "CORRECTED",
                "correctionId", correctionId.toString(),
                "originalAttemptId", eventId.toString(),
                "revision", 1,
                "employeeUuid", original.payload().get("employeeUuid"),
                "direction", "EXIT",
                "scannedAt", correctedAt.toString(),
                "previousDirection", "ENTRY",
                "previousScannedAt", original.payload().get("scannedAt")));

    sync.handle(correction);
    sync.handle(correction);

    assertThat(hr.received()).hasSize(2);
    var sent = hr.received().get(1);
    assertThat(sent.eventId()).isEqualTo(eventId);
    assertThat(sent.batchId()).isEqualTo(correctionId);
    assertThat(sent.direction()).isEqualTo("EXIT");
    assertThat(sent.scannedAt()).isEqualTo(correctedAt);
  }

  @Disabled(
      "TODO Team D (NFR-08): reconcile the period against Attendance via IAttendanceQuery and build the batch")
  @Test
  void should_send_a_correction_batch_for_missing_or_failed_records() {}

  @Disabled(
      "TODO Team D (NFR-08, AC-07): alert when records are still unconfirmed at 08:00 on the 1st")
  @Test
  void should_raise_a_deadline_alert_before_noon_on_the_first_day_of_the_month() {}
}