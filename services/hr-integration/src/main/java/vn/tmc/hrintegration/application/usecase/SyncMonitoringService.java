package vn.tmc.hrintegration.application.usecase;

import java.util.List;
import vn.tmc.hrintegration.application.port.in.ISyncMonitoring;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;

public class SyncMonitoringService implements ISyncMonitoring {
  private final ISyncLogRepository log;

  public SyncMonitoringService(ISyncLogRepository log) {
    this.log = log;
  }

  @Override
  public List<SyncRecord> byStatus(Principal caller, SyncStatus status) {
    Rbac.requireRole(caller, Role.SYSTEM_ADMIN);
    return log.byStatus(status);
  }
}
