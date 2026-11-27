package vn.tmc.gatestation.application.port.out;

import java.util.List;
import java.util.Optional;
import vn.tmc.gatestation.domain.GateConfig;

/** Driven port: the gate catalog (FR-11 to FR-16). Backed by the Gate Store under its profile. */
public interface IGateConfigRepository {
  Optional<GateConfig> find(String gateId);

  List<GateConfig> all();

  /** Inserts the gate or replaces the stored fields when it already exists. */
  GateConfig upsert(GateConfig gate);
}
