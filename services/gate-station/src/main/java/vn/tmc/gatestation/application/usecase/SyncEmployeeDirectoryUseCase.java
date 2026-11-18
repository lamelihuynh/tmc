package vn.tmc.gatestation.application.usecase;

import java.time.Instant;
import vn.tmc.gatestation.application.port.out.IEmployeeDirectoryCache;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;
import vn.tmc.platform.kernel.IClock;

/**
 * Pulls employee eligibility from User/QR's sync bundle into the edge directory (C-02). If User/QR
 * is down the error propagates and the directory keeps what it has: scans go on with the last known
 * eligibility. The cursor only moves after a successful pull.
 */
public class SyncEmployeeDirectoryUseCase {
  private final IKioskSyncSource source;
  private final IEmployeeDirectoryCache directory;
  private final IClock clock;
  private Instant cursor = Instant.EPOCH;

  public SyncEmployeeDirectoryUseCase(
      IKioskSyncSource source, IEmployeeDirectoryCache directory, IClock clock) {
    this.source = source;
    this.directory = directory;
    this.clock = clock;
  }

  /**
   * @return how many employees changed
   */
  public synchronized int refresh() {
    var requestedAt = clock.now();
    var delta = source.changedSince(cursor);
    directory.upsertAll(delta.employees());
    cursor = delta.generatedAt() != null ? delta.generatedAt() : requestedAt;
    return delta.employees().size();
  }

  public synchronized Instant cursor() {
    return cursor;
  }
}
