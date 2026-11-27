package vn.tmc.attendance.adapter.in.rest;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.tmc.attendance.application.port.in.AttemptNotFoundException;
import vn.tmc.attendance.application.port.in.AttendanceNotCountedException;
import vn.tmc.attendance.application.port.in.CorrectionConflictException;
import vn.tmc.attendance.domain.IllegalAttemptTransitionException;
import vn.tmc.attendance.domain.ManualAttendanceNotVerifiableException;
import vn.tmc.platform.security.AccessDeniedException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN"));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST"));
  }

  @ExceptionHandler(CorrectionConflictException.class)
  ResponseEntity<Map<String, String>> correctionConflict(CorrectionConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CORRECTION_CONFLICT"));
  }

  @ExceptionHandler(AttendanceNotCountedException.class)
  ResponseEntity<Map<String, String>> notCounted(AttendanceNotCountedException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "ATTENDANCE_NOT_COUNTED"));
  }

  @ExceptionHandler(ManualAttendanceNotVerifiableException.class)
  ResponseEntity<Map<String, String>> notVerifiable(ManualAttendanceNotVerifiableException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "MANUAL_ATTENDANCE_NOT_VERIFIABLE"));
  }

  @ExceptionHandler(AttemptNotFoundException.class)
  ResponseEntity<Map<String, String>> notFound(AttemptNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "ATTEMPT_NOT_FOUND"));
  }

  @ExceptionHandler(IllegalAttemptTransitionException.class)
  ResponseEntity<Map<String, String>> conflict(IllegalAttemptTransitionException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "ILLEGAL_TRANSITION", "detail", e.getMessage()));
  }
}
