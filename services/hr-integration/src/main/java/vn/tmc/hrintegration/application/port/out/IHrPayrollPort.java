package vn.tmc.hrintegration.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface IHrPayrollPort {

  record Submission(
      UUID eventId,
      UUID batchId,
      UUID employeeUuid,
      String factoryCode,
      String direction,
      Instant scannedAt) {

    public Submission(
        UUID eventId, UUID employeeUuid, String factoryCode, String direction, Instant scannedAt) {
      this(eventId, eventId, employeeUuid, factoryCode, direction, scannedAt);
    }
  }

  enum SubmitResult {
    ACCEPTED,
    REJECTED,
    TIMEOUT
  }

  enum LookupResult {
    ACCEPTED,
    REJECTED,
    NOT_FOUND,
    UNKNOWN
  }

  LookupResult lookup(UUID batchId);

  SubmitResult submit(Submission submission);
}
