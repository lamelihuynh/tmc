package vn.tmc.reporting.application.usecase;

import java.util.List;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Rbac;
import vn.tmc.platform.security.Role;
import vn.tmc.reporting.application.port.in.IReporting;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.domain.Presence;

public class ReportingQueryService implements IReporting {
  private final IPresenceRepository presences;

  public ReportingQueryService(IPresenceRepository presences) {
    this.presences = presences;
  }

  @Override
  public List<Presence> whoIsInside(Principal caller, String factoryCode) {
    Rbac.requireRole(caller, Role.PRODUCTION_SUPERVISOR, Role.HR_STAFF, Role.EXECUTIVE);
    Rbac.requireFactory(caller, factoryCode);
    // TODO Team D: a supervisor must only see employees of their own group or shift (needs group in
    // the event)
    return presences.byFactory(factoryCode).stream().filter(Presence::isInside).toList();
  }
}
