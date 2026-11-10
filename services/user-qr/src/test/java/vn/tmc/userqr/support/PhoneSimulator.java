package vn.tmc.userqr.support;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test double for the employee's phone: owns a P-256 key pair and signs QR tokens like the real
 * app.
 */
public class PhoneSimulator {
  public final KeyPair keys;
  public final UUID employee;
  public String deviceId;

  public PhoneSimulator(UUID employee) {
    this.employee = employee;
    try {
      var gen = KeyPairGenerator.getInstance("EC");
      gen.initialize(new ECGenParameterSpec("secp256r1"));
      this.keys = gen.generateKeyPair();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String publicKeyBase64() {
    return Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
  }

  public String token(Instant iat, Instant exp, String jti) {
    return token(iat, exp, jti, Map.of());
  }

  /**
   * @param extraClaims lets a test smuggle in forbidden claims such as a name
   */
  public String token(Instant iat, Instant exp, String jti, Map<String, Object> extraClaims) {
    return sign(
        deviceId,
        employee.toString(),
        iat,
        exp,
        jti,
        extraClaims,
        (ECPrivateKey) keys.getPrivate());
  }

  public static String sign(
      String kid,
      String sub,
      Instant iat,
      Instant exp,
      String jti,
      Map<String, Object> extra,
      ECPrivateKey key) {
    try {
      var builder =
          new JWTClaimsSet.Builder()
              .subject(sub)
              .jwtID(jti)
              .issueTime(Date.from(iat))
              .expirationTime(Date.from(exp))
              .claim("fac", List.of("BD", "LA"));
      extra.forEach(builder::claim);
      var jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).type(JOSEObjectType.JWT).build(),
              builder.build());
      jwt.sign(new ECDSASigner(key));
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
