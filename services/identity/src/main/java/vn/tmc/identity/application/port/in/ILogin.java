package vn.tmc.identity.application.port.in;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import vn.tmc.identity.application.port.in.IAuthentication.Status;
import vn.tmc.platform.security.Role;

/** Login that ends in an access token when the credentials are accepted. */
public interface ILogin {

  /** token and expiresAt are null unless status is GRANTED. */
  record LoginResult(
      Status status, UUID subject, Set<Role> roles, String accessToken, Instant expiresAt) {}

  LoginResult login(String username, String password, String mfaCode);
}
