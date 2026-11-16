package vn.tmc.gatestation.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.gatestation.adapter.out.redis.RedisAntiPassbackStore;
import vn.tmc.gatestation.adapter.out.redis.RedisTokenReservation;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * RedisTokenReservation and RedisAntiPassbackStore against a real Redis (ADR-12 R1, FR-17): atomic
 * SET NX, TTLs and the Lua scripts. Needs Docker: ./gradlew :services:gate-station:integrationTest
 */
@Tag("integration")
@Testcontainers
class RedisAdaptersIT {
  @Container static final GenericContainer<?> REDIS = TmcContainers.redis();

  static LettuceConnectionFactory factory;
  static StringRedisTemplate redis;

  @BeforeAll
  static void connect() {
    factory =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
    factory.afterPropertiesSet();
    redis = new StringRedisTemplate(factory);
  }

  @AfterAll
  static void close() {
    factory.destroy();
  }

  @Test
  void should_let_exactly_one_of_many_concurrent_callers_reserve_a_token() throws Exception {
    var tokens = new RedisTokenReservation(redis, Instant::now);
    var jti = "jti-" + UUID.randomUUID();
    var pool = Executors.newFixedThreadPool(16);
    var calls = new ArrayList<Callable<Boolean>>();
    for (int i = 0; i < 32; i++) {
      calls.add(() -> tokens.reserve(jti, Duration.ofSeconds(10)));
    }
    int winners = 0;
    for (Future<Boolean> f : pool.invokeAll(calls)) {
      winners += f.get() ? 1 : 0;
    }
    pool.shutdown();

    assertThat(winners).isEqualTo(1);
  }

  @Test
  void should_release_a_reserved_token_but_never_a_used_one() {
    var tokens = new RedisTokenReservation(redis, Instant::now);
    var reserved = "jti-" + UUID.randomUUID();
    var used = "jti-" + UUID.randomUUID();
    tokens.reserve(reserved, Duration.ofSeconds(10));
    tokens.reserve(used, Duration.ofSeconds(10));
    tokens.markUsed(used, Instant.now().plusSeconds(60));

    tokens.release(reserved);
    tokens.release(used);

    assertThat(tokens.reserve(reserved, Duration.ofSeconds(10))).isTrue(); // free again
    assertThat(tokens.reserve(used, Duration.ofSeconds(10))).isFalse(); // stays blocked
    var ttl = redis.getExpire("tmc:qr:jti:" + used);
    assertThat(ttl).isBetween(50L, 60L);
  }

  @Test
  void should_expire_a_reservation_nobody_finished() throws Exception {
    var tokens = new RedisTokenReservation(redis, Instant::now);
    var jti = "jti-" + UUID.randomUUID();
    tokens.reserve(jti, Duration.ofSeconds(1));

    Thread.sleep(1500);

    assertThat(tokens.reserve(jti, Duration.ofSeconds(10))).isTrue();
  }

  @Test
  void should_keep_one_pending_pass_and_move_it_to_last_on_confirm() {
    var store = new RedisAntiPassbackStore(redis);
    var employee = UUID.randomUUID();
    var at = Instant.parse("2026-11-20T07:00:00.123Z");

    assertThat(store.last(employee)).isEmpty();
    assertThat(store.tryReserve(employee, Direction.ENTRY, at, Duration.ofSeconds(30))).isTrue();
    assertThat(store.tryReserve(employee, Direction.ENTRY, at, Duration.ofSeconds(30))).isFalse();

    store.confirm(employee);

    var last = store.last(employee).orElseThrow();
    assertThat(last.direction()).isEqualTo(Direction.ENTRY);
    assertThat(last.at()).isEqualTo(at);
    assertThat(redis.getExpire("tmc:apb:last:" + employee)).isEqualTo(-1L); // no expiry
    assertThat(store.tryReserve(employee, Direction.EXIT, at, Duration.ofSeconds(30))).isTrue();
  }

  @Test
  void should_release_and_extend_the_pending_slot_without_touching_last() {
    var store = new RedisAntiPassbackStore(redis);
    var employee = UUID.randomUUID();
    var at = Instant.now();
    store.tryReserve(employee, Direction.ENTRY, at, Duration.ofSeconds(30));
    store.confirm(employee);
    store.tryReserve(employee, Direction.EXIT, at, Duration.ofSeconds(5));

    store.keepPending(employee, Duration.ofSeconds(120));
    assertThat(redis.getExpire("tmc:apb:pending:" + employee)).isGreaterThan(100L);

    store.release(employee);
    assertThat(store.last(employee).orElseThrow().direction()).isEqualTo(Direction.ENTRY);
    assertThat(store.tryReserve(employee, Direction.EXIT, at, Duration.ofSeconds(5))).isTrue();
  }
}
