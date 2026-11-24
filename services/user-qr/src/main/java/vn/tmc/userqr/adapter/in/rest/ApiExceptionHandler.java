package vn.tmc.userqr.adapter.in.rest;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.userqr.application.port.out.ActiveDeviceConflictException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "BAD_REQUEST", "detail", e.getMessage()));
  }

  /**
   * C-01: two registrations for the same employee ran at the same time and the other one won. The
   * detail names the situation only (no device id, no employee), so the app can register again.
   */
  @ExceptionHandler(ActiveDeviceConflictException.class)
  ResponseEntity<Map<String, String>> conflict(ActiveDeviceConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "CONFLICT", "detail", "CONCURRENT_DEVICE_REGISTRATION"));
  }
}
