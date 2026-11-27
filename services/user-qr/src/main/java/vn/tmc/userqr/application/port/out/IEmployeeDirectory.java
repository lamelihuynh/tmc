package vn.tmc.userqr.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Driven port: employee eligibility that the sync bundle forwards to the edge (C-02). Eligibility
 * only, no name or personal data (ADR-09). Backed by PostgreSQL ({@code employees}, V3) on the
 * default profile and in memory on dev profiles.
 */
public interface IEmployeeDirectory {

  record Eligibility(UUID employeeUuid, boolean active, Set<String> factories, Instant updatedAt) {
    public Eligibility {
      factories = factories == null ? Set.of() : Set.copyOf(factories);
    }
  }

  Optional<Eligibility> findById(UUID employeeUuid);

  /** Insert or update an employee's eligibility (newer replaces older). */
  void save(Eligibility eligibility);

  /**
   * Insert only if the employee is not yet present, so start-up dev seeding never overwrites
   * updates made by HR.
   */
  void saveIfAbsent(Eligibility eligibility);

  /**
   * Employees whose eligibility changed strictly after {@code since} (inactive ones are kept, not
   * deleted).
   */
  List<Eligibility> changedSince(Instant since);

  /** Read-only empty directory for tests that do not exercise employee eligibility. */
  static IEmployeeDirectory empty() {
    return new IEmployeeDirectory() {
      @Override
      public Optional<Eligibility> findById(UUID employeeUuid) {
        return Optional.empty();
      }

      @Override
      public void save(Eligibility eligibility) {}

      @Override
      public void saveIfAbsent(Eligibility eligibility) {}

      @Override
      public List<Eligibility> changedSince(Instant since) {
        return List.of();
      }
    };
  }
}
