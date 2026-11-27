package vn.tmc.reporting.application.usecase;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.tmc.platform.kernel.EventEnvelope;
import vn.tmc.reporting.application.port.out.IFraudAlertStore;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.domain.FraudAlert;
import vn.tmc.reporting.domain.FraudRule;
import vn.tmc.reporting.domain.ScanRecord;

/**
 * Projects AttendanceConfirmed and AttendanceCorrected into the presence read model (ADR-10).
 * Wrapped by IdempotentConsumer, so a redelivered message is a no-op. Each scan is stored under its
 * attemptId (the event's eventId); a correction carries the same attemptId with a higher
 * aggregateVersion and replaces the stored scan, while a late older version is ignored. A
 * correction of an earlier scan never overrides a later scan, because presence is the latest
 * scannedAt. After each scan the fraud rules (ADR-12 R4) compare it with the employee's latest pass
 * in every other factory; a finding is stored as a FraudAlert for HR and logged, nothing is
 * deleted. TODO Team D: dashboard aggregates (FR-24, FR-25), export (FR-28).
 */
public class ProjectAttendanceEventUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectAttendanceEventUseCase.class);

  private final IPresenceRepository presences;
  private final List<FraudRule> rules;
  private final IFraudAlertStore alerts;

  public ProjectAttendanceEventUseCase(
      IPresenceRepository presences, List<FraudRule> rules, IFraudAlertStore alerts) {
    this.presences = presences;
    this.rules = List.copyOf(rules);
    this.alerts = alerts;
  }

  public void handle(EventEnvelope event) {
    if (!"AttendanceConfirmed".equals(event.eventType())
        && !"AttendanceCorrected".equals(event.eventType())) {
      return; // other event types are not projected
    }
    var scan = toScan(event);
    if (scan == null) {
      // malformed payload: retrying cannot fix it, so skip it instead of filling the DLQ
      LOG.warn(
          "Skipping {} {}: missing or invalid employeeUuid, direction, scannedAt or factoryCode",
          event.eventType(),
          event.messageId());
      return;
    }
    presences.upsertScan(scan);
    checkFraud(scan);
  }

  private void checkFraud(ScanRecord scan) {
    if (rules.isEmpty()) {
      return;
    }
    var current = scan.toPresence();
    for (var other : presences.latestPerFactory(scan.employeeUuid())) {
      if (other.factoryCode().equals(scan.factoryCode())) {
        continue;
      }
      for (var rule : rules) {
        rule.evaluate(other, current)
            .ifPresent(
                finding -> {
                  LOG.warn(
                      "Fraud alert {} for employee {} attempt {}: {}",
                      finding.rule(),
                      scan.employeeUuid(),
                      scan.attemptId(),
                      finding.detail());
                  alerts.record(
                      new FraudAlert(
                          scan.attemptId(),
                          scan.employeeUuid(),
                          scan.factoryCode(),
                          other.factoryCode(),
                          finding.rule(),
                          finding.detail(),
                          scan.scannedAt()));
                });
      }
    }
  }

  private static ScanRecord toScan(EventEnvelope event) {
    var payload = event.payload();
    var employee = payload.get("employeeUuid");
    var direction = payload.get("direction");
    var scannedAt = payload.get("scannedAt");
    if (employee == null
        || direction == null
        || scannedAt == null
        || event.eventId() == null
        || event.factoryCode() == null) {
      return null;
    }
    try {
      return new ScanRecord(
          event.eventId(),
          UUID.fromString(employee.toString()),
          event.factoryCode(),
          direction.toString(),
          Instant.parse(scannedAt.toString()),
          event.aggregateVersion());
    } catch (IllegalArgumentException | DateTimeParseException e) {
      return null;
    }
  }
}
