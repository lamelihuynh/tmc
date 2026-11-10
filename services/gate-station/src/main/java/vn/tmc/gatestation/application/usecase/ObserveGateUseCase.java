package vn.tmc.gatestation.application.usecase;

import java.util.UUID;
import vn.tmc.gatestation.application.port.in.IGateObservation;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort;

/**
 * Resolves an unknown outcome by asking the turnstile what the command did, never by sending a new
 * one.
 */
public class ObserveGateUseCase implements IGateObservation {
  private final IGateConfigRepository gates;
  private final ITurnstileControlPort turnstile;

  public ObserveGateUseCase(IGateConfigRepository gates, ITurnstileControlPort turnstile) {
    this.gates = gates;
    this.turnstile = turnstile;
  }

  @Override
  public Observation observe(UUID attemptId, String gateId) {
    var gate = gates.find(gateId).orElse(null);
    if (gate == null) {
      return Observation.UNDETERMINED;
    }
    return switch (turnstile.queryCommand(gate.hardwareGateId(), attemptId.toString())) {
      case OPENED -> Observation.OPENED;
      case NOT_OPENED -> Observation.NOT_OPENED;
      case UNDETERMINED -> Observation.UNDETERMINED;
    };
  }
}
