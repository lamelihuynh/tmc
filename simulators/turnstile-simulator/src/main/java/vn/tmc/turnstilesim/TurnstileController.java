package vn.tmc.turnstilesim;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TurnstileController {
  private final GateState state;

  public TurnstileController(GateState state) {
    this.state = state;
  }

  @PostMapping("/gates/{gateId}/open")
  public ResponseEntity<Map<String, Object>> open(
      @PathVariable String gateId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody(required = false) Map<String, Object> body)
      throws InterruptedException {
    var commandKey = body == null ? null : String.valueOf(body.get("command_key"));

    var replayed = state.replay(idempotencyKey);
    if (replayed != null) { // a retried command must not unlock a second time
      return answer(gateId, commandKey, replayed, true);
    }

    var fault = state.faultFor(gateId);
    switch (fault.mode()) {
      case JAM -> {
        state.record(gateId, idempotencyKey, commandKey, "MECHANICAL_ARM_JAMMED");
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("result", "MECHANICAL_ARM_JAMMED", "gate_id", gateId));
      }
      case OFFLINE -> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("result", "CONTROLLER_OFFLINE", "gate_id", gateId));
      }
      case GATEWAY_TIMEOUT -> {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(Map.of("result", "GATEWAY_TIMEOUT", "gate_id", gateId));
      }
      case DELAY_OPENED -> {
        state.record(gateId, idempotencyKey, commandKey, "UNLOCKED"); // it really opens
        Thread.sleep(fault.delayMs());
        return answer(gateId, commandKey, "UNLOCKED", false);
      }
      case DELAY_NOT_OPENED -> {
        state.record(gateId, idempotencyKey, commandKey, "NOT_OPENED");
        Thread.sleep(fault.delayMs());
        return answer(gateId, commandKey, "NOT_OPENED", false);
      }
      default -> {
        state.record(gateId, idempotencyKey, commandKey, "UNLOCKED");
        return answer(gateId, commandKey, "UNLOCKED", false);
      }
    }
  }

  private ResponseEntity<Map<String, Object>> answer(
      String gateId, String commandKey, String result, boolean replay) {
    var out = new LinkedHashMap<String, Object>();
    out.put("result", result);
    out.put("gate_id", gateId);
    out.put("command_key", commandKey);
    out.put("replayed", replay);
    return ResponseEntity.ok(out);
  }

  @PostMapping("/gates/{gateId}/close")
  public Map<String, Object> close(@PathVariable String gateId) {
    return Map.of("result", "LOCKED", "gate_id", gateId);
  }

  /**
   * Reports the last command so a caller can resolve a timeout without sending another open (ADR-11
   * P3).
   */
  @GetMapping("/gates/{gateId}/status")
  public Map<String, Object> status(@PathVariable String gateId) {
    var out = new LinkedHashMap<String, Object>();
    var fault = state.faultFor(gateId);
    out.put(
        "state",
        fault.mode() == GateState.FaultMode.OFFLINE
            ? "OFFLINE"
            : fault.mode() == GateState.FaultMode.JAM ? "JAMMED" : "ONLINE");
    var last = state.last(gateId);
    out.put("last_command_key", last == null ? null : last.commandKey());
    out.put("last_command_result", last == null ? null : last.result());
    out.put("pass_count", state.passes(gateId));
    return out;
  }

  public record FaultRequest(String gateId, GateState.FaultMode mode, long delayMs) {}

  @PostMapping("/admin/fault")
  public Map<String, Object> setFault(@RequestBody FaultRequest request) {
    state.setFault(request.gateId(), new GateState.Fault(request.mode(), request.delayMs()));
    return Map.of(
        "gate", request.gateId() == null ? "*" : request.gateId(), "mode", request.mode().name());
  }

  @DeleteMapping("/admin/fault")
  public Map<String, String> clear() {
    state.clearFaults();
    return Map.of("status", "cleared");
  }
}
