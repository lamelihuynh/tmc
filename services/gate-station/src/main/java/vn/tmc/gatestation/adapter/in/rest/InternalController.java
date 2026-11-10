package vn.tmc.gatestation.adapter.in.rest;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.gatestation.application.port.in.IGateObservation;

/** Called by Attendance reconcile. mTLS-protected, not exposed to kiosks or the public gateway. */
@RestController
public class InternalController {
  private final IGateObservation observation;

  public InternalController(IGateObservation observation) {
    this.observation = observation;
  }

  @GetMapping("/internal/v1/attempts/{attemptId}/gate-observation")
  public Map<String, String> observe(@PathVariable UUID attemptId, @RequestParam String gateId) {
    return Map.of("observation", observation.observe(attemptId, gateId).name());
  }
}
