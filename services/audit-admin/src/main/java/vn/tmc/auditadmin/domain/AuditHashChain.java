package vn.tmc.auditadmin.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeMap;

/**
 * Tamper evidence for the audit log (AC-05, ADR-16): every stored entry carries {@code hash =
 * SHA-256(prevHash || canonical(entry))}, where prevHash is the hash of the entry stored just
 * before it. Changing, removing or reordering any stored entry breaks every hash after it.
 */
public final class AuditHashChain {
  /** prevHash of the first entry. */
  public static final String GENESIS = "0".repeat(64);

  /** One stored entry with its position and chain hashes. */
  public record Link(long seq, AuditEntry entry, String prevHash, String hash) {}

  /**
   * @param valid true if every link matches
   * @param checked links examined (up to and including the first broken one)
   * @param brokenAt seq of the first link whose prevHash or hash does not match, null if valid
   */
  public record Verification(boolean valid, long checked, Long brokenAt) {}

  private AuditHashChain() {}

  public static String hash(String prevHash, AuditEntry entry) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      digest.update(prevHash.getBytes(StandardCharsets.US_ASCII));
      digest.update(canonical(entry).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
    }
  }

  /** Builds the link that follows {@code previous} (null for the first entry). */
  public static Link next(Link previous, long seq, AuditEntry entry) {
    var prevHash = previous == null ? GENESIS : previous.hash();
    return new Link(seq, entry, prevHash, hash(prevHash, entry));
  }

  /** Recomputes the chain over links in storage order. */
  public static Verification verify(List<Link> links) {
    var verifier = new Verifier();
    links.forEach(verifier::accept);
    return verifier.result();
  }

  /** Incremental form of {@link #verify}, for streaming a large table row by row. */
  public static final class Verifier {
    private String expectedPrev = GENESIS;
    private long checked;
    private Long brokenAt;

    public void accept(Link link) {
      if (brokenAt != null) {
        return;
      }
      checked++;
      if (!expectedPrev.equals(link.prevHash())
          || !hash(link.prevHash(), link.entry()).equals(link.hash())) {
        brokenAt = link.seq();
        return;
      }
      expectedPrev = link.hash();
    }

    public Verification result() {
      return new Verification(brokenAt == null, checked, brokenAt);
    }
  }

  /**
   * Unambiguous text form: every field is written as {@code <length>:<value>} ({@code -1:} for
   * null), details sorted by key, occurredAt as ISO-8601 in UTC.
   */
  static String canonical(AuditEntry e) {
    var out = new StringBuilder();
    field(out, String.valueOf(e.entryId()));
    field(out, e.occurredAt() == null ? null : e.occurredAt().toString());
    field(out, e.source());
    field(out, e.action());
    field(out, e.actor());
    field(out, e.target());
    var details = new TreeMap<>(e.details());
    field(out, String.valueOf(details.size()));
    details.forEach(
        (k, v) -> {
          field(out, k);
          field(out, v);
        });
    return out.toString();
  }

  private static void field(StringBuilder out, String value) {
    if (value == null) {
      out.append("-1:");
      return;
    }
    out.append(value.length()).append(':').append(value);
  }
}
