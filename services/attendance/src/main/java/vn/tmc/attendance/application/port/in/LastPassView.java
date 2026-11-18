package vn.tmc.attendance.application.port.in;

import java.time.Instant;
import java.util.UUID;
import vn.tmc.attendance.domain.AttendanceTransaction;
import vn.tmc.attendance.domain.Direction;

/**
 * The latest counted pass of an employee: what Gate Station needs to rebuild its anti-passback
 * state after Redis lost it (A-03, B-05).
 */
public record LastPassView(
    UUID employeeUuid, Direction direction, Instant occurredAt, String factoryCode, String gateId) {

  public static LastPassView of(AttendanceTransaction t) {
    return new LastPassView(
        t.employeeUuid(), t.direction(), t.occurredAt(), t.factoryCode(), t.gateId());
  }
}
