package vn.tmc.attendance.domain;

import java.util.Set;

/**
 * States of one scan attempt at the edge (ADR-11, docs/architecture/state-machine.md).
 *
 * <p>RECEIVED lives only in the kiosk journal; the edge persists from VALIDATED onward. PUBLISHED
 * is not an attempt state: it is the publish_status of the outbox row so the publisher never writes
 * the aggregate.
 */
public enum AttemptStatus {
  RECEIVED,
  VALIDATED,
  RESERVED,
  GATE_COMMANDED,
  GATE_OPENED,
  GATE_FAILED,
  GATE_UNKNOWN,
  COMMITTED,
  DENIED;

  public boolean isTerminal() {
    return this == COMMITTED || this == DENIED || this == GATE_FAILED;
  }

  public Set<AttemptStatus> allowedNext() {
    return switch (this) {
      case RECEIVED -> Set.of(VALIDATED, DENIED);
      case VALIDATED -> Set.of(RESERVED, DENIED);
      case RESERVED -> Set.of(GATE_COMMANDED, DENIED);
      case GATE_COMMANDED -> Set.of(GATE_OPENED, GATE_FAILED, GATE_UNKNOWN);
      case GATE_UNKNOWN -> Set.of(GATE_OPENED, GATE_FAILED);
      case GATE_OPENED -> Set.of(COMMITTED);
      case GATE_FAILED, COMMITTED, DENIED -> Set.of();
    };
  }
}
