package vn.tmc.hrintegration.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The attendance line sent to HR/Payroll for one attempt, kept in the sync log so a scheduled retry
 * can resend it without the original event. correctsEventId is set when this line corrects an
 * earlier one (AttendanceCorrected): it names the attemptId of the corrected line.
 */
public record PayrollLine(
    UUID employeeUuid,
    String factoryCode,
    String direction,
    Instant scannedAt,
    UUID correctsEventId) {}
