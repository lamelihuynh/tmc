package vn.tmc.platform.security;

import java.util.Set;

/**
 * Data scope attached to a principal: factories, groups/shifts, gates (document.md section 12.2).
 */
public record DataScope(
    Set<String> factories, Set<String> groups, Set<String> gates, boolean allFactories) {

  public DataScope {
    factories = factories == null ? Set.of() : Set.copyOf(factories);
    groups = groups == null ? Set.of() : Set.copyOf(groups);
    gates = gates == null ? Set.of() : Set.copyOf(gates);
  }

  public static DataScope all() {
    return new DataScope(Set.of(), Set.of(), Set.of(), true);
  }

  public static DataScope ofGates(String factory, String... gates) {
    return new DataScope(Set.of(factory), Set.of(), Set.of(gates), false);
  }

  public boolean coversGate(String gateId) {
    return allFactories || gates.contains(gateId);
  }

  public boolean coversFactory(String factoryCode) {
    return allFactories || factories.contains(factoryCode);
  }
}
