package vn.tmc.reporting.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.reporting.domain.Presence;
import vn.tmc.reporting.domain.ScanRecord;

/** Driven port: the presence read model, stored as one row per scan (attemptId). */
public interface IPresenceRepository {
  /**
   * Stores a scan, or replaces the stored version of the same attempt if this one is newer ({@link
   * ScanRecord#replaces}). An older or equal version is ignored.
   */
  void upsertScan(ScanRecord scan);

  /** The employee's latest scan in that factory. */
  Optional<Presence> find(UUID employeeUuid, String factoryCode);

  /** The latest scan of every employee seen in that factory. */
  List<Presence> byFactory(String factoryCode);
}
