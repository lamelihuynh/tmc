package vn.tmc.identity.application.port.out;

import java.util.Optional;
import vn.tmc.identity.domain.Account;

/** Driven port: where staff accounts live. The use case owns the rules; this only stores. */
public interface IAccountRepository {
  Optional<Account> findByUsername(String username);

  void save(Account account);
}
