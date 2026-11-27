package vn.tmc.reporting.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.PrincipalResolver;
import vn.tmc.reporting.application.port.in.IReporting;
import vn.tmc.reporting.domain.FraudAlert;
import vn.tmc.reporting.domain.Presence;

@RestController
public class ReportingController {
  private final IReporting reporting;
  private final PrincipalResolver principals;

  public ReportingController(IReporting reporting, PrincipalResolver principals) {
    this.reporting = reporting;
    this.principals = principals;
  }

  @GetMapping("/supervision/v1/presence")
  public List<Presence> presence(HttpServletRequest http, @RequestParam String factory) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new Unauthenticated();
    }
    return reporting.whoIsInside(caller, factory);
  }

  /** ADR-12 R4 alerts involving a factory, for HR review (UC-09). */
  @GetMapping("/supervision/v1/alerts")
  public List<FraudAlert> alerts(HttpServletRequest http, @RequestParam String factory) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new Unauthenticated();
    }
    return reporting.fraudAlerts(caller, factory);
  }

  /** No caller: no token, a bad or expired token, or dev headers where they are off. */
  static final class Unauthenticated extends RuntimeException {
    Unauthenticated() {
      super("unauthenticated");
    }
  }

  @RestControllerAdvice
  static class Errors {
    @ExceptionHandler(Unauthenticated.class)
    ResponseEntity<Map<String, String>> unauthenticated(Unauthenticated e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header("WWW-Authenticate", "Bearer")
          .body(Map.of("error", "UNAUTHENTICATED"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
    }
  }
}
