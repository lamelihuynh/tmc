package vn.tmc.identity.adapter.out.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import vn.tmc.identity.application.port.out.IMfaVerifier;
import vn.tmc.platform.kernel.IClock;

/**
 * RFC 6238 TOTP verifier for MFA (C-05, NFR-03): HMAC-SHA1, 6-digit codes, 30-second time step,
 * accepting the current step plus one step before and after ({@code ±1} step clock-skew window).
 *
 * <p>Replay protection (RFC 6238 section 5.2): records the last accepted time step per username so
 * a code that has already succeeded (or an older step) cannot be reused. Fails closed on unknown
 * users, malformed codes, or missing secrets.
 */
public class TotpMfaVerifier implements IMfaVerifier {
  public static final long TIME_STEP_SECONDS = 30L;
  public static final int DIGITS = 6;
  public static final int ALLOWED_STEP_SKEW = 1;

  private static final String HMAC_ALGORITHM = "HmacSHA1";
  private static final int MODULO = 1_000_000;
  private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  private final IClock clock;
  private final Map<String, byte[]> secrets = new ConcurrentHashMap<>();
  private final Map<String, Long> lastUsedSteps = new ConcurrentHashMap<>();

  public TotpMfaVerifier(IClock clock) {
    this.clock = clock;
  }

  /** Registers a raw shared secret for {@code username}. */
  public void registerSecret(String username, byte[] secret) {
    if (username == null || username.isBlank() || secret == null || secret.length == 0) {
      throw new IllegalArgumentException("username and non-empty secret are required");
    }
    secrets.put(username, Arrays.copyOf(secret, secret.length));
  }

  /** Registers an RFC 4648 Base32 shared secret for {@code username}. */
  public void registerBase32(String username, String base32Secret) {
    registerSecret(username, decodeBase32(base32Secret));
  }

  @Override
  public boolean verify(String username, String code) {
    if (username == null || !isSixDigits(code)) {
      return false;
    }
    byte[] secret = secrets.get(username);
    if (secret == null) {
      return false;
    }
    long currentStep = timeStep(clock.now());
    byte[] provided = code.getBytes(StandardCharsets.UTF_8);

    long matchedStep = Long.MIN_VALUE;
    for (int offset = -ALLOWED_STEP_SKEW; offset <= ALLOWED_STEP_SKEW; offset++) {
      long candidateStep = currentStep + offset;
      if (candidateStep < 0) {
        continue;
      }
      byte[] expected = generateCode(secret, candidateStep).getBytes(StandardCharsets.UTF_8);
      if (MessageDigest.isEqual(expected, provided) && matchedStep == Long.MIN_VALUE) {
        matchedStep = candidateStep;
      }
    }
    if (matchedStep == Long.MIN_VALUE) {
      return false;
    }

    long acceptedStep = matchedStep;
    var accepted = new AtomicBoolean(false);
    lastUsedSteps.compute(
        username,
        (u, lastStep) -> {
          if (lastStep != null && acceptedStep <= lastStep) {
            return lastStep;
          }
          accepted.set(true);
          return acceptedStep;
        });
    return accepted.get();
  }

  public static long timeStep(Instant instant) {
    return Math.floorDiv(instant.getEpochSecond(), TIME_STEP_SECONDS);
  }

  /** Generates the 6-digit RFC 6238 / RFC 4226 OTP for a given secret and time-step counter. */
  public static String generateCode(byte[] secret, long step) {
    try {
      byte[] counter = ByteBuffer.allocate(8).putLong(step).array();
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
      byte[] hmac = mac.doFinal(counter);
      int offset = hmac[hmac.length - 1] & 0x0F;
      int binary =
          ((hmac[offset] & 0x7F) << 24)
              | ((hmac[offset + 1] & 0xFF) << 16)
              | ((hmac[offset + 2] & 0xFF) << 8)
              | (hmac[offset + 3] & 0xFF);
      return String.format(Locale.ROOT, "%0" + DIGITS + "d", binary % MODULO);
    } catch (Exception e) {
      throw new IllegalStateException("HmacSHA1 is not available", e);
    }
  }

  /** Convenience helper for generating a code at {@code instant} from an RFC 4648 Base32 secret. */
  public static String generateCodeAt(String base32Secret, Instant instant) {
    return generateCode(decodeBase32(base32Secret), timeStep(instant));
  }

  public static byte[] decodeBase32(String input) {
    if (input == null) {
      throw new IllegalArgumentException("Base32 secret must not be null");
    }
    String clean =
        input.replace(" ", "").replace("-", "").replace("=", "").toUpperCase(Locale.ROOT);
    if (clean.isEmpty()) {
      throw new IllegalArgumentException("Base32 secret must not be empty");
    }
    byte[] out = new byte[(clean.length() * 5) / 8];
    if (out.length == 0) {
      throw new IllegalArgumentException("Base32 secret is too short");
    }
    int buffer = 0;
    int bitsLeft = 0;
    int index = 0;
    for (int i = 0; i < clean.length(); i++) {
      int value = BASE32_ALPHABET.indexOf(clean.charAt(i));
      if (value < 0) {
        throw new IllegalArgumentException("Invalid Base32 character in TOTP secret");
      }
      buffer = (buffer << 5) | value;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        out[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
        bitsLeft -= 8;
      }
    }
    return out;
  }

  private static boolean isSixDigits(String code) {
    if (code == null || code.length() != DIGITS) {
      return false;
    }
    for (int i = 0; i < DIGITS; i++) {
      char c = code.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "TotpMfaVerifier[" + secrets.size() + " enrolled]";
  }
}
