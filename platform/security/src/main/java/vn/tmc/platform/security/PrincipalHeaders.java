package vn.tmc.platform.security;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds a {@link Principal} from the headers the API Gateway injects after validating the JWT
 * issued by Identity: X-Subject, X-Roles, X-Scope-Factories, X-Scope-Gates. Services still run
 * {@link Rbac} checks themselves; the gateway never replaces them (ADR-08).
 *
 * <p>TODO Team C: replace by direct JWT validation against the Identity JWKS once identity exists.
 */
public final class PrincipalHeaders {
  private PrincipalHeaders() {}

  /**
   * @return null when the caller is unauthenticated (missing or malformed headers)
   */
  public static Principal parse(String subject, String roles, String factories, String gates) {
    if (subject == null || roles == null) {
      return null;
    }
    try {
      Set<Role> parsedRoles =
          Arrays.stream(roles.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .map(Role::valueOf)
              .collect(Collectors.toSet());
      DataScope scope =
          "*".equals(factories)
              ? DataScope.all()
              : new DataScope(split(factories), Set.of(), split(gates), false);
      return new Principal(UUID.fromString(subject.trim()), parsedRoles, scope);
    } catch (IllegalArgumentException e) {
      return null; // unknown role or bad UUID: treat as unauthenticated, never as a partial
      // principal
    }
  }

  private static Set<String> split(String csv) {
    if (csv == null || csv.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }
}
