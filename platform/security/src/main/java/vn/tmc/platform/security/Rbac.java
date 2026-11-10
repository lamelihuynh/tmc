package vn.tmc.platform.security;

/**
 * Server-side authorization helper (ADR-08). Every use case that is reachable from an API must call
 * one of these before doing work; the gateway never replaces this check.
 */
public final class Rbac {
  private Rbac() {}

  public static void requireRole(Principal caller, Role... allowed) {
    if (caller == null || !caller.hasAnyRole(allowed)) {
      throw new AccessDeniedException("role not permitted");
    }
  }

  public static void requireGate(Principal caller, String gateId) {
    if (caller == null || caller.scope() == null || !caller.scope().coversGate(gateId)) {
      throw new AccessDeniedException("gate out of scope: " + gateId);
    }
  }

  public static void requireFactory(Principal caller, String factoryCode) {
    if (caller == null || caller.scope() == null || !caller.scope().coversFactory(factoryCode)) {
      throw new AccessDeniedException("factory out of scope: " + factoryCode);
    }
  }
}
