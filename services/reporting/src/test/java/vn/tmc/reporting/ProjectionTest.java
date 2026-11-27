package vn.tmc.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
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
import vn.tmc.reporting.adapter.out.inmem.InMemoryFraudAlertStore;
import vn.tmc.reporting.adapter.out.inmem.InMemoryPresenceRepository;
import vn.tmc.reporting.application.usecase.ProjectAttendanceEventUseCase;
import vn.tmc.reporting.application.usecase.ReportingQueryService;
import vn.tmc.reporting.domain.ImpossibleTravelRule;

class ProjectionTest {
  private final InMemoryPresenceRepository repo = new InMemoryPresenceRepository();
  private final InMemoryFraudAlertStore alerts = new InMemoryFraudAlertStore();
  private final ProjectAttendanceEventUseCase projection =
      new ProjectAttendanceEventUseCase(
          repo, List.of(new ImpossibleTravelRule(ImpossibleTravelRule.DEFAULT_WINDOW)), alerts);
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
    var query = new ReportingQueryService(repo, alerts);
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
    var query = new ReportingQueryService(repo, alerts);
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

  private EventEnvelope entryAt(String factory, String scannedAt) {
    return new EventEnvelope(
        Ids.uuidV7(),
        "AttendanceConfirmed",
        1,
        Ids.uuidV7(),
        5,
        Instant.parse(scannedAt),
        "c",
        "attendance-edge",
        factory,
        factory + "-GATE-01",
        Map.of("employeeUuid", employee.toString(), "direction", "ENTRY", "scannedAt", scannedAt));
  }

  @Test
  void should_flag_two_entries_in_different_factories_ten_minutes_apart() {
    projection.handle(entryAt("BD", "2026-11-09T06:45:00Z"));
    var la = entryAt("LA", "2026-11-09T06:55:00Z");
    projection.handle(la);

    var hr =
        new Principal(
            UUID.randomUUID(),
            Set.of(Role.HR_STAFF),
            new DataScope(Set.of("LA"), Set.of(), Set.of(), false));
    var found = new ReportingQueryService(repo, alerts).fraudAlerts(hr, "LA");

    assertThat(found)
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.rule()).isEqualTo(ImpossibleTravelRule.NAME);
              assertThat(a.attemptId()).isEqualTo(la.eventId());
              assertThat(a.factoryCode()).isEqualTo("LA");
              assertThat(a.otherFactory()).isEqualTo("BD");
              assertThat(a.detail()).contains("10 min");
            });
    // flagged for review, not removed: the pass still counts in the read model
    assertThat(repo.find(employee, "LA")).isPresent();
    // the alert is visible from the other factory too, and only once on redelivery
    projection.handle(la);
    assertThat(alerts.involving("BD", 10)).hasSize(1);
  }

  @Test
  void should_not_flag_entries_in_two_factories_further_apart_than_the_travel_time() {
    projection.handle(entryAt("BD", "2026-11-09T06:45:00Z"));
    projection.handle(entryAt("LA", "2026-11-09T08:45:00Z"));
    projection.handle(entryAt("BD", "2026-11-09T06:50:00Z")); // same factory: never a finding

    assertThat(alerts.involving("LA", 10)).isEmpty();
    assertThat(alerts.involving("BD", 10)).isEmpty();
  }

  @Disabled(
      "TODO Team D (FR-24, FR-28): attendance report per group/shift/factory and role-limited export")
  @Test
  void should_build_a_group_attendance_report_within_the_role_scope() {}

  @Test
  void should_apply_a_correction_event_on_top_of_the_original() {
    var original = confirmed("ENTRY", 5, "2026-11-09T06:45:00Z");
    projection.handle(original);
    // HR: that pass was actually an exit (revision 1, aggregateVersion = 5 + 1)
    var correction =
        new EventEnvelope(
            Ids.uuidV7(),
            "AttendanceCorrected",
            1,
            original.eventId(),
            6,
            Instant.parse("2026-11-09T09:00:00Z"),
            "c",
            "attendance-edge",
            "BD",
            "BD-GATE-01",
            Map.of(
                "status",
                "CORRECTED",
                "originalAttemptId",
                original.eventId().toString(),
                "revision",
                1,
                "employeeUuid",
                employee.toString(),
                "direction",
                "EXIT",
                "scannedAt",
                "2026-11-09T06:45:00Z"));

    projection.handle(correction);
    projection.handle(original); // a late redelivery of the original must not undo it

    assertThat(repo.find(employee, "BD").orElseThrow().direction()).isEqualTo("EXIT");
  }

  private EventEnvelope scan(
      String type, UUID attemptId, String direction, long version, String scannedAt) {
    return new EventEnvelope(
        Ids.uuidV7(),
        type,
        1,
        attemptId,
        version,
        Instant.parse(scannedAt),
        "c",
        "attendance-edge",
        "BD",
        "BD-GATE-01",
        Map.of(
            "employeeUuid", employee.toString(), "direction", direction, "scannedAt", scannedAt));
  }

  @Test
  void should_not_let_a_correction_of_an_earlier_scan_override_a_later_scan() {
    var morning = Ids.uuidV7();
    // a manually verified morning entry ends with a higher version than an online afternoon exit
    projection.handle(scan("AttendanceConfirmed", morning, "ENTRY", 6, "2026-11-09T06:45:00Z"));
    projection.handle(scan("AttendanceConfirmed", Ids.uuidV7(), "EXIT", 5, "2026-11-09T14:45:00Z"));
    assertThat(repo.find(employee, "BD").orElseThrow().direction()).isEqualTo("EXIT");

    // HR corrects the morning scan (still ENTRY, a few minutes earlier): version 7
    projection.handle(scan("AttendanceCorrected", morning, "ENTRY", 7, "2026-11-09T06:40:00Z"));

    var now = repo.find(employee, "BD").orElseThrow();
    assertThat(now.direction()).isEqualTo("EXIT");
    assertThat(now.at()).isEqualTo(Instant.parse("2026-11-09T14:45:00Z"));
  }

  @Test
  void should_ignore_an_older_version_of_the_same_scan_that_arrives_late() {
    var attempt = Ids.uuidV7();
    projection.handle(scan("AttendanceCorrected", attempt, "EXIT", 6, "2026-11-09T06:45:00Z"));
    projection.handle(scan("AttendanceConfirmed", attempt, "ENTRY", 5, "2026-11-09T06:45:00Z"));

    assertThat(repo.find(employee, "BD").orElseThrow().direction()).isEqualTo("EXIT");
  }

  @Test
  void should_skip_a_scan_with_an_unreadable_time_instead_of_failing_the_message() {
    projection.handle(
        new EventEnvelope(
            Ids.uuidV7(),
            "AttendanceConfirmed",
            1,
            Ids.uuidV7(),
            1,
            Instant.now(),
            "c",
            "attendance-edge",
            "BD",
            "BD-GATE-01",
            Map.of("employeeUuid", employee.toString(), "direction", "ENTRY")));

    assertThat(repo.byFactory("BD")).isEmpty();
  }
}
