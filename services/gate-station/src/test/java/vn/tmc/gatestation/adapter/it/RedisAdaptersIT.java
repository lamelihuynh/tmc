package vn.tmc.gatestation.adapter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.dockerjava.api.model.ExposedPort;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.tmc.gatestation.adapter.out.inmem.FakeAttendanceGateway;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryHeartbeatStore;
import vn.tmc.gatestation.adapter.out.inmem.StaticKioskSyncSource;
import vn.tmc.gatestation.adapter.out.redis.RedisAntiPassbackStore;
import vn.tmc.gatestation.adapter.out.redis.RedisTokenReservation;
import vn.tmc.gatestation.application.port.in.IGateAccess.Outcome;
import vn.tmc.gatestation.application.port.in.IKioskSupport.OfflineEntry;
import vn.tmc.gatestation.application.port.in.IKioskSupport.OfflineOutcome;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.AttemptRecord;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway.AttemptState;
import vn.tmc.gatestation.application.port.out.ITokenReservation.Reservation;
import vn.tmc.gatestation.application.usecase.KioskSupportService;
import vn.tmc.gatestation.application.usecase.RecordScanUseCase;
import vn.tmc.gatestation.application.usecase.ScanSettings;
import vn.tmc.gatestation.domain.Direction;
import vn.tmc.gatestation.domain.LastPass;
import vn.tmc.gatestation.domain.policy.AccessPolicyChain;
import vn.tmc.gatestation.support.GateFixture;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.kernel.ReasonCode;
import vn.tmc.platform.testsupport.TmcContainers;

/**
 * RedisTokenReservation and RedisAntiPassbackStore against a real Redis (ADR-12 R1, FR-17): atomic
 * SET NX, TTLs and the Lua scripts. Needs Docker: ./gradlew :services:gate-station:integrationTest
 */
@Tag("integration")
@Testcontainers
class RedisAdaptersIT {
  @Container
  static final GenericContainer<?> REDIS =
      TmcContainers.redis().withCommand("redis-server", "--appendonly", "yes");

  static LettuceConnectionFactory factory;
  static StringRedisTemplate redis;

  @BeforeAll
  static void connect() {
    connect(REDIS.getMappedPort(6379));
  }

  static void connect(int port) {
    factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(REDIS.getHost(), port));
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
    var calls = new ArrayList<Callable<Reservation>>();
    for (int i = 0; i < 32; i++) {
      var attemptId = UUID.randomUUID();
      calls.add(() -> tokens.reserve(jti, attemptId, Duration.ofSeconds(10)));
    }
    int winners = 0;
    for (Future<Reservation> f : pool.invokeAll(calls)) {
      winners += f.get() == Reservation.ACQUIRED ? 1 : 0;
    }
    pool.shutdown();

    assertThat(winners).isEqualTo(1);
  }

  @Test
  void should_release_a_reserved_token_but_never_a_used_one() {
    var tokens = new RedisTokenReservation(redis, Instant::now);
    var reserved = "jti-" + UUID.randomUUID();
    var used = "jti-" + UUID.randomUUID();
    var owner = UUID.randomUUID();
    tokens.reserve(reserved, owner, Duration.ofSeconds(10));
    tokens.reserve(used, owner, Duration.ofSeconds(10));
    tokens.markUsed(used, owner, Instant.now().plusSeconds(60));
    assertThat(tokens.ownedBy(reserved, owner)).isTrue();
    assertThat(tokens.ownedBy(used, owner)).isTrue();
    assertThat(tokens.ownedBy(used, UUID.randomUUID())).isFalse();

    tokens.release(reserved, UUID.randomUUID()); // another attempt cannot release it
    assertThat(tokens.reserve(reserved, UUID.randomUUID(), Duration.ofSeconds(10)))
        .isEqualTo(Reservation.OTHER_ATTEMPT);
    tokens.release(reserved, owner);
    tokens.release(used, owner);

    assertThat(tokens.reserve(reserved, UUID.randomUUID(), Duration.ofSeconds(10)))
        .isEqualTo(Reservation.ACQUIRED);
    assertThat(tokens.reserve(used, owner, Duration.ofSeconds(10)))
        .isEqualTo(Reservation.SAME_ATTEMPT);
    assertThat(tokens.reserve(used, UUID.randomUUID(), Duration.ofSeconds(10)))
        .isEqualTo(Reservation.OTHER_ATTEMPT);
    var ttl = redis.getExpire("tmc:qr:jti:" + used);
    assertThat(ttl).isBetween(50L, 60L);
  }

  @Test
  void should_keep_a_used_token_blocked_after_redis_restarts_with_aof() {
    var tokens = new RedisTokenReservation(redis, Instant::now);
    var jti = "jti-" + UUID.randomUUID();
    var owner = UUID.randomUUID();
    assertThat(tokens.reserve(jti, owner, Duration.ofMinutes(3))).isEqualTo(Reservation.ACQUIRED);
    tokens.markUsed(jti, owner, Instant.now().plusSeconds(180));

    DockerClientFactory.instance().client().restartContainerCmd(REDIS.getContainerId()).exec();
    factory.destroy();
    // Docker Desktop may assign a new random host port when the same container restarts.
    var bindings =
        DockerClientFactory.instance()
            .client()
            .inspectContainerCmd(REDIS.getContainerId())
            .exec()
            .getNetworkSettings()
            .getPorts()
            .getBindings()
            .get(ExposedPort.tcp(6379));
    connect(Integer.parseInt(bindings[0].getHostPortSpec()));

    await()
        .atMost(Duration.ofSeconds(30))
        .ignoreExceptions()
        .untilAsserted(
            () ->
                assertThat(redis.opsForValue().get("tmc:qr:jti:" + jti))
                    .isEqualTo("USED:" + owner));
    var reconnected = new RedisTokenReservation(redis, Instant::now);
    assertThat(reconnected.reserve(jti, UUID.randomUUID(), Duration.ofMinutes(3)))
        .isEqualTo(Reservation.OTHER_ATTEMPT);
  }

  @Test
  void should_expire_a_reservation_nobody_finished() throws Exception {
    var tokens = new RedisTokenReservation(redis, Instant::now);
    var jti = "jti-" + UUID.randomUUID();
    tokens.reserve(jti, UUID.randomUUID(), Duration.ofSeconds(1));

    Thread.sleep(1500);

    assertThat(tokens.reserve(jti, UUID.randomUUID(), Duration.ofSeconds(10)))
        .isEqualTo(Reservation.ACQUIRED);
  }

  @Test
  void should_replay_a_committed_attempt_after_a_retry_in_the_redis_claim_gap() throws Exception {
    var f = new GateFixture();
    var jti = "jti-race-" + UUID.randomUUID();
    f.token("race", GateFixture.E1, jti);
    var request = f.request("BD-GATE-01", "race");
    var enteredRecord = new CountDownLatch(1);
    var finishRecord = new CountDownLatch(1);
    var attendance =
        new FakeAttendanceGateway() {
          @Override
          public AttemptState recordAttempt(AttemptRecord record) {
            enteredRecord.countDown();
            try {
              if (!finishRecord.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting to record attempt");
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new IllegalStateException(e);
            }
            return super.recordAttempt(record);
          }
        };
    var scan =
        new RecordScanUseCase(
            f.gates,
            f.qr,
            f.employees,
            AccessPolicyChain.standard(),
            new RedisTokenReservation(redis, f.clock),
            f.antiPassback,
            attendance,
            f.turnstile,
            ScanSettings.defaults(),
            f.clock);
    var pool = Executors.newSingleThreadExecutor();
    try {
      var first = pool.submit(() -> scan.scan(request));
      assertThat(enteredRecord.await(30, TimeUnit.SECONDS)).isTrue();
      var retry = scan.scan(request);
      assertThat(retry.outcome()).isEqualTo(Outcome.IN_PROGRESS);
      assertThat(retry.duplicate()).isTrue();
      assertThat(f.turnstile.openCalls()).isZero();

      finishRecord.countDown();
      assertThat(first.get(30, TimeUnit.SECONDS).outcome()).isEqualTo(Outcome.ALLOWED);
      var replay = scan.scan(request);
      assertThat(replay.outcome()).isEqualTo(Outcome.ALLOWED);
      assertThat(replay.duplicate()).isTrue();
      assertThat(f.turnstile.openCalls()).isEqualTo(1);
    } finally {
      finishRecord.countDown();
      pool.shutdownNow();
    }
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

  @Test
  void should_not_replace_a_newer_last_pass_with_an_older_restored_or_pending_pass() {
    var store = new RedisAntiPassbackStore(redis);
    var employee = UUID.randomUUID();
    var older = Instant.parse("2026-11-20T07:00:00Z");
    var newer = older.plusSeconds(30);

    store.rememberIfNewer(employee, new LastPass(Direction.EXIT, newer));
    store.rememberIfNewer(employee, new LastPass(Direction.ENTRY, older));
    assertThat(store.last(employee)).contains(new LastPass(Direction.EXIT, newer));

    store.tryReserve(employee, Direction.ENTRY, older, Duration.ofSeconds(30));
    store.confirm(employee);
    assertThat(store.last(employee)).contains(new LastPass(Direction.EXIT, newer));
  }

  @Test
  void should_block_an_online_entry_after_an_offline_batch_updates_redis() {
    var f = new GateFixture();
    var store = new RedisAntiPassbackStore(redis);
    var support =
        new KioskSupportService(
            f.gates,
            new StaticKioskSyncSource(),
            new InMemoryHeartbeatStore(),
            f.attendance,
            store,
            f.clock);
    var at = f.clock.now().minusSeconds(1);
    var entry =
        new OfflineEntry(
            Ids.uuidV7(),
            1,
            GateFixture.E1,
            "BD-GATE-01",
            Direction.ENTRY,
            "offline-" + UUID.randomUUID(),
            at,
            "OFFLINE_OPENED",
            null);

    var results = support.ingestOffline("BD-KIOSK-01", List.of(entry));
    assertThat(results.getFirst().outcome()).isEqualTo(OfflineOutcome.ACCEPTED);
    assertThat(store.last(GateFixture.E1)).contains(new LastPass(Direction.ENTRY, at));

    f.token("online", GateFixture.E1, "online-" + UUID.randomUUID());
    var scan =
        new RecordScanUseCase(
            f.gates,
            f.qr,
            f.employees,
            AccessPolicyChain.standard(),
            f.tokens,
            store,
            f.attendance,
            f.turnstile,
            ScanSettings.defaults(),
            f.clock);
    var online = scan.scan(f.request("BD-GATE-01", "online"));
    assertThat(online.outcome()).isEqualTo(Outcome.DENIED);
    assertThat(online.reason()).isEqualTo(ReasonCode.ANTI_PASSBACK_VIOLATION);
    assertThat(f.turnstile.openCalls()).isZero();
  }
}
