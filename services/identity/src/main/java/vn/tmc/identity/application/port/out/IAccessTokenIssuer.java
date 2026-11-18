package vn.tmc.identity.application.port.out;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.security.Role;

/** Driven port: signs the access token of docs/api/identity.openapi.yaml (ES256, 15 minutes). */
public interface IAccessTokenIssuer {

  record IssuedToken(String token, Instant expiresAt) {}

  IssuedToken issue(UUID subject, Set<Role> roles, Set<String> factories, Instant now);
}
