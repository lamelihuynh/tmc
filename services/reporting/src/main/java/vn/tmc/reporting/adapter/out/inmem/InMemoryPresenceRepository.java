package vn.tmc.reporting.adapter.out.inmem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import vn.tmc.reporting.application.port.out.IPresenceRepository;
import vn.tmc.reporting.domain.Presence;

/** Process-memory read model. TODO Team D: PostgreSQL table updated with processed_messages. */
public class InMemoryPresenceRepository implements IPresenceRepository {
  private record Key(UUID employeeUuid, String factoryCode) {}

  private final Map<Key, Presence> rows = new ConcurrentHashMap<>();

  @Override
  public Optional<Presence> find(UUID employeeUuid, String factoryCode) {
    return Optional.ofNullable(rows.get(new Key(employeeUuid, factoryCode)));
  }

  @Override
  public void save(Presence presence) {
    rows.put(new Key(presence.employeeUuid(), presence.factoryCode()), presence);
  }

  @Override
  public List<Presence> byFactory(String factoryCode) {
    return rows.values().stream().filter(p -> p.factoryCode().equals(factoryCode)).toList();
  }
}
