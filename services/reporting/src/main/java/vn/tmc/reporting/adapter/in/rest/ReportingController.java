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
import vn.tmc.platform.security.PrincipalHeaders;
import vn.tmc.reporting.application.port.in.IReporting;
import vn.tmc.reporting.domain.Presence;

@RestController
public class ReportingController {
  private final IReporting reporting;

  public ReportingController(IReporting reporting) {
    this.reporting = reporting;
  }

  @GetMapping("/supervision/v1/presence")
  public List<Presence> presence(HttpServletRequest http, @RequestParam String factory) {
    var caller =
        PrincipalHeaders.parse(
            http.getHeader("X-Subject"),
            http.getHeader("X-Roles"),
            http.getHeader("X-Scope-Factories"),
            http.getHeader("X-Scope-Gates"));
    if (caller == null) {
      throw new AccessDeniedException("unauthenticated");
    }
    return reporting.whoIsInside(caller, factory);
  }

  @RestControllerAdvice
  static class Errors {
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
    }
  }
}
