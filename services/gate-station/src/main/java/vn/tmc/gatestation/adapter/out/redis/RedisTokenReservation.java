package vn.tmc.gatestation.adapter.out.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.platform.kernel.IClock;

/**
 * One-time use of a token id on the edge Redis (ADR-12 layer R1). Key {@code tmc:qr:jti:<jti>}:
 *
 * <ul>
 *   <li>reserve: atomic owner-aware claim, so exactly one attempt wins.
 *   <li>markUsed: atomically change that owner's claim to USED until token expiry.
 *   <li>release: delete only while the value is still RESERVED for the same attempt (so a token
 *       that was marked used in between is not freed).
 * </ul>
 *
 * Redis must run with AOF so USED keys survive a restart (docker/docker-compose.yml).
 *
 * <p>Verified against Redis by RedisAdaptersIT.
 */
public class RedisTokenReservation implements ITokenReservation {
  private static final String PREFIX = "tmc:qr:jti:";
  private static final DefaultRedisScript<Long> RESERVE =
      new DefaultRedisScript<>(
          "local value = redis.call('GET', KEYS[1]); "
              + "if not value then redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[1]); return 1 end; "
              + "if value == ARGV[1] or value == ARGV[3] then return 2 end; return 0",
          Long.class);
  private static final DefaultRedisScript<Long> RELEASE_IF_RESERVED =
      new DefaultRedisScript<>(
          "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) "
              + "else return 0 end",
          Long.class);
  private static final DefaultRedisScript<Long> MARK_USED =
      new DefaultRedisScript<>(
          "local value = redis.call('GET', KEYS[1]); "
              + "if not value or value == ARGV[1] or value == ARGV[2] then "
              + "redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2]); return 1 end; return 0",
          Long.class);

  private final StringRedisTemplate redis;
  private final IClock clock;

  public RedisTokenReservation(StringRedisTemplate redis, IClock clock) {
    this.redis = redis;
    this.clock = clock;
  }

  @Override
  public Reservation reserve(String jti, UUID attemptId, Duration ttl) {
    var owner = attemptId.toString();
    Long code =
        redis.execute(
            RESERVE,
            List.of(PREFIX + jti),
            "RESERVED:" + owner,
            Long.toString(Math.max(1, ttl.toMillis())),
            "USED:" + owner);
    if (Long.valueOf(1).equals(code)) return Reservation.ACQUIRED;
    if (Long.valueOf(2).equals(code)) return Reservation.SAME_ATTEMPT;
    return Reservation.OTHER_ATTEMPT;
  }

  @Override
  public boolean ownedBy(String jti, UUID attemptId) {
    var value = redis.opsForValue().get(PREFIX + jti);
    return ("RESERVED:" + attemptId).equals(value) || ("USED:" + attemptId).equals(value);
  }

  @Override
  public void release(String jti, UUID attemptId) {
    redis.execute(RELEASE_IF_RESERVED, List.of(PREFIX + jti), "RESERVED:" + attemptId);
  }

  @Override
  public void markUsed(String jti, UUID attemptId, Instant blockUntil) {
    var remaining = Duration.between(clock.now(), blockUntil);
    if (remaining.compareTo(Duration.ofSeconds(1)) < 0) {
      remaining = Duration.ofSeconds(1); // already past its window; keep it briefly, never forever
    }
    var owner = attemptId.toString();
    Long updated =
        redis.execute(
            MARK_USED,
            List.of(PREFIX + jti),
            "RESERVED:" + owner,
            "USED:" + owner,
            Long.toString(remaining.toMillis()));
    if (!Long.valueOf(1).equals(updated)) {
      throw new IllegalStateException("token is owned by another attempt");
    }
  }
}
