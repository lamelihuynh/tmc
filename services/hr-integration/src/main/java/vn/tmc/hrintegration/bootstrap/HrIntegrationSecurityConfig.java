package vn.tmc.hrintegration.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.PrincipalResolver;
import vn.tmc.platform.security.jwt.JwtPrincipalVerifier;

/**
 * D-13: callers are identified by the identity JWT (JWKS from {@code tmc.security.jwks-url}, or
 * {@code tmc.security.jwks-json} in tests). The development headers are accepted only where {@code
 * tmc.security.allow-dev-headers} is true (local and inmem profiles). Same contract as user-qr,
 * attendance and gate-station.
 */
@Configuration
public class HrIntegrationSecurityConfig {
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
}
