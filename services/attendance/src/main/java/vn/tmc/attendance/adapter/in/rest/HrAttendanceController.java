package vn.tmc.attendance.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.attendance.application.port.in.IAttendanceCorrection;
import vn.tmc.attendance.application.port.in.IAttendanceCorrection.CorrectAttendanceCommand;
import vn.tmc.attendance.application.port.in.IAttendanceCorrection.CorrectionView;
import vn.tmc.attendance.application.port.in.IManualAttendanceVerification;
import vn.tmc.attendance.application.port.in.IManualAttendanceVerification.VerificationResult;
import vn.tmc.attendance.application.port.in.IManualAttendanceVerification.VerifyManualAttendanceCommand;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.security.AccessDeniedException;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.PrincipalResolver;

/** HR commands are separate from the trusted service-to-service attendance API (ADR-08). */
@RestController
@RequestMapping("/hr/v1")
public class HrAttendanceController {
  private final IAttendanceCorrection corrections;
  private final IManualAttendanceVerification verifications;
  private final PrincipalResolver principals;

  public HrAttendanceController(
      IAttendanceCorrection corrections,
      IManualAttendanceVerification verifications,
      PrincipalResolver principals) {
    this.corrections = corrections;
    this.verifications = verifications;
    this.principals = principals;
  }

  public record CorrectionRequest(
      @NotNull UUID correctionId,
      @NotNull Direction direction,
      @NotNull Instant scannedAt,
      @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String reason,
      String correlationId) {}

  public record VerificationRequest(
      @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String reason,
      String correlationId) {}

  @PostMapping("/attendance/{attemptId}/corrections")
  public CorrectionView correct(
      HttpServletRequest http,
      @PathVariable UUID attemptId,
      @Valid @RequestBody CorrectionRequest request) {
    return corrections.correct(
        requireCaller(http),
        new CorrectAttendanceCommand(
            request.correctionId(),
            attemptId,
            request.direction(),
            request.scannedAt(),
            request.reason(),
            correlationId(http, request.correlationId())));
  }

  @PostMapping("/attempts/{attemptId}/verify-manual")
  public VerificationResult verifyManual(
      HttpServletRequest http,
      @PathVariable UUID attemptId,
      @Valid @RequestBody VerificationRequest request) {
    return verifications.verify(
        requireCaller(http),
        new VerifyManualAttendanceCommand(
            attemptId, request.reason(), correlationId(http, request.correlationId())));
  }

  private Principal requireCaller(HttpServletRequest http) {
    var caller = principals.resolve(http::getHeader);
    if (caller == null) {
      // Keep the fail-closed 403 contract shared by the other services and identity.openapi.
      throw new AccessDeniedException("unauthenticated");
    }
    return caller;
  }

  private String correlationId(HttpServletRequest http, String bodyValue) {
    var header = http.getHeader("X-Correlation-Id");
    return header == null || header.isBlank() ? bodyValue : header;
  }
}
