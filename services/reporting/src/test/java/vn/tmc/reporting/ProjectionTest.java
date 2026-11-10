package vn.tmc.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.InMemoryProcessedMessageStore;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.reporting.adapter.out.inmem.InMemoryPresenceRepository;
import vn.tmc.reporting.application.usecase.ProjectAttendanceEventUseCase;
import vn.tmc.reporting.application.usecase.ReportingQueryService;

class ProjectionTest {
  private final InMemoryPresenceRepository repo = new InMemoryPresenceRepository();
  private final ProjectAttendanceEventUseCase projection = new ProjectAttendanceEventUseCase(repo);
  private final UUID employee = UUID.randomUUID();

  private EventEnvelope confirmed(String direction, long version, String at) {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AttendanceConfirmed",
        1,
        Ids.uuidV7(),
        version,
        Instant.parse(at),
        "c",
        "attendance-edge",
        "BD",
        "BD-GATE-01",
        Map.of("employeeUuid", employee.toString(), "direction", direction, "scannedAt", at));
  }

  @Test
  void should_mark_the_employee_inside_after_an_entry_and_outside_after_an_exit() {
    projection.handle(confirmed("ENTRY", 1, "2026-11-09T06:45:00Z"));
    var supervisor =
        new Principal(
            UUID.randomUUID(),
            Set.of(Role.PRODUCTION_SUPERVISOR),
            new DataScope(Set.of("BD"), Set.of(), Set.of(), false));
    var query = new ReportingQueryService(repo);
    assertThat(query.whoIsInside(supervisor, "BD")).hasSize(1);

    projection.handle(confirmed("EXIT", 2, "2026-11-09T14:45:00Z"));

    assertThat(query.whoIsInside(supervisor, "BD")).isEmpty();
  }

  @Test
  void should_ignore_an_older_message_that_arrives_late() {
    projection.handle(confirmed("EXIT", 2, "2026-11-09T14:45:00Z"));
    projection.handle(confirmed("ENTRY", 1, "2026-11-09T06:45:00Z")); // out of order

    assertThat(repo.find(employee, "BD").orElseThrow().direction()).isEqualTo("EXIT");
  }

  @Test
  void should_process_a_redelivered_message_only_once() {
    var calls = new int[1];
    var consumer =
        new IdempotentConsumer(
            "reporting",
            new InMemoryProcessedMessageStore(),
            e -> {
              calls[0]++;
              projection.handle(e);
            });
    var msg = confirmed("ENTRY", 1, "2026-11-09T06:45:00Z");

    consumer.accept(msg);
    consumer.accept(msg);

    assertThat(calls[0]).isEqualTo(1);
  }

  @Test
  void should_ignore_other_event_types_and_malformed_payloads() {
    projection.handle(
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
    projection.handle(
        new EventEnvelope(
            Ids.uuidV7(),
            "AttendanceConfirmed",
            1,
            Ids.uuidV7(),
            1,
            Instant.now(),
            "c",
            "s",
            "BD",
            "G",
            Map.of()));

    assertThat(repo.byFactory("BD")).isEmpty();
  }

  @Test
  void should_forbid_reading_another_factory_or_with_the_wrong_role() {
    var query = new ReportingQueryService(repo);
    var bdSupervisor =
        new Principal(
            UUID.randomUUID(),
            Set.of(Role.PRODUCTION_SUPERVISOR),
            new DataScope(Set.of("BD"), Set.of(), Set.of(), false));
    var guard = new Principal(UUID.randomUUID(), Set.of(Role.SECURITY_GUARD), DataScope.all());

    assertThatThrownBy(() -> query.whoIsInside(bdSupervisor, "LA"))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> query.whoIsInside(guard, "BD"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Disabled(
      "TODO Team D (ADR-12 test 8): ImpossibleTravelRule flags BD then LA within the travel window")
  @Test
  void should_flag_two_entries_in_different_factories_ten_minutes_apart() {}

  @Disabled(
      "TODO Team D (FR-24, FR-28): attendance report per group/shift/factory and role-limited export")
  @Test
  void should_build_a_group_attendance_report_within_the_role_scope() {}

  @Disabled(
      "TODO Team D (document.md 12.6): AttendanceCorrected supersedes the original in the read model")
  @Test
  void should_apply_a_correction_event_on_top_of_the_original() {}
}
