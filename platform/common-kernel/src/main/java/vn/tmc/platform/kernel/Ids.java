package vn.tmc.platform.kernel;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID v7 (time-ordered) generator used for attempt_id, messageId, journal keys (DP-02 Idempotency
 * by Default).
 */
public final class Ids {
  private static final SecureRandom RANDOM = new SecureRandom();

  private Ids() {}

  public static UUID uuidV7() {
    long millis = System.currentTimeMillis();
    long msb = (millis & 0xFFFFFFFFFFFFL) << 16;
    long randA = RANDOM.nextInt(1 << 12);
    msb |= (0x7L << 12) | randA;
    long lsb = RANDOM.nextLong();
    lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
    return new UUID(msb, lsb);
  }
}
