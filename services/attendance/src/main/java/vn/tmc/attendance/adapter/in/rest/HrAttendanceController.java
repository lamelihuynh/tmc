package vn.tmc.attendance.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.attendance.application.port.in.AnomalyView;
import vn.tmc.attendance.application.port.in.IAttendanceQuery;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.PrincipalResolver;

/** HR-facing attendance endpoints (UC-09, B-03). Requires HR_STAFF role. */
@RestController
@RequestMapping("/hr/v1")
public class HrAttendanceController {
  private final IAttendanceQuery query;
  private final PrincipalResolver principals;

  public HrAttendanceController(IAttendanceQuery query, PrincipalResolver principals) {
    this.query = query;
    this.principals = principals;
  }

  @GetMapping("/anomalies")
  public List<AnomalyView> anomalies(
      HttpServletRequest http,
      @RequestParam(required = false) String factory,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(defaultValue = "100") int limit) {
    return query.anomalies(requireCaller(http), factory, from, to, limit);
  }

  private Principal requireCaller(HttpServletRequest http) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new AccessDeniedException("unauthenticated");
    }
    return caller;
  }
}
