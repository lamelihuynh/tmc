package vn.tmc.identity.adapter.out.crypto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import vn.tmc.identity.application.port.out.IAccessTokenIssuer;
import vn.tmc.identity.domain.Account;
import vn.tmc.platform.kernel.Ids;
import vn.tmc.platform.security.DataScope;
import vn.tmc.platform.security.Principal;
import vn.tmc.platform.security.Role;
import vn.tmc.platform.security.jwt.AccessTokenContract;
import vn.tmc.platform.security.jwt.AccessTokenSigner;

/** ES256 issuer over the shared {@link AccessTokenSigner}, so the claim set matches verifiers. */
public class NimbusAccessTokenIssuer implements IAccessTokenIssuer {
  private final AccessTokenSigner signer;

  public NimbusAccessTokenIssuer(AccessTokenSigner signer) {
    this.signer = signer;
  }

  @Override
  public IssuedToken issue(UUID subject, Set<Role> roles, Set<String> factories, Instant now) {
    var scope =
        factories.contains(Account.ALL_FACTORIES)
            ? DataScope.all()
            : new DataScope(factories, Set.of(), Set.of(), false);
    var token = signer.sign(new Principal(subject, roles, scope), now, Ids.uuidV7().toString());
    return new IssuedToken(token, now.plus(AccessTokenContract.TTL));
  }
}
