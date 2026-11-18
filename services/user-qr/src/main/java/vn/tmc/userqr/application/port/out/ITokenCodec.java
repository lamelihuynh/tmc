package vn.tmc.userqr.application.port.out;

import java.security.PublicKey;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.userqr.domain.QrClaims;

/**
 * Driven port: reads and checks the compact JWS of a QR token (ADR-12). Keeps the JOSE library out
 * of the use case. Parsing never trusts the content: it only extracts what the use case needs to
 * find the device key; the signature is checked in a second step.
 */
public interface ITokenCodec {

  /** keyId is the JWS "kid", which is the device id. */
  record Parsed(String keyId, QrClaims claims) {}

  /** The token cannot be read, has the wrong algorithm, or carries a claim that is not allowed. */
  class TokenFormatException extends RuntimeException {
    private final ReasonCode reason;

    public TokenFormatException(ReasonCode reason, String message) {
      super(message);
      this.reason = reason;
    }

    public ReasonCode reason() {
      return reason;
    }
  }

  /**
   * @throws TokenFormatException when the token is not a well-formed ES256 QR token
   */
  Parsed parse(String compactToken);

  /**
   * @return true only when the signature was made by the private key matching {@code key}
   */
  boolean verifySignature(String compactToken, PublicKey key);
}
