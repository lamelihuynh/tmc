package vn.tmc.identity.bootstrap;

import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.identity.adapter.out.inmem.InMemoryAccountRepository;
import vn.tmc.identity.adapter.out.inmem.Pbkdf2PasswordVerifier;
import vn.tmc.identity.application.port.in.IAuthentication;
import vn.tmc.identity.application.port.out.IAccountRepository;
import vn.tmc.identity.application.port.out.IMfaVerifier;
import vn.tmc.identity.application.port.out.IPasswordVerifier;
import vn.tmc.identity.application.usecase.AuthenticateUseCase;
import vn.tmc.identity.domain.Account;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.Role;

@Configuration
public class IdentityConfig {
  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  IAccountRepository accounts() {
    var repo = new InMemoryAccountRepository();
    // dev seed only
    repo.save(
        new Account(
            UUID.fromString("0192a1c0-0000-7000-8000-00000000a001"),
            "supervisor1",
            Set.of(Role.PRODUCTION_SUPERVISOR)));
    repo.save(
        new Account(
            UUID.fromString("0192a1c0-0000-7000-8000-00000000a002"),
            "admin1",
            Set.of(Role.SYSTEM_ADMIN)));
    // Employees for the employee app (apps/employee-app). The subject is the same employee UUID
    // that gate-station seeds in application.yml, so a login here maps to a known employee there.
    // nv003 is the inactive employee: login works, the gate answers EMPLOYEE_INACTIVE.
    for (int i = 1; i <= EMPLOYEE_SEEDS; i++) {
      repo.save(
          new Account(
              UUID.fromString("0192a1c0-0000-7000-8000-00000000000" + i),
              "nv00" + i,
              Set.of(Role.EMPLOYEE)));
    }
    return repo;
  }

  private static final int EMPLOYEE_SEEDS = 5;

  @Bean
  IPasswordVerifier passwords() {
    var verifier = new Pbkdf2PasswordVerifier();
    for (int i = 1; i <= EMPLOYEE_SEEDS; i++) {
      verifier.set("nv00" + i, "dev-only-change-me");
    }
    verifier.set("supervisor1", "dev-only-change-me");
    verifier.set("admin1", "dev-only-change-me");
    return verifier;
  }

  /** TODO Team C: real TOTP. Until then MFA always fails closed. */
  @Bean
  IMfaVerifier mfa() {
    return (username, code) -> false;
  }

  @Bean
  IAuthentication authentication(
      IAccountRepository accounts, IPasswordVerifier passwords, IMfaVerifier mfa, IClock clock) {
    return new AuthenticateUseCase(accounts, passwords, mfa, clock);
  }
}
