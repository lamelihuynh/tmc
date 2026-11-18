package vn.tmc.gatestation.adapter.out.inmem;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.gatestation.application.port.out.IQRValidation;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Stand-in for the QR Verifier: a test says which token strings are valid and which are denied. A
 * token nobody registered is MALFORMED_TOKEN, like garbage would be.
 */
public class ProgrammableQrValidation implements IQRValidation {
  private final Map<String, Verdict> verdicts = new ConcurrentHashMap<>();

  public void accept(String token, UUID employeeUuid, String jti, Instant expiresAt) {
    verdicts.put(token, Verdict.ok(employeeUuid, jti, expiresAt));
  }

  public void deny(String token, ReasonCode reason) {
    verdicts.put(token, Verdict.denied(reason));
  }

  @Override
  public Verdict verify(String compactToken) {
    var verdict = compactToken == null ? null : verdicts.get(compactToken);
    return verdict != null ? verdict : Verdict.denied(ReasonCode.MALFORMED_TOKEN);
  }
}
