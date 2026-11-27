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
import vn.tmc.auditadmin.domain.AuditHashChain;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.PrincipalResolver;

/**
 * Read only on purpose: there is no POST, PUT, PATCH or DELETE for audit data (UC-11 exception
 * 5.1).
 */
@RestController
public class AuditController {
  private final IAuditQuery query;
  private final PrincipalResolver principals;

  public AuditController(IAuditQuery query, PrincipalResolver principals) {
    this.query = query;
    this.principals = principals;
  }

  @GetMapping("/admin/v1/audit")
  public List<AuditEntry> search(
      HttpServletRequest http,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String actor) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new Unauthenticated();
    }
    return query.search(caller, from, to, action, actor);
  }

  /** AC-05: is the stored log still intact? */
  @GetMapping("/admin/v1/audit/verification")
  public AuditHashChain.Verification verify(HttpServletRequest http) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      throw new Unauthenticated();
    }
    return query.verifyChain(caller);
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
