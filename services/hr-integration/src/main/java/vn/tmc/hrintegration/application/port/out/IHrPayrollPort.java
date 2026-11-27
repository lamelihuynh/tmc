package vn.tmc.hrintegration.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Driven port to HR/Payroll (ADR-07, docs/api/hr-payroll.openapi.yaml). */
public interface IHrPayrollPort {

  /**
   * One single-line batch. eventId is the record's event_id at HR (its dedupe key) and equals
   * batchId; correctsEventId names the corrected line, null for an original line.
   */
  record Submission(
      UUID eventId,
      UUID batchId,
      UUID employeeUuid,
      String factoryCode,
      String direction,
      Instant scannedAt,
      UUID correctsEventId) {

    public Submission(
        UUID eventId,
        UUID batchId,
        UUID employeeUuid,
        String factoryCode,
        String direction,
        Instant scannedAt) {
      this(eventId, batchId, employeeUuid, factoryCode, direction, scannedAt, null);
    }

    public Submission(
        UUID eventId, UUID employeeUuid, String factoryCode, String direction, Instant scannedAt) {
      this(eventId, eventId, employeeUuid, factoryCode, direction, scannedAt, null);
    }
  }

  enum SubmitResult {
    /** HR took the batch (2xx other than 207). */
    ACCEPTED,
    /** HR checked the data and refused it (422). Do not resend unchanged. */
    REJECTED,
    /**
     * HR refused the request before processing it (401, 403, 400 ...): it does not have the batch,
     * and the cause is on our side (token, configuration). Safe to resend once fixed.
     */
    REFUSED,
    /** No usable answer (timeout, 5xx, 207, network): HR may or may not have the batch. */
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
