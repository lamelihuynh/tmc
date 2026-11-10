package vn.tmc.userqr.application.usecase;

import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.userqr.application.port.in.IQRValidation;
import vn.tmc.userqr.application.port.in.VerificationResult;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.application.port.out.ITokenCodec;
import vn.tmc.userqr.domain.TokenVerificationPolicy;

/**
 * Verification order (cheap and safe first), same code path online at the edge and as the model for
 * the kiosk's offline verifier: structure and PII guard, device lookup, device status, signature,
 * time rules. Fail-secure: any unexpected problem denies (DP-04).
 */
public class VerifyQrTokenUseCase implements IQRValidation {
  private final ITokenCodec codec;
  private final IDeviceKeyRegistry devices;
  private final TokenVerificationPolicy policy;
  private final IClock clock;

  public VerifyQrTokenUseCase(
      ITokenCodec codec, IDeviceKeyRegistry devices, TokenVerificationPolicy policy, IClock clock) {
    this.codec = codec;
    this.devices = devices;
    this.policy = policy;
    this.clock = clock;
  }

  @Override
  public VerificationResult verify(String compactToken) {
    ITokenCodec.Parsed parsed;
    try {
      parsed = codec.parse(compactToken);
    } catch (ITokenCodec.TokenFormatException e) {
      return VerificationResult.denied(e.reason());
    } catch (RuntimeException e) {
      return VerificationResult.denied(ReasonCode.MALFORMED_TOKEN);
    }

    var device = devices.findById(parsed.keyId()).orElse(null);
    if (device == null) {
      return VerificationResult.denied(ReasonCode.DEVICE_UNKNOWN);
    }
    if (!device.isActive()) {
      return VerificationResult.denied(ReasonCode.DEVICE_REVOKED);
    }
    // The key owner must be the subject of the token, otherwise a device could vouch for someone
    // else.
    if (!device.employeeUuid().equals(parsed.claims().subject())) {
      return VerificationResult.denied(ReasonCode.INVALID_SIGNATURE);
    }
    boolean signatureOk;
    try {
      signatureOk = codec.verifySignature(compactToken, device.publicKey());
    } catch (RuntimeException e) {
      signatureOk = false;
    }
    if (!signatureOk) {
      return VerificationResult.denied(ReasonCode.INVALID_SIGNATURE);
    }
    var timeProblem = policy.check(parsed.claims(), clock.now());
    if (timeProblem.isPresent()) {
      return VerificationResult.denied(timeProblem.get());
    }
    var c = parsed.claims();
    return VerificationResult.ok(
        c.subject(), device.deviceId(), c.jti(), c.expiresAt(), c.factories());
  }
}
