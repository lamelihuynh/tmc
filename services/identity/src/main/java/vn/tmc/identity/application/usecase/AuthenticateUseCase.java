package vn.tmc.identity.application.usecase;

import java.util.Set;
import vn.tmc.identity.application.port.in.IAuthentication;
import vn.tmc.identity.application.port.out.IAccountRepository;
import vn.tmc.identity.application.port.out.IMfaVerifier;
import vn.tmc.identity.application.port.out.IPasswordVerifier;
import vn.tmc.platform.kernel.IClock;

/**
 * Login with lockout and MFA (NFR-03). TODO Team C: issue the JWT (Nimbus, ES256) and publish the
 * JWKS so the gateway and services can validate it; 15 minute inactivity expiry; audit of every
 * attempt.
 */
public class AuthenticateUseCase implements IAuthentication {
  private final IAccountRepository accounts;
  private final IPasswordVerifier passwords;
  private final IMfaVerifier mfa;
  private final IClock clock;

  public AuthenticateUseCase(
      IAccountRepository accounts, IPasswordVerifier passwords, IMfaVerifier mfa, IClock clock) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.mfa = mfa;
    this.clock = clock;
  }

  @Override
  public Result authenticate(String username, String password, String mfaCode) {
    var now = clock.now();
    var account = accounts.findByUsername(username).orElse(null);
    if (account == null || !account.active()) {
      return new Result(Status.DENIED, null, Set.of());
    }
    if (account.isLocked(now)) {
      return new Result(Status.LOCKED, null, Set.of());
    }
    if (!passwords.matches(username, password)) {
      account.recordFailure(now);
      accounts.save(account);
      return new Result(account.isLocked(now) ? Status.LOCKED : Status.DENIED, null, Set.of());
    }
    if (account.requiresMfa()) {
      if (mfaCode == null || mfaCode.isBlank()) {
        return new Result(Status.MFA_REQUIRED, null, Set.of());
      }
      if (!mfa.verify(username, mfaCode)) {
        account.recordFailure(now);
        accounts.save(account);
        return new Result(account.isLocked(now) ? Status.LOCKED : Status.DENIED, null, Set.of());
      }
    }
    account.recordSuccess();
    accounts.save(account);
    return new Result(Status.GRANTED, account.subject(), account.roles());
  }
}
