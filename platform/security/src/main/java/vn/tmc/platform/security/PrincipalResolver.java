package vn.tmc.platform.security;

import java.util.function.Function;
import vn.tmc.platform.security.jwt.JwtPrincipalVerifier;

/**
 * Finds the caller of an HTTP request without depending on servlet types: pass {@code
 * request::getHeader}. Rules (docs/api/identity.openapi.yaml):
 *
 * <ol>
 *   <li>{@code Authorization: Bearer <jwt>} present: the token decides. A bad token is
 *       unauthenticated; it never falls back to the dev headers.
 *   <li>No {@code Authorization}: the dev headers ({@link PrincipalHeaders}) are trusted only when
 *       {@code allowDevHeaders} is true, which services set on their dev profiles (local, inmem).
 * </ol>
 *
 * Returns null for "unauthenticated"; controllers map that to 403 as before.
 */
public final class PrincipalResolver {
  public static final String AUTHORIZATION = "Authorization";
  private static final String BEARER = "Bearer ";

  private final JwtPrincipalVerifier verifier;
  private final boolean allowDevHeaders;

  /**
   * @param verifier null when the service has no JWKS configured (then Bearer tokens are refused)
   */
  public PrincipalResolver(JwtPrincipalVerifier verifier, boolean allowDevHeaders) {
    this.verifier = verifier;
    this.allowDevHeaders = allowDevHeaders;
  }

  /** Dev headers only, the behaviour every service had before JWT existed. */
  public static PrincipalResolver devHeadersOnly() {
    return new PrincipalResolver(null, true);
  }

  public boolean allowsDevHeaders() {
    return allowDevHeaders;
  }

  public Principal resolve(Function<String, String> header) {
    String authorization = header.apply(AUTHORIZATION);
    if (authorization != null && !authorization.isBlank()) {
      if (verifier == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
        return null;
      }
      return verifier.verify(authorization.substring(BEARER.length()).trim());
    }
    if (!allowDevHeaders) {
      return null;
    }
    return PrincipalHeaders.parse(
        header.apply("X-Subject"),
        header.apply("X-Roles"),
        header.apply("X-Scope-Factories"),
        header.apply("X-Scope-Gates"));
  }
}
