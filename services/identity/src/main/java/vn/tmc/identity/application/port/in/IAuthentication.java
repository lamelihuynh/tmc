package vn.tmc.identity.application.port.in;

import java.util.Set;
import java.util.UUID;
import vn.tmc.platform.security.Role;

public interface IAuthentication {

  enum Status {
    GRANTED,
    MFA_REQUIRED,
    DENIED,
    LOCKED
  }

  /** The caller can only tell "wrong credentials"; whether the account exists is never revealed. */
  record Result(Status status, UUID subject, Set<Role> roles) {}

  Result authenticate(String username, String password, String mfaCode);
}
