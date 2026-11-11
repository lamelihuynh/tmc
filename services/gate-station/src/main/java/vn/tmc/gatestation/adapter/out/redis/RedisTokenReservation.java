package vn.tmc.gatestation.adapter.out.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.platform.kernel.IClock;

/**
 * One-time use of a token id on the edge Redis (ADR-12 layer R1). Key {@code tmc:qr:jti:<jti>}:
 *
 * <ul>
 *   <li>reserve: {@code SET key RESERVED NX EX ttl}, atomic, so exactly one concurrent caller wins.
 *   <li>markUsed: {@code SET key USED EX (blockUntil - now)}.
 *   <li>release: delete only while the value is still RESERVED (one Lua script, so a token that was
 *       marked used in between is not freed).
 * </ul>
 *
 * Redis must run with AOF so USED keys survive a restart (docker/docker-compose.yml).
 *
 * <p>NOT yet run against a real Redis. TODO Team A (A-01): Testcontainers integration test.
 */
public class RedisTokenReservation implements ITokenReservation {
  private static final String PREFIX = "tmc:qr:jti:";
  private static final String RESERVED = "RESERVED";
  private static final String USED = "USED";
  private static final DefaultRedisScript<Long> RELEASE_IF_RESERVED =
      new DefaultRedisScript<>(
          "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) "
              + "else return 0 end",
          Long.class);

  private final StringRedisTemplate redis;
  private final IClock clock;

  public RedisTokenReservation(StringRedisTemplate redis, IClock clock) {
    this.redis = redis;
    this.clock = clock;
  }

  @Override
  public boolean reserve(String jti, Duration ttl) {
    return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(PREFIX + jti, RESERVED, ttl));
  }

  @Override
  public void release(String jti) {
    redis.execute(RELEASE_IF_RESERVED, List.of(PREFIX + jti), RESERVED);
  }

  @Override
  public void markUsed(String jti, Instant blockUntil) {
    var remaining = Duration.between(clock.now(), blockUntil);
    if (remaining.compareTo(Duration.ofSeconds(1)) < 0) {
      remaining = Duration.ofSeconds(1); // already past its window; keep it briefly, never forever
    }
    redis.opsForValue().set(PREFIX + jti, USED, remaining);
  }
}
