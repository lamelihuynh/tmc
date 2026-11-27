package vn.tmc.gatestation.adapter.out.inmem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.domain.LastPass;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Stand-in for the Attendance service: follows the same state rules (RESERVED, GATE_COMMANDED, then
 * COMMITTED, GATE_FAILED or GATE_UNKNOWN; every call idempotent per attempt id) and can be told to
 * fail, so tests can stop the flow at a chosen step. For tests and the inmem profile.
 */
public class FakeAttendanceGateway implements IAttendanceGateway {
  private final Map<UUID, AttemptState> attempts = new ConcurrentHashMap<>();
  private final Map<UUID, Instant> scannedAt = new ConcurrentHashMap<>();
  private final Map<UUID, LastPass> seededLastPass = new ConcurrentHashMap<>();
  private volatile boolean failRecordAttempt;
  private volatile boolean failRecordResult;
  private volatile boolean failLastDirection;

  /** While true, recordAttempt throws as if Attendance were down. */
  public void failRecordAttempt(boolean fail) {
    this.failRecordAttempt = fail;
  }

  /** While true, recordGateResult throws as if Attendance were down. */
  public void failRecordResult(boolean fail) {
    this.failRecordResult = fail;
  }

  /** While true, lastDirection throws as if Attendance were down. */
  public void failLastDirection(boolean fail) {
    this.failLastDirection = fail;
  }

  /** For tests: the stored state of an attempt. */
  public Optional<AttemptState> state(UUID attemptId) {
    return find(attemptId);
  }

  @Override
  public Optional<AttemptState> find(UUID attemptId) {
    return Optional.ofNullable(attempts.get(attemptId));
  }

  @Override
  public AttemptState recordAttempt(AttemptRecord record) {
    if (failRecordAttempt) {
      throw new IllegalStateException("attendance unavailable");
    }
    scannedAt.putIfAbsent(record.attemptId(), record.scannedAt());
    return attempts.computeIfAbsent(
        record.attemptId(),
        id ->
            new AttemptState(id, State.RESERVED, null, record.employeeUuid(), record.direction()));
  }

  @Override
  public AttemptState recordDenied(DenialRecord record) {
    return attempts.computeIfAbsent(
        record.attemptId(),
        id ->
            new AttemptState(
                id, State.DENIED, record.reason(), record.employeeUuid(), record.direction()));
  }

  @Override
  public AttemptState markGateCommanded(UUID attemptId) {
    return attempts.compute(
        attemptId,
        (id, current) -> {
          if (current == null) {
            throw new IllegalStateException("attempt not found: " + id);
          }
          return current.state() == State.RESERVED
              ? next(current, State.GATE_COMMANDED, null)
              : current;
        });
  }

  @Override
  public AttemptState recordGateResult(UUID attemptId, GateResult result, String correlationId) {
    if (failRecordResult) {
      throw new IllegalStateException("attendance unavailable");
    }
    return attempts.compute(
        attemptId,
        (id, current) -> {
          if (current == null) {
            throw new IllegalStateException("attempt not found: " + id);
          }
          if (isFinal(current.state())) {
            return current;
          }
          return switch (result) {
            case OPENED -> next(current, State.COMMITTED, null);
            case ERROR -> next(current, State.GATE_FAILED, ReasonCode.GATE_HARDWARE_ERROR);
            case UNKNOWN ->
                next(current, State.GATE_UNKNOWN, ReasonCode.TURNSTILE_TIMEOUT_RETRY_SCAN);
          };
        });
  }

  @Override
  public List<OfflineItemResult> ingestOffline(String kioskId, List<OfflineItem> items) {
    var results = new ArrayList<OfflineItemResult>();
    for (var item : items) {
      boolean denied = "OFFLINE_DENIED".equals(item.localStatus());
      var stored =
          attempts.putIfAbsent(
              item.attemptId(),
              new AttemptState(
                  item.attemptId(),
                  denied ? State.DENIED : State.COMMITTED,
                  denied ? item.deniedReason() : null,
                  item.employeeUuid(),
                  item.direction()));
      if (stored == null) {
        scannedAt.put(item.attemptId(), item.scannedAt());
      }
      results.add(
          stored == null
              ? new OfflineItemResult(item.attemptId(), OfflineOutcome.ACCEPTED, "counted")
              : new OfflineItemResult(
                  item.attemptId(), OfflineOutcome.DUPLICATE, "attempt already known"));
    }
    return results;
  }

  @Override
  public Optional<LastPass> lastDirection(UUID employeeUuid, Instant before) {
    if (failLastDirection) {
      throw new IllegalStateException("attendance unavailable");
    }
    var seeded = seededLastPass.get(employeeUuid);
    var counted =
        attempts.values().stream()
            .filter(a -> a.state() == State.COMMITTED)
            .filter(a -> employeeUuid.equals(a.employeeUuid()) && a.direction() != null)
            .filter(a -> scannedAt.containsKey(a.attemptId()))
            .filter(a -> scannedAt.get(a.attemptId()).isBefore(before))
            .map(a -> new LastPass(a.direction(), scannedAt.get(a.attemptId())))
            .max(Comparator.comparing(LastPass::at));
    if (seeded != null && seeded.at().isBefore(before)) {
      return counted.filter(c -> c.at().isAfter(seeded.at())).or(() -> Optional.of(seeded));
    }
    return counted;
  }

  /** For tests: pretend Attendance already holds this counted pass for the employee. */
  public void seedLastPass(UUID employeeUuid, LastPass pass) {
    seededLastPass.put(employeeUuid, pass);
  }

  private static AttemptState next(AttemptState current, State state, ReasonCode reason) {
    return new AttemptState(
        current.attemptId(), state, reason, current.employeeUuid(), current.direction());
  }

  private static boolean isFinal(State state) {
    return state == State.COMMITTED || state == State.DENIED || state == State.GATE_FAILED;
  }
}
