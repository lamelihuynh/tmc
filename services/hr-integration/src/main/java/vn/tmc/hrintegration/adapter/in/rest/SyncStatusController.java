package vn.tmc.hrintegration.adapter.in.rest;

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
import vn.tmc.hrintegration.application.port.in.ISyncMonitoring;
import vn.tmc.hrintegration.domain.SyncRecord;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.PrincipalResolver;

@RestController
public class SyncStatusController {
  private final ISyncMonitoring monitoring;
  private final PrincipalResolver principals;

  public SyncStatusController(ISyncMonitoring monitoring, PrincipalResolver principals) {
    this.monitoring = monitoring;
    this.principals = principals;
  }

  @GetMapping("/admin/v1/hr-sync")
  public List<SyncRecord> byStatus(HttpServletRequest http, @RequestParam SyncStatus status) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new Unauthenticated();
    }
    return monitoring.byStatus(caller, status);
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
