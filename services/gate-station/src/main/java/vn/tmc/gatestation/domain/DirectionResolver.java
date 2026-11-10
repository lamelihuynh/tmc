package vn.tmc.gatestation.domain;

import java.util.Optional;

/** Resolves the direction of a pass (document.md UC-08 alternative 4.1). */
public final class DirectionResolver {
  private DirectionResolver() {}

  public static Direction resolve(GateConfig gate, Optional<LastPass> last) {
    return switch (gate.mode()) {
      case ENTRY_ONLY -> Direction.ENTRY;
      case EXIT_ONLY -> Direction.EXIT;
      // inside means the next pass is an exit; outside or unknown means an entry
      case BIDIRECTIONAL -> last.map(l -> l.direction().opposite()).orElse(Direction.ENTRY);
    };
  }
}
