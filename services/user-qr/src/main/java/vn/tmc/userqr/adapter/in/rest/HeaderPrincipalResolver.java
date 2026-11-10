package vn.tmc.userqr.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.PrincipalHeaders;

/**
 * Thin servlet adapter over the shared header parser; keeps servlet types out of platform/security.
 */
final class HeaderPrincipalResolver {
  private HeaderPrincipalResolver() {}

  static Principal resolve(HttpServletRequest request) {
    return PrincipalHeaders.parse(
        request.getHeader("X-Subject"),
        request.getHeader("X-Roles"),
        request.getHeader("X-Scope-Factories"),
        request.getHeader("X-Scope-Gates"));
  }
}
