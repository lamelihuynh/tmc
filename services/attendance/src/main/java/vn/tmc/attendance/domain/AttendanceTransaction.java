package vn.tmc.attendance.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The canonical attendance record (system of record, ADR-03). Created only when a pass is
 * confirmed; attemptId is unique so a replay can never create a second one (DP-02).
 */
public record AttendanceTransaction(
    UUID attemptId,
    UUID employeeUuid,
    String factoryCode,
    String gateId,
    Direction direction,
    Instant occurredAt,
    AttemptSource source) {}
