package vn.tmc.identity.application.usecase;

import vn.tmc.identity.application.port.in.IAuthentication;
import vn.tmc.identity.application.port.in.IAuthentication.Status;
import vn.tmc.identity.application.port.in.ILogin;
import vn.tmc.identity.application.port.out.IAccessTokenIssuer;
import vn.tmc.platform.kernel.IClock;

/** Credentials first ({@link IAuthentication}); only a GRANTED login gets a token. */
public class LoginUseCase implements ILogin {
  private final IAuthentication authentication;
  private final IAccessTokenIssuer issuer;
  private final IClock clock;

  public LoginUseCase(IAuthentication authentication, IAccessTokenIssuer issuer, IClock clock) {
    this.authentication = authentication;
    this.issuer = issuer;
    this.clock = clock;
  }

  @Override
  public LoginResult login(String username, String password, String mfaCode) {
    var r = authentication.authenticate(username, password, mfaCode);
    if (r.status() != Status.GRANTED) {
      return new LoginResult(r.status(), null, java.util.Set.of(), null, null);
    }
    var token = issuer.issue(r.subject(), r.roles(), r.factories(), clock.now());
    return new LoginResult(r.status(), r.subject(), r.roles(), token.token(), token.expiresAt());
  }
}
