package vn.tmc.platform.security;

import java.util.Set;
import java.util.UUID;

/** Authenticated caller. Subject is an internal UUID, never a readable name (ADR-09). */
public record Principal(UUID subject, Set<Role> roles, DataScope scope) {

  public Principal {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }

  public boolean hasAnyRole(Role... candidates) {
    for (Role r : candidates) {
      if (roles.contains(r)) {
        return true;
      }
    }
    return false;
  }
}
