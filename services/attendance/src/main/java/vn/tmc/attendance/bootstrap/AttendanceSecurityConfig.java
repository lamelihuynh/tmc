package vn.tmc.attendance.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.security.PrincipalResolver;
import vn.tmc.platform.security.jwt.JwtPrincipalVerifier;

/** Same JWT contract as user-qr; development headers are an explicit profile setting only. */
@Configuration
public class AttendanceSecurityConfig {
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
