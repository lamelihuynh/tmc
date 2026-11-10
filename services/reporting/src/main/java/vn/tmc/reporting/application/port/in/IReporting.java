package vn.tmc.reporting.application.port.in;

import java.util.List;
import vn.tmc.platform.security.Principal;
import vn.tmc.reporting.domain.Presence;

/** Read only (ISP): this interface has no way to change attendance (AC-03, AC-05). */
public interface IReporting {

  /** Who is inside a factory now. Supervisors, HR and executives, within their data scope. */
  List<Presence> whoIsInside(Principal caller, String factoryCode);
}
