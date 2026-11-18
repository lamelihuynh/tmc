package vn.tmc.gatestation.adapter.out.inmem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.domain.GateConfig;

/** Fixed gate catalog loaded once (from application.yml in the scaffold). */
public class InMemoryGateConfigRepository implements IGateConfigRepository {
  private final Map<String, GateConfig> gates = new LinkedHashMap<>();

  public InMemoryGateConfigRepository(List<GateConfig> gates) {
    gates.forEach(g -> this.gates.put(g.gateId(), g));
  }

  @Override
  public Optional<GateConfig> find(String gateId) {
    return Optional.ofNullable(gates.get(gateId));
  }

  @Override
  public List<GateConfig> all() {
    return List.copyOf(gates.values());
  }
}
