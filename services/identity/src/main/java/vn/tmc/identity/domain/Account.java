package vn.tmc.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.security.Role;

/**
 * A staff account (ADR-08). Rules from NFR-03: lock for 15 minutes after 5 wrong passwords; HR
 * Staff and System Administrator must pass MFA. Employees authenticate in the mobile app, not here
 * (TODO Team C).
 */
public final class Account {
  public static final int MAX_FAILURES = 5;
  public static final Duration LOCK_TIME = Duration.ofMinutes(15);
  private static final Set<Role> MFA_ROLES = Set.of(Role.HR_STAFF, Role.SYSTEM_ADMIN);

  private final UUID subject;
  private final String username;
  private final Set<Role> roles;
  private int failedAttempts;
  private Instant lockedUntil;
  private boolean active = true;

  public Account(UUID subject, String username, Set<Role> roles) {
    this.subject = subject;
    this.username = username;
    this.roles = Set.copyOf(roles);
  }

  public UUID subject() {
    return subject;
  }

  public String username() {
    return username;
  }

  public Set<Role> roles() {
    return roles;
  }

  public boolean active() {
    return active;
  }

  public int failedAttempts() {
    return failedAttempts;
  }

  public void deactivate() {
    this.active = false;
  }

  public boolean isLocked(Instant now) {
    return lockedUntil != null && now.isBefore(lockedUntil);
  }

  public boolean requiresMfa() {
    return roles.stream().anyMatch(MFA_ROLES::contains);
  }

  public void recordFailure(Instant now) {
    failedAttempts++;
    if (failedAttempts >= MAX_FAILURES) {
      lockedUntil = now.plus(LOCK_TIME);
      failedAttempts = 0;
    }
  }

  public void recordSuccess() {
    failedAttempts = 0;
    lockedUntil = null;
  }
}
