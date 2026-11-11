package vn.tmc.gatestation.adapter.out.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import vn.tmc.gatestation.application.port.out.IAntiPassbackStore;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.LastPass;

/**
 * Anti-passback state on the edge Redis. Two keys per employee, both holding {@code
 * <DIRECTION>|<epochMillis>}:
 *
 * <ul>
 *   <li>{@code tmc:apb:last:<employee>}: last confirmed pass, no expiry.
 *   <li>{@code tmc:apb:pending:<employee>}: the pass in flight, taken with SET NX EX so two scans
 *       of one person cannot both proceed; it expires by itself if nobody confirms or releases.
 * </ul>
 *
 * confirm moves pending to last in one Lua script.
 *
 * <p>NOT yet run against a real Redis. TODO Team A (A-01): Testcontainers integration test. TODO
 * Team A (A-03): rebuild "last" from Attendance when the key is missing (cold cache).
 */
public class RedisAntiPassbackStore implements IAntiPassbackStore {
  private static final String LAST = "tmc:apb:last:";
  private static final String PENDING = "tmc:apb:pending:";
  private static final DefaultRedisScript<Long> CONFIRM =
      new DefaultRedisScript<>(
          "local v = redis.call('GET', KEYS[1]) "
              + "if v then redis.call('SET', KEYS[2], v) redis.call('DEL', KEYS[1]) return 1 "
              + "else return 0 end",
          Long.class);

  private final StringRedisTemplate redis;

  public RedisAntiPassbackStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Optional<LastPass> last(UUID employeeUuid) {
    return Optional.ofNullable(redis.opsForValue().get(LAST + employeeUuid))
        .map(RedisAntiPassbackStore::decode);
  }

  @Override
  public boolean tryReserve(
      UUID employeeUuid, Direction direction, Instant scannedAt, Duration pendingTtl) {
    return Boolean.TRUE.equals(
        redis
            .opsForValue()
            .setIfAbsent(PENDING + employeeUuid, encode(direction, scannedAt), pendingTtl));
  }

  @Override
  public void confirm(UUID employeeUuid) {
    redis.execute(CONFIRM, List.of(PENDING + employeeUuid, LAST + employeeUuid));
  }

  @Override
  public void release(UUID employeeUuid) {
    redis.delete(PENDING + employeeUuid);
  }

  @Override
  public void keepPending(UUID employeeUuid, Duration ttl) {
    redis.expire(PENDING + employeeUuid, ttl);
  }

  private static String encode(Direction direction, Instant at) {
    return direction.name() + "|" + at.toEpochMilli();
  }

  private static LastPass decode(String value) {
    int sep = value.indexOf('|');
    return new LastPass(
        Direction.valueOf(value.substring(0, sep)),
        Instant.ofEpochMilli(Long.parseLong(value.substring(sep + 1))));
  }
}
