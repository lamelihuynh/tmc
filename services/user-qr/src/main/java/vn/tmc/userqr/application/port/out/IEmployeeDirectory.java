package vn.tmc.userqr.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Driven port: employee eligibility that the sync bundle forwards to the edge (C-02). Eligibility
 * only, no name or personal data (ADR-09). TODO Team C (C-02): fed by HR instead of configuration.
 */
public interface IEmployeeDirectory {

  record Eligibility(UUID employeeUuid, boolean active, Set<String> factories, Instant updatedAt) {
    public Eligibility {
      factories = factories == null ? Set.of() : Set.copyOf(factories);
    }
  }

  /**
   * Employees whose eligibility changed after {@code since} (inactive ones are kept, not deleted).
   */
  List<Eligibility> changedSince(Instant since);
}
