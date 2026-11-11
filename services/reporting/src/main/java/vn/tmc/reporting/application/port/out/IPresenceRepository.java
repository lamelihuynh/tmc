package vn.tmc.reporting.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.tmc.reporting.domain.Presence;

/** Driven port: the presence read model, one row per employee and factory. */
public interface IPresenceRepository {
  Optional<Presence> find(UUID employeeUuid, String factoryCode);

  void save(Presence presence);

  List<Presence> byFactory(String factoryCode);
}
