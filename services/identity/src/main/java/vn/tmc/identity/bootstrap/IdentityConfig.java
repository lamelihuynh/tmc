package vn.tmc.identity.bootstrap;

import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.identity.adapter.out.crypto.NimbusAccessTokenIssuer;
import vn.tmc.identity.adapter.out.inmem.InMemoryAccountRepository;
import vn.tmc.identity.adapter.out.inmem.Pbkdf2PasswordVerifier;
import vn.tmc.identity.application.port.in.IAuthentication;
import vn.tmc.identity.application.port.in.ILogin;
import vn.tmc.identity.application.port.out.IAccessTokenIssuer;
import vn.tmc.identity.application.port.out.IAccountRepository;
import vn.tmc.identity.application.port.out.IMfaVerifier;
import vn.tmc.identity.application.port.out.IPasswordVerifier;
import vn.tmc.identity.application.usecase.AuthenticateUseCase;
import vn.tmc.identity.application.usecase.LoginUseCase;
import vn.tmc.identity.domain.Account;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.AccessTokenSigner;

@Configuration
public class IdentityConfig {
  private static final Logger LOG = LoggerFactory.getLogger(IdentityConfig.class);

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
            Set.of(Role.PRODUCTION_SUPERVISOR),
            Set.of("BD")));
    repo.save(
        new Account(
            UUID.fromString("0192a1c0-0000-7000-8000-00000000a002"),
            "admin1",
            Set.of(Role.SYSTEM_ADMIN),
            Set.of(Account.ALL_FACTORIES)));
    // Employees for the employee app; same UUIDs as gate-station and user-qr (DevSeed).
    for (var e : DevSeed.EMPLOYEES) {
      repo.save(new Account(e.employeeUuid(), e.username(), Set.of(Role.EMPLOYEE), e.factories()));
    }
    return repo;
  }

  @Bean
  IPasswordVerifier passwords() {
    var verifier = new Pbkdf2PasswordVerifier();
    for (var e : DevSeed.EMPLOYEES) {
      verifier.set(e.username(), DevSeed.DEV_PASSWORD);
    }
    verifier.set("supervisor1", DevSeed.DEV_PASSWORD);
    verifier.set("admin1", DevSeed.DEV_PASSWORD);
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

  /**
   * Signing key. {@code tmc.identity.jwt.private-jwk} (env IDENTITY_JWT_PRIVATE_JWK) holds a
   * private EC P-256 JWK; empty means a key generated at start-up (dev): tokens then stop verifying
   * after a restart, which is acceptable for a 15 minute token. TODO Team C: key from a secret
   * store and rotation (two keys in the JWKS during the overlap).
   */
  @Bean
  AccessTokenSigner accessTokenSigner(@Value("${tmc.identity.jwt.private-jwk:}") String jwk) {
    if (jwk == null || jwk.isBlank()) {
      var signer = AccessTokenSigner.ephemeral();
      LOG.warn("No tmc.identity.jwt.private-jwk: using a generated key, kid={}", signer.keyId());
      return signer;
    }
    return AccessTokenSigner.fromPrivateJwk(jwk);
  }

  @Bean
  IAccessTokenIssuer accessTokenIssuer(AccessTokenSigner signer) {
    return new NimbusAccessTokenIssuer(signer);
  }

  @Bean
  ILogin login(IAuthentication authentication, IAccessTokenIssuer issuer, IClock clock) {
    return new LoginUseCase(authentication, issuer, clock);
  }
}
