package vn.tmc.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.tmc.attendance.adapter.out.inmem.InMemoryAttendanceCorrectionRepository;
import vn.tmc.attendance.application.usecase.AttendanceEventFactory;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.AttendanceCorrection;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.Ids;

/**
 * Contract of AttendanceCorrected (docs/api/attendance-events.asyncapi.yaml, CorrectionPayload).
 */
class AttendanceCorrectedEventTest {
  private final Instant now = Instant.parse("2026-11-20T09:00:00Z");
  private final AttendanceEventFactory events = new AttendanceEventFactory(() -> now);
  private final UUID hrUser = UUID.fromString("0192a1c0-0000-7000-8000-00000000a003");
  private final AttendanceTransaction original =
      new AttendanceTransaction(
          Ids.uuidV7(),
          UUID.fromString("0192a1c0-0000-7000-8000-000000000001"),
          "BD",
          "BD-GATE-01",
          Direction.ENTRY,
          Instant.parse("2026-11-20T07:20:00Z"),
          AttemptSource.ONLINE);

  @Test
  void should_carry_the_full_corrected_values_and_point_at_the_original() {
    var c =
        AttendanceCorrection.first(
            Ids.uuidV7(),
            original,
            Direction.ENTRY,
            Instant.parse("2026-11-20T06:55:00Z"),
            "MISSED_SCAN_CONFIRMED_BY_SUPERVISOR",
            hrUser,
            now);

    var e = events.attendanceCorrected(original, 5, c, "corr-hr-1");

    assertThat(e.eventType()).isEqualTo("AttendanceCorrected");
    assertThat(e.eventId()).isEqualTo(original.attemptId());
    assertThat(e.aggregateVersion()).isEqualTo(6);
    assertThat(e.factoryCode()).isEqualTo("BD");
    assertThat(e.gateId()).isEqualTo("BD-GATE-01");
    assertThat(e.payload())
        .containsEntry("status", "CORRECTED")
        .containsEntry("correctionId", c.correctionId().toString())
        .containsEntry("originalAttemptId", original.attemptId().toString())
        .containsEntry("revision", 1)
        .containsEntry("employeeUuid", original.employeeUuid().toString())
        .containsEntry("direction", "ENTRY")
        .containsEntry("scannedAt", "2026-11-20T06:55:00Z")
        .containsEntry("previousDirection", "ENTRY")
        .containsEntry("previousScannedAt", "2026-11-20T07:20:00Z")
        .containsEntry("reason", "MISSED_SCAN_CONFIRMED_BY_SUPERVISOR")
        .containsEntry("correctedBy", hrUser.toString())
        .containsEntry("source", "ONLINE");
  }

  @Test
  void should_give_a_later_revision_a_higher_aggregate_version() {
    var first =
        AttendanceCorrection.first(
            Ids.uuidV7(),
            original,
            Direction.EXIT,
            original.occurredAt(),
            "WRONG_DIRECTION",
            hrUser,
            now);
    var second =
        AttendanceCorrection.of(
            Ids.uuidV7(),
            original,
            2,
            first.direction(),
            first.scannedAt(),
            Direction.ENTRY,
            original.occurredAt(),
            "CORRECTION_REVERTED",
            hrUser,
            now);

    long v1 = events.attendanceCorrected(original, 5, first, "c").aggregateVersion();
    long v2 = events.attendanceCorrected(original, 5, second, "c").aggregateVersion();

    assertThat(v1).isGreaterThan(5);
    assertThat(v2).isGreaterThan(v1);
    assertThat(second.previousDirection()).isEqualTo(Direction.EXIT);
  }

  @Test
  void should_refuse_free_text_reasons_and_revision_zero() {
    assertThatThrownBy(
            () ->
                AttendanceCorrection.first(
                    Ids.uuidV7(),
                    original,
                    Direction.EXIT,
                    now,
                    "Nguyen Van A forgot",
                    hrUser,
                    now))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                AttendanceCorrection.of(
                    Ids.uuidV7(),
                    original,
                    0,
                    Direction.ENTRY,
                    now,
                    Direction.EXIT,
                    now,
                    "WRONG_DIRECTION",
                    hrUser,
                    now))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_refuse_a_correction_of_another_transaction() {
    var other =
        new AttendanceTransaction(
            Ids.uuidV7(),
            original.employeeUuid(),
            "BD",
            "BD-GATE-01",
            Direction.EXIT,
            now,
            AttemptSource.ONLINE);
    var c =
        AttendanceCorrection.first(
            Ids.uuidV7(), other, Direction.ENTRY, now, "WRONG_DIRECTION", hrUser, now);

    assertThatThrownBy(() -> events.attendanceCorrected(original, 1, c, "c"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_keep_one_row_per_revision_in_the_in_memory_store() {
    var repo = new InMemoryAttendanceCorrectionRepository();
    var c =
        AttendanceCorrection.first(
            Ids.uuidV7(), original, Direction.EXIT, now, "WRONG_DIRECTION", hrUser, now);
    var sameRevision =
        AttendanceCorrection.first(
            Ids.uuidV7(), original, Direction.ENTRY, now, "WRONG_DIRECTION", hrUser, now);

    assertThat(repo.add(c)).isTrue();
    assertThat(repo.add(sameRevision)).isFalse();
    assertThat(repo.latest(original.attemptId())).contains(c);
  }
}
