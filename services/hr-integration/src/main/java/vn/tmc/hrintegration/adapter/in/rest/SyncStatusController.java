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
import vn.tmc.platform.security.PrincipalHeaders;

@RestController
public class SyncStatusController {
  private final ISyncMonitoring monitoring;

  public SyncStatusController(ISyncMonitoring monitoring) {
    this.monitoring = monitoring;
  }

  @GetMapping("/admin/v1/hr-sync")
  public List<SyncRecord> byStatus(HttpServletRequest http, @RequestParam SyncStatus status) {
    var caller =
        PrincipalHeaders.parse(
            http.getHeader("X-Subject"),
            http.getHeader("X-Roles"),
            http.getHeader("X-Scope-Factories"),
            http.getHeader("X-Scope-Gates"));
    if (caller == null) {
      throw new AccessDeniedException("unauthenticated");
    }
    return monitoring.byStatus(caller, status);
  }

  @RestControllerAdvice
  static class Errors {
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
    }
  }
}
