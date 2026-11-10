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
    return repo;
  }

  @Bean
  IPasswordVerifier passwords() {
    var verifier = new Pbkdf2PasswordVerifier();
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
