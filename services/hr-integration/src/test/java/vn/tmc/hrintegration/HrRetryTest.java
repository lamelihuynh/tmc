package vn.tmc.hrintegration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vn.tmc.hrintegration.adapter.out.inmem.InMemorySyncLogRepository;
import vn.tmc.hrintegration.adapter.out.inmem.SimulatedHrPayrollAdapter;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.LookupResult;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort.SubmitResult;
import vn.tmc.hrintegration.application.usecase.SyncAttendanceEventUseCase;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;

/** Retries that do not wait for a redelivery: the reconcile job (SyncAttendanceEventUseCase). */
class HrRetryTest {
  private final AtomicReference<Instant> now =
      new AtomicReference<>(Instant.parse("2026-11-27T02:00:00Z"));
  private final InMemorySyncLogRepository log = new InMemorySyncLogRepository();
  private final SimulatedHrPayrollAdapter hr = new SimulatedHrPayrollAdapter();
  private final SyncAttendanceEventUseCase sync = new SyncAttendanceEventUseCase(log, hr, now::get);

  private EventEnvelope confirmed(UUID attemptId) {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AttendanceConfirmed",
        1,
        attemptId,
        5,
        now.get(),
        "c",
        "attendance-edge",
        "BD",
        "BD-GATE-01",
        Map.of(
            "employeeUuid", UUID.randomUUID().toString(),
            "direction", "ENTRY",
            "scannedAt", "2026-11-27T01:00:00Z"));
  }

  private void advance(Duration d) {
    now.set(now.get().plus(d));
  }

  @Test
  void should_resend_an_unknown_batch_later_only_after_hr_says_it_does_not_have_it() {
    var id = Ids.uuidV7();
    hr.respondWith(SubmitResult.TIMEOUT);
    sync.handle(confirmed(id));
    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.UNKNOWN);

    assertThat(sync.retryDue(10)).isZero(); // backoff: not due yet
    hr.respondWith(SubmitResult.ACCEPTED);
    advance(Duration.ofMinutes(2));

    assertThat(sync.retryDue(10)).isEqualTo(1); // lookup says NOT_FOUND, so it is resent
    assertThat(hr.received()).hasSize(2);
    assertThat(hr.received().get(1).batchId()).isEqualTo(id);
    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);
  }

  @Test
  void should_settle_an_unknown_batch_from_the_lookup_without_resending() {
    var id = Ids.uuidV7();
    hr.respondWith(SubmitResult.TIMEOUT);
    sync.handle(confirmed(id));
    hr.respondToLookupWith(LookupResult.ACCEPTED);
    advance(Duration.ofMinutes(2));

    sync.retryDue(10);

    assertThat(hr.received()).hasSize(1);
    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);
  }

  @Test
  void should_keep_retrying_a_refused_batch_with_growing_delay_until_hr_takes_it() {
    var id = Ids.uuidV7();
    hr.respondWith(SubmitResult.REFUSED); // e.g. a wrong HR_PAYROLL_API_TOKEN
    sync.handle(confirmed(id));
    var first = log.find(id).orElseThrow();
    assertThat(first.status()).isEqualTo(SyncStatus.RETRY_PENDING);
    assertThat(first.lastError()).contains("HR_PAYROLL_API_TOKEN");

    advance(Duration.ofMinutes(1));
    sync.retryDue(10);
    var second = log.find(id).orElseThrow();
    assertThat(hr.received()).hasSize(2);
    assertThat(Duration.between(now.get(), second.nextAttemptAt()))
        .isGreaterThan(Duration.between(first.updatedAt(), first.nextAttemptAt()));

    hr.respondWith(SubmitResult.ACCEPTED); // token fixed
    now.set(second.nextAttemptAt());
    sync.retryDue(10);

    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);
    assertThat(hr.received()).extracting(s -> s.batchId()).containsOnly(id);
  }

  @Test
  void should_look_up_a_batch_left_in_progress_by_a_stopped_process() {
    var id = Ids.uuidV7();
    sync.handle(confirmed(id));
    var synced = log.find(id).orElseThrow();
    // what a crash between "IN_PROGRESS saved" and "answer saved" leaves behind
    log.save(synced.with(SyncStatus.IN_PROGRESS, null, now.get()));

    advance(Duration.ofMinutes(1));
    assertThat(sync.retryDue(10)).isZero(); // could still be running
    advance(SyncAttendanceEventUseCase.STUCK_AFTER);
    sync.retryDue(10);

    assertThat(hr.received()).hasSize(1); // HR had it: no second send
    assertThat(log.find(id).orElseThrow().status()).isEqualTo(SyncStatus.SYNCED);
  }

  @Test
  void should_send_a_correction_even_while_the_original_is_unknown() {
    var original = confirmed(Ids.uuidV7());
    hr.respondWith(SubmitResult.TIMEOUT);
    sync.handle(original);
    hr.respondWith(SubmitResult.ACCEPTED);
    hr.respondToLookupWith(LookupResult.UNKNOWN);
    var correctionId = Ids.uuidV7();

    sync.handle(
        new EventEnvelope(
            Ids.uuidV7(),
            "AttendanceCorrected",
            1,
            original.eventId(),
            6,
            now.get(),
            "c",
            "attendance-edge",
            "BD",
            "BD-GATE-01",
            Map.of(
                "status",
                "CORRECTED",
                "correctionId",
                correctionId.toString(),
                "originalAttemptId",
                original.eventId().toString(),
                "revision",
                1,
                "employeeUuid",
                original.payload().get("employeeUuid"),
                "direction",
                "EXIT",
                "scannedAt",
                "2026-11-27T01:00:00Z")));

    var row = log.find(original.eventId()).orElseThrow();
    assertThat(row.status()).isEqualTo(SyncStatus.SYNCED);
    assertThat(row.batchId()).isEqualTo(correctionId);
    assertThat(hr.received().get(1).correctsEventId()).isEqualTo(original.eventId());
  }
}
