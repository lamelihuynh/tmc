package vn.tmc.hrintegration.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Driven port: the existing HR/Payroll system (document.md 16.4). The interface speaks our model;
 * the adapter translates to whatever HR/Payroll expects (anti-corruption layer).
 */
public interface IHrPayrollPort {

  /** eventId is the idempotency key HR/Payroll dedupes on (ADR-05). */
  record Submission(
      UUID eventId, UUID employeeUuid, String factoryCode, String direction, Instant scannedAt) {}

  enum SubmitResult {
    ACCEPTED,
    /** HR/Payroll refused the record; resending the same data cannot help. */
    REJECTED,
    /** No answer in time. HR/Payroll may or may not have stored it. */
    TIMEOUT
  }

  SubmitResult submit(Submission submission);
}
