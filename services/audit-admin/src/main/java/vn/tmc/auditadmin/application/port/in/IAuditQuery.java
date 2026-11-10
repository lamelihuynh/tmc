package vn.tmc.auditadmin.application.port.in;

import java.time.Instant;
import java.util.List;
import vn.tmc.auditadmin.domain.AuditEntry;
import vn.tmc.platform.security.Principal;

/** UC-11. System Administrator only. The act of searching is itself audited (step 6). */
public interface IAuditQuery {
  List<AuditEntry> search(Principal caller, Instant from, Instant to, String action, String actor);
}
