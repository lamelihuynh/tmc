package vn.tmc.gatestation.application.port.out;

import java.util.List;
import java.util.Optional;
import vn.tmc.gatestation.domain.GateConfig;

/** Driven port: the gate catalog (FR-11 to FR-16). TODO Team A (A-04): PostgreSQL Gate Store. */
public interface IGateConfigRepository {
  Optional<GateConfig> find(String gateId);

  List<GateConfig> all();
}
