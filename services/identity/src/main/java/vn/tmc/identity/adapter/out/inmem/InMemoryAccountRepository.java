package vn.tmc.identity.adapter.out.inmem;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.identity.application.port.out.IAccountRepository;
import vn.tmc.identity.domain.Account;

/** Process-memory account store for tests and the dev seed. TODO Team C: JDBC store. */
public class InMemoryAccountRepository implements IAccountRepository {
  private final Map<String, Account> byUsername = new ConcurrentHashMap<>();

  @Override
  public Optional<Account> findByUsername(String username) {
    return username == null ? Optional.empty() : Optional.ofNullable(byUsername.get(username));
  }

  @Override
  public void save(Account account) {
    byUsername.put(account.username(), account);
  }
}
