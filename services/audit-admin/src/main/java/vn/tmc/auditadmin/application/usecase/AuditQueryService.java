package vn.tmc.auditadmin.application.usecase;

import java.time.Instant;
import java.util.List;
import vn.tmc.auditadmin.application.port.in.IAuditQuery;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.domain.AuditEntry;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;

public class AuditQueryService implements IAuditQuery {
  private static final int LIMIT = 500;
  private final IAuditStore store;
  private final RecordAuditUseCase recorder;
  private final IClock clock;

  public AuditQueryService(IAuditStore store, RecordAuditUseCase recorder, IClock clock) {
    this.store = store;
    this.recorder = recorder;
    this.clock = clock;
  }

  @Override
  public List<AuditEntry> search(
      Principal caller, Instant from, Instant to, String action, String actor) {
    Rbac.requireRole(caller, Role.SYSTEM_ADMIN);
    var result = store.search(from, to, action, actor, LIMIT);
    recorder.recordLocal(
        "AUDIT_QUERIED",
        caller.subject().toString(),
        "filter:" + action + "/" + actor,
        clock.now());
    return result;
  }
}
