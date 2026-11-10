package vn.tmc.kiosksim;

import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.kernel.Ids;

/**
 * Kiosk behavior from ADR-11 and ADR-12:
 *
 * <ol>
 *   <li>write the scan to the journal BEFORE touching the network (write-ahead, new attempt_id per
 *       scan)
 *   <li>ask the edge; on no answer retry the SAME attempt_id (the edge dedupes, so a lost reply
 *       cannot double count)
 *   <li>only if the edge stays unreachable, verify locally and open through the local gate link
 *   <li>when the network returns, hand every undelivered journal row to the edge in journal order
 * </ol>
 */
public class KioskApp {

  /** The physical fallback path (relay / RS-485 / TCP), document.md section 14.5 layer 3. */
  public interface LocalGate {
    void open(String gateId, String direction);
  }

  /** Pauses between polls; replaced in tests so they stay fast. */
  public interface Sleeper {
    void sleep(Duration d) throws InterruptedException;
  }

  public record Result(
      UUID attemptId, String outcome, String reason, String direction, boolean offline) {}

  public record FlushReport(int retriedUnfinished, int handedOver, int stillPending) {}

  private final Journal journal;
  private final EdgeClient edge;
  private final OfflineVerifier verifier;
  private final LocalGate localGate;
  private final IClock clock;
  private final String kioskId;
  private final int retries;
  private final Sleeper sleeper;

  /**
   * How many times a still-running attempt is asked about again before the kiosk settles for
   * "pending".
   */
  private static final int IN_PROGRESS_POLLS = 6;

  private static final Duration POLL_PAUSE = Duration.ofMillis(300);

  public KioskApp(
      Journal journal,
      EdgeClient edge,
      OfflineVerifier verifier,
      LocalGate localGate,
      IClock clock,
      String kioskId,
      int retries) {
    this(
        journal,
        edge,
        verifier,
        localGate,
        clock,
        kioskId,
        retries,
        d -> Thread.sleep(d.toMillis()));
  }

  public KioskApp(
      Journal journal,
      EdgeClient edge,
      OfflineVerifier verifier,
      LocalGate localGate,
      IClock clock,
      String kioskId,
      int retries,
      Sleeper sleeper) {
    this.sleeper = sleeper;
    this.journal = journal;
    this.edge = edge;
    this.verifier = verifier;
    this.localGate = localGate;
    this.clock = clock;
    this.kioskId = kioskId;
    this.retries = retries;
  }

  public void setForcedOffline(boolean forced) {
    journal.setMeta("forced_offline", Boolean.toString(forced));
  }

  public boolean forcedOffline() {
    return journal.meta("forced_offline").map(Boolean::parseBoolean).orElse(false);
  }

  public Result scan(String token, String gateId) {
    var attemptId = Ids.uuidV7();
    var scannedAt = clock.now();
    long seq = journal.append(attemptId, gateId, sha256(token), scannedAt); // 1. durable first
    var claims = peek(token);

    if (!forcedOffline()) {
      int unreachable = 0;
      int polls = 0;
      while (true) {
        try {
          var r =
              edge.scan(attemptId, gateId, token, scannedAt, seq, kioskId); // 2. same id every time
          if ("IN_PROGRESS".equals(r.outcome()) && polls++ < IN_PROGRESS_POLLS) {
            // an earlier request for THIS attempt is still running on the edge (for example our
            // first call
            // was slow): ask again instead of guessing. It will answer from its stored result.
            pause();
            continue;
          }
          return recordEdgeAnswer(attemptId, r, claims);
        } catch (EdgeClient.EdgeUnreachableException e) {
          // try again with the same attempt_id; the edge may have processed the first one
          if (unreachable++ >= retries) {
            break;
          }
        }
      }
    }
    return decideOffline(attemptId, token, gateId, scannedAt); // 3. fallback
  }

  private void pause() {
    try {
      sleeper.sleep(POLL_PAUSE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private Result recordEdgeAnswer(UUID attemptId, EdgeClient.ScanResponse r, String[] claims) {
    var status =
        switch (r.outcome()) {
          case "ALLOWED", "UNCONFIRMED" -> "OPENED";
          case "DENIED" -> "DENIED";
          default ->
              "PENDING"; // PENDING or IN_PROGRESS: the edge owns the attempt and will resolve it
        };
    journal.update(attemptId, status, claims[1], claims[0], r.direction(), true, r.outcome());
    return new Result(attemptId, r.outcome(), r.reasonCode(), r.direction(), false);
  }

  private Result decideOffline(UUID attemptId, String token, String gateId, Instant now) {
    var verdict = verifier.verify(token, now);
    if (!verdict.valid()) {
      journal.update(attemptId, "OFFLINE_DENIED", null, null, null, false, verdict.reason());
      return new Result(attemptId, "DENIED", verdict.reason(), null, true);
    }
    var gate = journal.gate(gateId).orElse(null);
    if (gate == null) {
      journal.update(attemptId, "OFFLINE_DENIED", null, null, null, false, "GATE_NOT_FOUND");
      return new Result(attemptId, "DENIED", "GATE_NOT_FOUND", null, true);
    }
    var direction =
        switch (gate.mode()) {
          case "ENTRY_ONLY" -> "ENTRY";
          case "EXIT_ONLY" -> "EXIT";
          default ->
              journal
                  .lastDirection(verdict.employeeUuid())
                  .map(d -> d.equals("ENTRY") ? "EXIT" : "ENTRY")
                  .orElse("ENTRY");
        };
    // The journal row is made durable BEFORE the physical gate opens, so a crash cannot lose a
    // pass.
    journal.update(
        attemptId,
        "OFFLINE_OPENED",
        verdict.employeeUuid(),
        verdict.jti(),
        direction,
        false,
        "OFFLINE");
    localGate.open(gateId, direction);
    return new Result(attemptId, "ALLOWED", null, direction, true);
  }

  /** Pulls device keys, revocations and the gate catalog so offline checks stay current. */
  public void sync() throws EdgeClient.EdgeUnreachableException {
    var since = journal.meta("last_sync").map(Instant::parse).orElse(Instant.EPOCH);
    var bundle = edge.sync(since);
    bundle.gates().forEach(journal::saveGate);
    bundle.deviceKeys().forEach(journal::saveDeviceKey);
    journal.setMeta("last_sync", clock.now().toString());
  }

  /**
   * Hands over everything the edge has not seen, in journal order. Stops quietly if the edge is
   * still down.
   */
  public FlushReport flush() {
    int retried = 0;
    int handed = 0;
    try {
      for (var row : journal.unfinished()) {
        // no raw token is stored, so the hash is sent: if the edge knows the attempt it answers
        // from storage,
        // if it does not, the scan is denied and the employee simply scans again
        var r =
            edge.scan(
                row.attemptId(),
                row.gateId(),
                row.tokenHash(),
                row.scannedAt(),
                row.seq(),
                kioskId);
        journal.update(
            row.attemptId(),
            switch (r.outcome()) {
              case "ALLOWED", "UNCONFIRMED" -> "OPENED";
              case "DENIED" -> "DENIED";
              default -> "PENDING";
            },
            null,
            null,
            r.direction(),
            true,
            r.outcome());
        retried++;
      }
      var pending = journal.pendingOfflineHandover();
      if (!pending.isEmpty()) {
        var lines = new ArrayList<EdgeClient.OfflineLine>();
        for (var row : pending) {
          lines.add(
              new EdgeClient.OfflineLine(
                  row.attemptId(),
                  row.seq(),
                  row.employeeUuid(),
                  row.gateId(),
                  row.direction(),
                  row.jti(),
                  row.scannedAt(),
                  row.status(),
                  "OFFLINE_DENIED".equals(row.status()) ? row.result() : null));
        }
        for (var result : edge.offlineBatch(kioskId, lines)) {
          journal
              .find(result.attemptId())
              .ifPresent(
                  r ->
                      journal.update(
                          r.attemptId(), r.status(), null, null, null, true, result.outcome()));
          handed++;
        }
      }
    } catch (EdgeClient.EdgeUnreachableException e) {
      // still offline; nothing is lost, the rows stay in the journal
    }
    return new FlushReport(
        retried, handed, journal.pendingOfflineHandover().size() + journal.unfinished().size());
  }

  /** Reads jti and subject WITHOUT trusting them, only to feed the local replay cache. */
  private static String[] peek(String token) {
    try {
      var c = SignedJWT.parse(token).getJWTClaimsSet();
      return new String[] {c.getJWTID(), c.getSubject()};
    } catch (Exception e) {
      return new String[] {null, null};
    }
  }

  static String sha256(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** Test and demo helper. */
  public static OfflineVerifier defaultVerifier(Journal journal) {
    return new OfflineVerifier(
        journal, Duration.ofSeconds(60), Duration.ofSeconds(60), Duration.ofMinutes(2));
  }

  /** Test helper. */
  List<Journal.Row> rows() {
    return journal.all();
  }
}
