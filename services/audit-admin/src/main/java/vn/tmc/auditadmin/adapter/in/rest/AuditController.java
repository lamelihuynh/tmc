package vn.tmc.auditadmin.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.tmc.auditadmin.application.port.in.IAuditQuery;
import vn.tmc.auditadmin.domain.AuditEntry;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.PrincipalHeaders;

/**
 * Read only on purpose: there is no POST, PUT, PATCH or DELETE for audit data (UC-11 exception
 * 5.1).
 */
@RestController
public class AuditController {
  private final IAuditQuery query;

  public AuditController(IAuditQuery query) {
    this.query = query;
  }

  @GetMapping("/admin/v1/audit")
  public List<AuditEntry> search(
      HttpServletRequest http,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String actor) {
    var caller =
        PrincipalHeaders.parse(
            http.getHeader("X-Subject"),
            http.getHeader("X-Roles"),
            http.getHeader("X-Scope-Factories"),
            http.getHeader("X-Scope-Gates"));
    if (caller == null) {
      throw new AccessDeniedException("unauthenticated");
    }
    return query.search(caller, from, to, action, actor);
  }

  @RestControllerAdvice
  static class Errors {
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
    }
  }
}
