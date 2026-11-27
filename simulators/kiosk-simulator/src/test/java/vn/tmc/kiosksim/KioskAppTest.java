package vn.tmc.kiosksim;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.tmc.platform.kernel.IClock;

class KioskAppTest {

  static class FakeEdge implements EdgeClient {
    boolean reachable = true;
    String outcomeForScan = "ALLOWED";
    final List<UUID> scanAttemptIds = new ArrayList<>();
    final List<String> scanTokens = new ArrayList<>();
    final Set<UUID> known = new HashSet<>();
    final List<OfflineLine> batches = new ArrayList<>();
    SyncBundle bundle = new SyncBundle(List.of(), List.of());

    /** Answer for a sync since EPOCH; null means the same as {@link #bundle}. */
    SyncBundle fullBundle;

    final List<Instant> syncSince = new ArrayList<>();

    @Override
    public ScanResponse scan(UUID id, String gate, String token, Instant at, long seq, String kiosk)
        throws EdgeUnreachableException {
      if (!reachable) {
        throw new EdgeUnreachableException("down", null);
      }

      scanAttemptIds.add(id);
      scanTokens.add(token);
      boolean duplicate = !known.add(id);
      return new ScanResponse(id, outcomeForScan, null, "ENTRY", duplicate);
    }

    @Override
    public SyncBundle sync(Instant since) throws EdgeUnreachableException {
      if (!reachable) {
        throw new EdgeUnreachableException("down", null);
      }
      syncSince.add(since);
      return Instant.EPOCH.equals(since) && fullBundle != null ? fullBundle : bundle;
    }

    @Override
    public List<OfflineResult> offlineBatch(String kiosk, List<OfflineLine> lines)
        throws EdgeUnreachableException {
      if (!reachable) {
        throw new EdgeUnreachableException("down", null);
      }

      batches.addAll(lines);
      return lines.stream()
          .map(
              line ->
                  new OfflineResult(
                      line.attemptId(),
                      known.add(line.attemptId()) ? "ACCEPTED" : "DUPLICATE",
                      null))
          .toList();
    }
  }

  static class MutableClock implements IClock {
    Instant now = Instant.parse("2026-11-09T06:45:00Z");

    @Override
    public Instant now() {
      return now;
    }
  }

  @TempDir Path dir;

  FakeEdge edge = new FakeEdge();
  MutableClock clock = new MutableClock();
  Journal journal;
  KioskApp kiosk;
  final List<String> localOpens = new ArrayList<>();

  KeyPair phoneKeys;
  final UUID employee = UUID.fromString("0192a1c0-0000-7000-8000-000000000001");
  final String deviceId = "dev-1";

  @BeforeEach
  void setUp() throws Exception {
    journal = new Journal(dir.resolve("kiosk.db").toString());
    kiosk =
        new KioskApp(
            journal,
            edge,
            KioskApp.defaultVerifier(journal),
            (gate, direction) -> localOpens.add(gate + ":" + direction),
            clock,
            "BD-KIOSK-01",
            2);

    var gen = KeyPairGenerator.getInstance("EC");
    gen.initialize(new ECGenParameterSpec("secp256r1"));
    phoneKeys = gen.generateKeyPair();
    syncedWithKey(false);
  }

  @AfterEach
  void closeJournal() {
    if (journal != null) {
      journal.close();
    }
  }

  private void syncedWithKey(boolean revoked) {
    journal.saveDeviceKey(
        new Journal.DeviceKey(
            deviceId,
            employee.toString(),
            Base64.getEncoder().encodeToString(phoneKeys.getPublic().getEncoded()),
            revoked));
    journal.saveGate(new Journal.Gate("BD-GATE-01", "ENTRY_ONLY"));
    journal.saveGate(new Journal.Gate("BD-GATE-03", "BIDIRECTIONAL"));
  }

  private String token(String jti, Instant iat, Instant exp) throws Exception {
    var claims =
        new JWTClaimsSet.Builder()
            .subject(employee.toString())
            .jwtID(jti)
            .issueTime(Date.from(iat))
            .expirationTime(Date.from(exp))
            .build();
    var jwt =
        new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(deviceId).build(), claims);
    jwt.sign(new ECDSASigner((ECPrivateKey) phoneKeys.getPrivate()));
    return jwt.serialize();
  }

  private String fresh(String jti) throws Exception {
    return token(jti, clock.now(), clock.now().plusSeconds(30));
  }

  @Test
  void should_journal_before_asking_the_edge_and_hand_the_row_over_when_the_edge_answers()
      throws Exception {
    var result = kiosk.scan(fresh("j1"), "BD-GATE-01");

    assertThat(result.outcome()).isEqualTo("ALLOWED");
    assertThat(result.offline()).isFalse();
    var row = journal.find(result.attemptId()).orElseThrow();
    assertThat(row.status()).isEqualTo("OPENED");
    assertThat(row.handedOver()).isTrue();
    assertThat(localOpens).isEmpty();
  }

  @Test
  void should_never_store_the_raw_token_in_the_journal() throws Exception {
    var token = fresh("j-secret");
    kiosk.scan(token, "BD-GATE-01");

    var row = journal.all().get(0);
    assertThat(row.tokenHash()).isEqualTo(KioskApp.sha256(token)).doesNotContain(token);
    assertThat(row.toString()).doesNotContain(token);
  }

  @Test
  void should_record_a_denial_from_the_edge_without_opening_anything() throws Exception {
    edge.outcomeForScan = "DENIED";

    var result = kiosk.scan(fresh("j2"), "BD-GATE-01");

    assertThat(result.outcome()).isEqualTo("DENIED");
    assertThat(journal.find(result.attemptId()).orElseThrow().status()).isEqualTo("DENIED");
    assertThat(localOpens).isEmpty();
  }

  @Test
  void should_retry_with_the_same_attempt_id_when_the_edge_does_not_answer() throws Exception {
    var edgeWithFlakyStart =
        new FakeEdge() {
          int calls;

          @Override
          public ScanResponse scan(UUID id, String g, String t, Instant at, long seq, String k)
              throws EdgeUnreachableException {
            if (calls++ < 2) {
              known.add(id);
              scanAttemptIds.add(id);
              throw new EdgeUnreachableException("lost", null);
            }
            return super.scan(id, g, t, at, seq, k);
          }
        };
    var app =
        new KioskApp(
            journal,
            edgeWithFlakyStart,
            KioskApp.defaultVerifier(journal),
            (g, d) -> localOpens.add(g),
            clock,
            "K",
            2);

    var result = app.scan(fresh("j3"), "BD-GATE-01");

    assertThat(result.offline()).isFalse();
    assertThat(edgeWithFlakyStart.scanAttemptIds).hasSize(3).containsOnly(result.attemptId());
    assertThat(result.outcome()).isEqualTo("ALLOWED");
    assertThat(localOpens).isEmpty();
  }

  @Test
  void should_ask_again_with_the_same_attempt_id_while_the_edge_says_in_progress()
      throws Exception {
    var slowEdge =
        new FakeEdge() {
          int calls;

          @Override
          public ScanResponse scan(UUID id, String g, String t, Instant at, long seq, String k) {
            scanAttemptIds.add(id);
            return new ScanResponse(
                id, calls++ < 2 ? "IN_PROGRESS" : "ALLOWED", null, "ENTRY", calls > 1);
          }
        };
    var app =
        new KioskApp(
            journal,
            slowEdge,
            KioskApp.defaultVerifier(journal),
            (g, d) -> localOpens.add(g),
            clock,
            "K",
            0,
            d -> {});

    var result = app.scan(fresh("poll"), "BD-GATE-01");

    assertThat(result.outcome()).isEqualTo("ALLOWED");
    assertThat(slowEdge.scanAttemptIds).hasSize(3).containsOnly(result.attemptId());
    assertThat(journal.find(result.attemptId()).orElseThrow().status()).isEqualTo("OPENED");
    assertThat(localOpens).isEmpty();
  }

  @Test
  void should_settle_for_pending_when_the_edge_never_finishes_and_never_open_locally()
      throws Exception {
    var stuckEdge =
        new FakeEdge() {
          @Override
          public ScanResponse scan(UUID id, String g, String t, Instant at, long seq, String k) {
            scanAttemptIds.add(id);
            return new ScanResponse(id, "IN_PROGRESS", "IN_PROGRESS", null, true);
          }
        };
    var app =
        new KioskApp(
            journal,
            stuckEdge,
            KioskApp.defaultVerifier(journal),
            (g, d) -> localOpens.add(g),
            clock,
            "K",
            2,
            d -> {});

    var result = app.scan(fresh("stuck"), "BD-GATE-01");

    assertThat(result.outcome()).isEqualTo("IN_PROGRESS");
    assertThat(stuckEdge.scanAttemptIds).containsOnly(result.attemptId());
    assertThat(journal.find(result.attemptId()).orElseThrow().status()).isEqualTo("PENDING");
    assertThat(localOpens).isEmpty();
  }

  @Test
  void d15_should_deny_an_inactive_employee_offline() throws Exception {
    edge.bundle =
        new EdgeClient.SyncBundle(
            List.of(),
            List.of(),
            List.of(new EdgeClient.Employee(employee, "INACTIVE", List.of("BD"))));
    kiosk.sync();
    edge.reachable = false;

    var result = kiosk.scan(fresh("d15-inactive"), "BD-GATE-01");

    assertThat(result.offline()).isTrue();
    assertThat(result.outcome()).isEqualTo("DENIED");
    assertThat(result.reason()).isEqualTo("EMPLOYEE_INACTIVE");
    assertThat(localOpens).isEmpty();

    var row = journal.find(result.attemptId()).orElseThrow();
    assertThat(row.status()).isEqualTo("OFFLINE_DENIED");
    assertThat(row.result()).isEqualTo("EMPLOYEE_INACTIVE");
  }

  @Test
  void d15_should_deny_at_la_and_allow_at_bd_for_a_bd_employee() throws Exception {
    edge.bundle =
        new EdgeClient.SyncBundle(
            List.of(new Journal.Gate("LA-GATE-01", "ENTRY_ONLY")),
            List.of(),
            List.of(new EdgeClient.Employee(employee, "ACTIVE", List.of("BD"))));
    kiosk.sync();
    edge.reachable = false;

    var denied = kiosk.scan(fresh("d15-wrong-factory"), "LA-GATE-01");

    assertThat(denied.offline()).isTrue();
    assertThat(denied.outcome()).isEqualTo("DENIED");
    assertThat(denied.reason()).isEqualTo("FACTORY_NOT_PERMITTED");
    assertThat(localOpens).isEmpty();

    var row = journal.find(denied.attemptId()).orElseThrow();
    assertThat(row.status()).isEqualTo("OFFLINE_DENIED");
    assertThat(row.result()).isEqualTo("FACTORY_NOT_PERMITTED");

    var allowed = kiosk.scan(fresh("d15-right-factory"), "BD-GATE-01");

    assertThat(allowed.offline()).isTrue();
    assertThat(allowed.outcome()).isEqualTo("ALLOWED");
    assertThat(localOpens).containsExactly("BD-GATE-01:ENTRY");
  }

  @Test
  void d15_should_allow_a_valid_scan_with_a_legacy_bundle() throws Exception {
    edge.bundle = new EdgeClient.SyncBundle(List.of(), List.of());
    kiosk.sync();
    edge.reachable = false;

    var result = kiosk.scan(fresh("d15-legacy"), "BD-GATE-01");

    assertThat(journal.hasEmployeeDirectory()).isFalse();
    assertThat(result.offline()).isTrue();
    assertThat(result.outcome()).isEqualTo("ALLOWED");
    assertThat(localOpens).containsExactly("BD-GATE-01:ENTRY");
  }

  @Test
  void should_verify_locally_open_through_the_local_link_and_journal_when_the_edge_stays_down()
      throws Exception {
    edge.reachable = false;

    var result = kiosk.scan(fresh("j4"), "BD-GATE-01");

    assertThat(result.offline()).isTrue();
    assertThat(result.outcome()).isEqualTo("ALLOWED");
    assertThat(result.direction()).isEqualTo("ENTRY");
    assertThat(localOpens).containsExactly("BD-GATE-01:ENTRY");
    var row = journal.find(result.attemptId()).orElseThrow();
    assertThat(row.status()).isEqualTo("OFFLINE_OPENED");
    assertThat(row.handedOver()).isFalse();
    assertThat(row.employeeUuid()).isEqualTo(employee.toString());
  }

  @Test
  void should_refuse_the_same_token_twice_offline_using_the_local_replay_cache() throws Exception {
    edge.reachable = false;
    var token = fresh("j-replay");

    var first = kiosk.scan(token, "BD-GATE-01");
    var second = kiosk.scan(token, "BD-GATE-01");

    assertThat(first.outcome()).isEqualTo("ALLOWED");
    assertThat(second.outcome()).isEqualTo("DENIED");
    assertThat(second.reason()).isEqualTo("QR_ALREADY_USED");
    assertThat(localOpens).hasSize(1);
  }

  @Test
  void should_allow_the_token_again_after_the_replay_window_has_passed() throws Exception {
    edge.reachable = false;
    kiosk.scan(token("j-w", clock.now(), clock.now().plusSeconds(30)), "BD-GATE-01");
    clock.now = clock.now.plus(Duration.ofMinutes(3));

    var later = token("j-w", clock.now(), clock.now().plusSeconds(30));

    assertThat(kiosk.scan(later, "BD-GATE-01").outcome()).isEqualTo("ALLOWED");
  }

  @Test
  void should_not_open_offline_when_no_keys_were_ever_synced() throws Exception {
    edge.reachable = false;

    try (var emptyJournal = new Journal(dir.resolve("empty.db").toString())) {
      var app =
          new KioskApp(
              emptyJournal,
              edge,
              KioskApp.defaultVerifier(emptyJournal),
              (g, d) -> localOpens.add(g),
              clock,
              "K",
              0);

      var result = app.scan(fresh("j5"), "BD-GATE-01");

      assertThat(result.outcome()).isEqualTo("DENIED");
      assertThat(result.reason()).isEqualTo("DEVICE_UNKNOWN");
      assertThat(localOpens).isEmpty();
    }
  }

  @Test
  void should_not_open_offline_for_a_device_revoked_in_the_last_sync() throws Exception {
    edge.reachable = false;
    syncedWithKey(true);

    var result = kiosk.scan(fresh("j6"), "BD-GATE-01");

    assertThat(result.reason()).isEqualTo("DEVICE_REVOKED");
    assertThat(localOpens).isEmpty();
  }

  @Test
  void should_not_open_offline_for_an_expired_tampered_or_garbage_token() throws Exception {
    edge.reachable = false;
    var expired = token("j7", clock.now().minusSeconds(300), clock.now().minusSeconds(270));
    var tampered = fresh("j8");
    var parts = tampered.split("\\.");
    var forged =
        parts[0]
            + "."
            + parts[1]
            + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[64]);

    assertThat(kiosk.scan(expired, "BD-GATE-01").reason()).isEqualTo("QR_EXPIRED");
    assertThat(kiosk.scan(forged, "BD-GATE-01").reason()).isEqualTo("INVALID_SIGNATURE");
    assertThat(kiosk.scan("garbage", "BD-GATE-01").reason()).isEqualTo("MALFORMED_TOKEN");
    assertThat(localOpens).isEmpty();
  }

  @Test
  void should_alternate_direction_offline_on_a_bidirectional_gate() throws Exception {
    edge.reachable = false;

    var in = kiosk.scan(fresh("a"), "BD-GATE-03");
    var out = kiosk.scan(fresh("b"), "BD-GATE-03");

    assertThat(in.direction()).isEqualTo("ENTRY");
    assertThat(out.direction()).isEqualTo("EXIT");
  }

  @Test
  void should_deny_offline_when_the_gate_is_not_in_the_cache() throws Exception {
    edge.reachable = false;

    assertThat(kiosk.scan(fresh("j9"), "UNKNOWN-GATE").reason()).isEqualTo("GATE_NOT_FOUND");
  }

  @Test
  void should_stay_offline_while_forced_even_if_the_edge_is_up() throws Exception {
    kiosk.setForcedOffline(true);

    var result = kiosk.scan(fresh("j10"), "BD-GATE-01");

    assertThat(result.offline()).isTrue();
    assertThat(edge.scanAttemptIds).isEmpty();
  }

  @Test
  void should_hand_over_offline_rows_in_journal_order_when_the_edge_returns() throws Exception {
    edge.reachable = false;
    var a = kiosk.scan(fresh("f1"), "BD-GATE-03");
    var b = kiosk.scan(fresh("f2"), "BD-GATE-03");
    edge.reachable = true;

    var report = kiosk.flush();

    assertThat(report.handedOver()).isEqualTo(2);
    assertThat(report.stillPending()).isZero();
    assertThat(edge.batches)
        .extracting(EdgeClient.OfflineLine::attemptId)
        .containsExactly(a.attemptId(), b.attemptId());
    assertThat(edge.batches).extracting(EdgeClient.OfflineLine::journalSeq).isSorted();
    assertThat(journal.pendingOfflineHandover()).isEmpty();
  }

  @Test
  void should_keep_everything_in_the_journal_if_the_edge_is_still_down_on_flush() throws Exception {
    edge.reachable = false;
    kiosk.scan(fresh("f3"), "BD-GATE-01");

    var report = kiosk.flush();

    assertThat(report.handedOver()).isZero();
    assertThat(report.stillPending()).isEqualTo(1);
  }

  @Test
  void should_not_double_hand_over_when_flush_runs_twice() throws Exception {
    edge.reachable = false;
    kiosk.scan(fresh("f4"), "BD-GATE-01");
    edge.reachable = true;

    kiosk.flush();
    kiosk.flush();

    assertThat(edge.batches).hasSize(1);
  }

  @Test
  void should_retry_an_unfinished_scan_with_the_hash_never_the_raw_token() throws Exception {
    var id = UUID.randomUUID();
    var token = fresh("f5");
    journal.append(id, "BD-GATE-01", KioskApp.sha256(token), clock.now());

    var report = kiosk.flush();

    assertThat(report.retriedUnfinished()).isEqualTo(1);
    assertThat(edge.scanAttemptIds).containsExactly(id);
    assertThat(edge.scanTokens.get(0)).isEqualTo(KioskApp.sha256(token)).isNotEqualTo(token);
  }

  @Test
  void should_load_keys_and_gates_from_a_sync_bundle() throws Exception {
    try (var other = new Journal(dir.resolve("fresh.db").toString())) {
      edge.bundle =
          new EdgeClient.SyncBundle(
              List.of(new Journal.Gate("BD-GATE-01", "ENTRY_ONLY")),
              List.of(new Journal.DeviceKey("dev-9", employee.toString(), "AAAA", false)));
      var app =
          new KioskApp(other, edge, KioskApp.defaultVerifier(other), (g, d) -> {}, clock, "K", 0);

      app.sync();

      assertThat(other.deviceKey("dev-9")).isPresent();
      assertThat(other.gate("BD-GATE-01")).isPresent();
    }
  }

  @Test
  void should_fetch_the_whole_directory_when_an_upgraded_kiosk_has_none_yet() throws Exception {
    // a kiosk that synced before the directory existed: it has a cursor but no directory
    journal.setMeta("last_sync", clock.now().minusSeconds(3600).toString());
    edge.bundle = new EdgeClient.SyncBundle(List.of(), List.of(), List.of()); // nothing changed
    edge.fullBundle =
        new EdgeClient.SyncBundle(
            List.of(),
            List.of(),
            List.of(new EdgeClient.Employee(employee, "ACTIVE", List.of("BD"))));

    kiosk.sync();
    edge.reachable = false;
    var result = kiosk.scan(fresh("upgrade"), "BD-GATE-01");

    assertThat(edge.syncSince).containsExactly(Instant.EPOCH);
    assertThat(journal.hasEmployeeDirectory()).isTrue();
    assertThat(result.outcome()).isEqualTo("ALLOWED");
  }

  @Test
  void should_mark_the_directory_present_only_after_a_complete_bundle() throws Exception {
    try (var other = new Journal(dir.resolve("incremental.db").toString())) {
      other.saveEmployees(
          List.of(new EdgeClient.Employee(employee, "ACTIVE", List.of("BD"))), false);
      assertThat(other.hasEmployeeDirectory()).isFalse();

      other.saveEmployees(List.of(), true);
      assertThat(other.hasEmployeeDirectory()).isTrue();
    }
  }

  @Test
  void should_use_the_edge_time_as_the_next_sync_cursor() throws Exception {
    var generatedAt = Instant.parse("2026-11-27T03:00:00Z");
    edge.bundle =
        new EdgeClient.SyncBundle(
            List.of(),
            List.of(),
            List.of(new EdgeClient.Employee(employee, "ACTIVE", List.of("BD"))),
            generatedAt);

    kiosk.sync();
    kiosk.sync();

    assertThat(edge.syncSince).containsExactly(Instant.EPOCH, generatedAt);
  }

  @Test
  void should_take_the_factory_of_a_gate_from_the_bundle_not_its_name() throws Exception {
    edge.bundle =
        new EdgeClient.SyncBundle(
            List.of(new Journal.Gate("NORTH-1", "ENTRY_ONLY", "BD")),
            List.of(),
            List.of(new EdgeClient.Employee(employee, "ACTIVE", List.of("BD"))));
    kiosk.sync();
    edge.reachable = false;

    var result = kiosk.scan(fresh("named-gate"), "NORTH-1");

    assertThat(result.outcome()).isEqualTo("ALLOWED");
    assertThat(journal.gate("NORTH-1").orElseThrow().factoryCode()).isEqualTo("BD");
  }

  @Test
  void should_add_the_factory_column_to_a_journal_from_an_older_version() throws Exception {
    var path = dir.resolve("old.db").toString();
    try (var c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + path);
        var st = c.createStatement()) {
      st.execute("CREATE TABLE gates (gate_id TEXT PRIMARY KEY, mode TEXT NOT NULL)");
      st.execute("INSERT INTO gates VALUES ('LA-GATE-02', 'BIDIRECTIONAL')");
    }

    try (var upgraded = new Journal(path)) {
      var gate = upgraded.gate("LA-GATE-02").orElseThrow();
      assertThat(gate.factoryCode()).isNull();
      assertThat(gate.factory()).isEqualTo("LA");
      upgraded.saveGate(new Journal.Gate("LA-GATE-02", "ENTRY_ONLY", "LA"));
      assertThat(upgraded.gate("LA-GATE-02").orElseThrow().factoryCode()).isEqualTo("LA");
    }
  }
}
