package vn.tmc.hrintegration.application.port.in;

import java.util.List;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.security.Principal;

/**
 * FR-30: System Administrator watches the sync state. TODO Team D: trigger-resend, batch status.
 */
public interface ISyncMonitoring {
  List<SyncRecord> byStatus(Principal caller, SyncStatus status);
}
