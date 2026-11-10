package vn.tmc.reporting.application.usecase;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.domain.Presence;

/**
 * Updates the presence read model from AttendanceConfirmed (ADR-10). Wrapped by IdempotentConsumer
 * so a redelivered message is a no-op; the version check also protects against out-of-order
 * delivery. TODO Team D: AttendanceCorrected, dashboard aggregates (FR-24, FR-25), export (FR-28),
 * fraud rules.
 */
public class ProjectAttendanceEventUseCase {
  private final IPresenceRepository presences;

  public ProjectAttendanceEventUseCase(IPresenceRepository presences) {
    this.presences = presences;
  }

  public void handle(EventEnvelope event) {
    if (!"AttendanceConfirmed".equals(event.eventType())) {
      return; // other event types are not projected yet
    }
    var employee = event.payload().get("employeeUuid");
    var direction = event.payload().get("direction");
    if (employee == null || direction == null) {
      return; // malformed payload: ignore rather than poison the queue
    }
    var candidate =
        new Presence(
            UUID.fromString(employee.toString()),
            event.factoryCode(),
            direction.toString(),
            Instant.parse(event.payload().get("scannedAt").toString()),
            event.aggregateVersion());
    var current = presences.find(candidate.employeeUuid(), candidate.factoryCode());
    if (current.isEmpty() || current.get().supersededBy(candidate)) {
      presences.save(candidate);
    }
  }
}
