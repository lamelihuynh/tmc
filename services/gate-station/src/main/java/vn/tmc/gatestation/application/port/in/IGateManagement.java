package vn.tmc.gatestation.application.port.in;

import java.util.List;
import java.util.Optional;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.platform.security.Principal;

/** Gate catalog administration (FR-11 to FR-13, FR-16). Only SYSTEM_ADMIN may call it. */
public interface IGateManagement {
  List<GateConfig> list(Principal caller);

  Optional<GateConfig> find(Principal caller, String gateId);

  GateConfig upsert(Principal caller, GateConfig gate);

  Optional<GateConfig> setMaintenance(Principal caller, String gateId, boolean maintenance);
}
