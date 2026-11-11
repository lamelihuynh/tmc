package vn.tmc.attendance.adapter.out.inmem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.attendance.application.port.out.IGateStatusQuery;
import vn.tmc.attendance.domain.AccessAttempt;

/**
 * Stub for tests and for running attendance alone: answers what it was told per attempt, and
 * UNDETERMINED otherwise (the safe answer: nothing is committed or failed by guessing).
 */
public class ConfigurableGateStatusQuery implements IGateStatusQuery {
  private final Map<UUID, GateObservation> answers = new ConcurrentHashMap<>();

  public void answer(UUID attemptId, GateObservation observation) {
    answers.put(attemptId, observation);
  }

  @Override
  public GateObservation observe(AccessAttempt attempt) {
    return answers.getOrDefault(attempt.attemptId(), GateObservation.UNDETERMINED);
  }
}
