package vn.tmc.gatestation.domain;

/**
 * A registered gate station and the turnstile it controls (FR-11 to FR-16). hardwareGateId is the
 * id used by the Turnstile Control System; gateId is TMC's own id. One station controls exactly one
 * turnstile.
 */
public record GateConfig(
    String gateId, String factoryCode, String hardwareGateId, GateMode mode, boolean maintenance) {

  public boolean acceptsScans() {
    return !maintenance;
  }
}
