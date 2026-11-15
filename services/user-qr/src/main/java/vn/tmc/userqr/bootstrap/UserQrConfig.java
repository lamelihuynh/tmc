package vn.tmc.userqr.bootstrap;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.PrincipalResolver;
import vn.tmc.platform.security.jwt.JwtPrincipalVerifier;
import vn.tmc.userqr.adapter.out.crypto.NimbusTokenCodec;
import vn.tmc.userqr.adapter.out.inmem.InMemoryDeviceKeyRegistry;
import vn.tmc.userqr.adapter.out.inmem.LoggingAuditLog;
import vn.tmc.userqr.adapter.out.persistence.JdbcDeviceKeyRegistry;
import vn.tmc.userqr.application.port.in.IDeviceManagement;
import vn.tmc.userqr.application.port.in.IQRValidation;
import vn.tmc.userqr.application.port.in.ISyncBundle;
import vn.tmc.userqr.application.port.out.IAuditLog;
import vn.tmc.userqr.application.port.out.IDeviceKeyRegistry;
import vn.tmc.userqr.application.port.out.ITokenCodec;
import vn.tmc.userqr.application.usecase.BuildSyncBundleUseCase;
import vn.tmc.userqr.application.usecase.DeviceManagementService;
import vn.tmc.userqr.application.usecase.VerifyQrTokenUseCase;
import vn.tmc.userqr.domain.TokenVerificationPolicy;

/** Composition root: the only place that knows which adapter implements which port. */
@Configuration
public class UserQrConfig {

  @Bean
  IClock clock() {
    return IClock.system();
  }

  /**
   * Who is calling (docs/api/identity.openapi.yaml). {@code tmc.security.jwks-url}: identity's
   * JWKS, Bearer tokens are verified against it; {@code tmc.security.jwks-json}: a pinned JWKS
   * instead (tests). {@code tmc.security.allow-dev-headers}: X-Subject/X-Roles are trusted when no
   * Authorization header is sent; true only on the dev profiles (local, inmem).
   */
  @Bean
  PrincipalResolver principalResolver(
      @Value("${tmc.security.jwks-url:}") String jwksUrl,
      @Value("${tmc.security.jwks-json:}") String jwksJson,
      @Value("${tmc.security.allow-dev-headers:false}") boolean allowDevHeaders,
      IClock clock) {
    JwtPrincipalVerifier verifier = null;
    if (!jwksJson.isBlank()) {
      verifier = JwtPrincipalVerifier.fromJwksJson(jwksJson, clock);
    } else if (!jwksUrl.isBlank()) {
      verifier = JwtPrincipalVerifier.fromJwksUrl(jwksUrl, clock);
    }
    return new PrincipalResolver(verifier, allowDevHeaders);
  }

  @Bean
  ITokenCodec tokenCodec() {
    return new NimbusTokenCodec();
  }

  @Bean
  IAuditLog auditLog() {
    return new LoggingAuditLog();
  }

  @Bean
  TokenVerificationPolicy verificationPolicy(
      @Value("${tmc.qr.clock-skew-seconds:60}") long skew,
      @Value("${tmc.qr.max-lifetime-seconds:60}") long maxLifetime) {
    return new TokenVerificationPolicy(Duration.ofSeconds(skew), Duration.ofSeconds(maxLifetime));
  }

  @Bean
  IQRValidation qrValidation(
      ITokenCodec codec,
      IDeviceKeyRegistry registry,
      TokenVerificationPolicy policy,
      IClock clock) {
    return new VerifyQrTokenUseCase(codec, registry, policy, clock);
  }

  @Bean
  IDeviceManagement deviceManagement(IDeviceKeyRegistry registry, IAuditLog audit, IClock clock) {
    return new DeviceManagementService(registry, audit, clock);
  }

  @Bean
  ISyncBundle syncBundle(IDeviceKeyRegistry registry, IClock clock) {
    return new BuildSyncBundleUseCase(registry, clock);
  }

  @Configuration
  @Profile({"inmem", "local"})
  static class InMemory {
    @Bean
    IDeviceKeyRegistry deviceKeyRegistry() {
      return new InMemoryDeviceKeyRegistry();
    }
  }

  @Configuration
  @Profile("!inmem & !local")
  static class Postgres {
    @Bean
    IDeviceKeyRegistry deviceKeyRegistry(JdbcTemplate jdbc) {
      return new JdbcDeviceKeyRegistry(jdbc);
    }
  }
}
