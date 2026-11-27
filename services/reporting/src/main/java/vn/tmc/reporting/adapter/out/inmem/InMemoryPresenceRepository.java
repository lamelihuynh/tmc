package vn.tmc.reporting.adapter.out.inmem;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.domain.Presence;
import vn.tmc.reporting.domain.ScanRecord;

/** Process-memory read model for the inmem and local profiles; same rules as the JDBC one. */
public class InMemoryPresenceRepository implements IPresenceRepository {
  /** Latest first; ties broken the same way as the SQL ORDER BY in the JDBC adapter. */
  static final Comparator<ScanRecord> LATEST_FIRST =
      Comparator.comparing(ScanRecord::scannedAt)
          .thenComparingLong(ScanRecord::aggregateVersion)
          .thenComparing(ScanRecord::attemptId)
          .reversed();

  private final Map<UUID, ScanRecord> scans = new ConcurrentHashMap<>();

  @Override
  public void upsertScan(ScanRecord scan) {
    scans.merge(
        scan.attemptId(),
        scan,
        (stored, candidate) -> candidate.replaces(stored) ? candidate : stored);
  }

  @Override
  public Optional<Presence> find(UUID employeeUuid, String factoryCode) {
    return scans.values().stream()
        .filter(s -> s.employeeUuid().equals(employeeUuid) && s.factoryCode().equals(factoryCode))
        .sorted(LATEST_FIRST)
        .findFirst()
        .map(ScanRecord::toPresence);
  }

  @Override
  public List<Presence> byFactory(String factoryCode) {
    return scans.values().stream()
        .filter(s -> s.factoryCode().equals(factoryCode))
        .collect(Collectors.groupingBy(ScanRecord::employeeUuid))
        .values()
        .stream()
        .map(list -> list.stream().sorted(LATEST_FIRST).findFirst().orElseThrow().toPresence())
        .toList();
  }
}
