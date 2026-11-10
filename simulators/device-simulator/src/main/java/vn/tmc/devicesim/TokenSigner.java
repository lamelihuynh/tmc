package vn.tmc.devicesim;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the QR token exactly as the phone app would (ADR-12): compact JWS, ES256, kid = deviceId,
 * claims limited to sub, jti, iat, exp, fac, nonce. The options exist only to demonstrate
 * rejections.
 */
public final class TokenSigner {
  private TokenSigner() {}

  public record Options(
      long ttlSeconds, String jti, boolean addPii, boolean longLived, Instant issuedAt) {
    public static Options defaults() {
      return new Options(30, null, false, false, Instant.now());
    }
  }

  public static String sign(DeviceProfile profile, List<String> factories, Options options) {
    if (profile.deviceId() == null) {
      throw new IllegalStateException("device is not registered yet; run `register` first");
    }
    try {
      Instant iat = options.issuedAt();
      Instant exp = iat.plusSeconds(options.longLived() ? 24 * 3600 : options.ttlSeconds());
      var claims =
          new JWTClaimsSet.Builder()
              .subject(profile.employeeUuid().toString())
              .jwtID(options.jti() != null ? options.jti() : UUID.randomUUID().toString())
              .issueTime(Date.from(iat))
              .expirationTime(Date.from(exp))
              .claim("fac", factories)
              .claim("nonce", UUID.randomUUID().toString().substring(0, 8));
      if (options.addPii()) {
        claims.claim("name", "Nguyen Van A"); // a server must reject this (ADR-09)
      }
      var jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.ES256)
                  .keyID(profile.deviceId())
                  .type(JOSEObjectType.JWT)
                  .build(),
              claims.build());
      jwt.sign(new ECDSASigner(profile.privateKey()));
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("cannot sign token", e);
    }
  }

  /** Visible so tests and the CLI can show what was signed. */
  public static Map<String, Object> describe(String token) throws Exception {
    var jwt = SignedJWT.parse(token);
    var out = new LinkedHashMap<String, Object>();
    out.put("kid", jwt.getHeader().getKeyID());
    out.putAll(jwt.getJWTClaimsSet().getClaims());
    return out;
  }
}
