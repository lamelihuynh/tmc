package vn.tmc.userqr.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.userqr.application.port.in.IQRValidation;
import vn.tmc.userqr.application.port.in.ISyncBundle;
import vn.tmc.userqr.application.port.in.VerificationResult;

/**
 * Internal endpoints used by Gate Station (verify) and Edge Agent (sync). mTLS-protected, not
 * public.
 */
@RestController
public class QrVerifyController {
  private final IQRValidation validation;
  private final ISyncBundle sync;

  public QrVerifyController(IQRValidation validation, ISyncBundle sync) {
    this.validation = validation;
    this.sync = sync;
  }

  public record VerifyRequest(@NotBlank String token) {}

  @PostMapping("/internal/v1/qr/verify")
  public VerificationResult verify(@Valid @RequestBody VerifyRequest body) {
    return validation.verify(body.token());
  }

  @GetMapping("/internal/v1/sync-bundle")
  public ISyncBundle.Bundle bundle(@RequestParam(required = false) Instant since) {
    return sync.changedSince(since == null ? Instant.EPOCH : since);
  }
}
