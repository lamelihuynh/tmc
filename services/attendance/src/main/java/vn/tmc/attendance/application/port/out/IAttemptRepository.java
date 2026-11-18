package vn.tmc.attendance.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.attendance.domain.AccessAttempt;
import vn.tmc.attendance.domain.AttemptStatus;

/**
 * Driven port: storage of the AccessAttempt aggregate (table access_attempts). attempt_id is the
 * primary key, which is what makes a retried request land on the same row (ADR-05).
 */
public interface IAttemptRepository {
  Optional<AccessAttempt> findById(UUID attemptId);

  /** Insert or replace by attempt id. */
  void save(AccessAttempt attempt);

  /**
   * Replay check for offline batches (ADR-12 layer R3): is the same token id already recorded under
   * a different attempt?
   */
  boolean existsOtherWithJti(String jti, UUID exceptAttemptId);

  /** Oldest change first. */
  List<AccessAttempt> findByStatus(AttemptStatus status, int limit);

  /** Attempts in {@code status} whose last change is strictly before {@code cutoff}. */
  List<AccessAttempt> findByStatusUpdatedBefore(AttemptStatus status, Instant cutoff, int limit);
}
