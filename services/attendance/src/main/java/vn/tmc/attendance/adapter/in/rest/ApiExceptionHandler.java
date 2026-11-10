package vn.tmc.attendance.adapter.in.rest;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.tmc.attendance.application.port.in.AttemptNotFoundException;
import vn.tmc.attendance.domain.IllegalAttemptTransitionException;

@RestControllerAdvice
public class ApiExceptionHandler {

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
