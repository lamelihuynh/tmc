package vn.tmc.identity.application.usecase;

import java.time.Instant;
import java.util.Set;
import vn.tmc.identity.application.port.in.IAuthentication;
import vn.tmc.identity.application.port.out.IAccountRepository;
import vn.tmc.identity.application.port.out.IAuditLog;
import vn.tmc.identity.application.port.out.IMfaVerifier;
import vn.tmc.identity.application.port.out.IPasswordVerifier;
import vn.tmc.identity.domain.Account;
import vn.tmc.platform.kernel.IClock;

/**
 * Credential check with lockout and MFA (NFR-03). The token is issued by {@link LoginUseCase} on
 * top of this. Every attempt is audited (C-08): the caller still only learns GRANTED, DENIED,
 * LOCKED or MFA_REQUIRED, while audit-admin gets the reason code. The audit never carries the
 * username, password or MFA code; an attempt on an unknown username has no target at all.
 *
 * <p>An audit write that fails propagates: a login that cannot be audited is not granted. TODO Team
 * C (NFR-03): 15 minute inactivity expiry.
 */
public class AuthenticateUseCase implements IAuthentication {
  public static final String LOGIN_SUCCEEDED = "LOGIN_SUCCEEDED";
  public static final String LOGIN_FAILED = "LOGIN_FAILED";
  public static final String LOGIN_LOCKED = "LOGIN_LOCKED";
  public static final String LOGIN_MFA_REQUIRED = "LOGIN_MFA_REQUIRED";

  /** Reason codes in {@code result} of LOGIN_FAILED. */
  public static final String UNKNOWN_ACCOUNT = "UNKNOWN_ACCOUNT";

  public static final String ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";
  public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
  public static final String BAD_PASSWORD = "BAD_PASSWORD";
  public static final String BAD_MFA_CODE = "BAD_MFA_CODE";

  private final IAccountRepository accounts;
  private final IPasswordVerifier passwords;
  private final IMfaVerifier mfa;
  private final IAuditLog audit;
  private final IClock clock;

  public AuthenticateUseCase(
      IAccountRepository accounts,
      IPasswordVerifier passwords,
      IMfaVerifier mfa,
      IAuditLog audit,
      IClock clock) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.mfa = mfa;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public Result authenticate(String username, String password, String mfaCode) {
    var now = clock.now();
    var account = accounts.findByUsername(username).orElse(null);
    if (account == null) {
      audit.record(LOGIN_FAILED, null, null, UNKNOWN_ACCOUNT);
      return new Result(Status.DENIED, null, Set.of());
    }
    var target = account.subject().toString();
    if (!account.active()) {
      audit.record(LOGIN_FAILED, null, target, ACCOUNT_INACTIVE);
      return new Result(Status.DENIED, null, Set.of());
    }
    if (account.isLocked(now)) {
      audit.record(LOGIN_FAILED, null, target, ACCOUNT_LOCKED);
      return new Result(Status.LOCKED, null, Set.of());
    }
    if (!passwords.matches(username, password)) {
      return failure(account, now, BAD_PASSWORD);
    }
    if (account.requiresMfa()) {
      if (mfaCode == null || mfaCode.isBlank()) {
        audit.record(LOGIN_MFA_REQUIRED, null, target, "MFA_REQUIRED");
        return new Result(Status.MFA_REQUIRED, null, Set.of());
      }
      if (!mfa.verify(username, mfaCode)) {
        return failure(account, now, BAD_MFA_CODE);
      }
    }
    account.recordSuccess();
    accounts.save(account);
    audit.record(LOGIN_SUCCEEDED, account.subject(), target, "OK");
    return new Result(Status.GRANTED, account.subject(), account.roles(), account.factories());
  }

  /** A wrong password or MFA code; the one that reaches the limit locks the account. */
  private Result failure(Account account, Instant now, String reason) {
    account.recordFailure(now);
    accounts.save(account);
    var target = account.subject().toString();
    audit.record(LOGIN_FAILED, null, target, reason);
    if (account.isLocked(now)) {
      audit.record(LOGIN_LOCKED, null, target, "LOCKED");
      return new Result(Status.LOCKED, null, Set.of());
    }
    return new Result(Status.DENIED, null, Set.of());
  }
}
