package vn.tmc.turnstilesim;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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
    if (idempotencyKey == null
        || idempotencyKey.isBlank()
        || body == null
        || !(body.get("command_key") instanceof String commandKey)
        || commandKey.isBlank()
        || !(body.get("direction") instanceof String direction)
        || !("ENTRY".equals(direction) || "EXIT".equals(direction))
        || !(body.get("trigger_source") instanceof String trigger)
        || !("QR_SCAN".equals(trigger) || "MANUAL_GUARD_OVERRIDE".equals(trigger))
        || ("MANUAL_GUARD_OVERRIDE".equals(trigger) && !validUuid(body.get("operator_id")))) {
      return ResponseEntity.badRequest().body(Map.of("result", "INVALID_COMMAND"));
    }
    var result = state.open(gateId, idempotencyKey, commandKey);
    if (result.delayMs() > 0) Thread.sleep(result.delayMs());
    return answer(gateId, result);
  }

  private ResponseEntity<Map<String, Object>> answer(String gateId, GateState.OpenResult result) {
    var out = new LinkedHashMap<String, Object>();
    out.put("result", result.result());
    out.put("gate_id", gateId);
    out.put("command_key", result.commandKey());
    out.put("replayed", result.replayed());
    return ResponseEntity.status(result.httpStatus()).body(out);
  }

  private static boolean validUuid(Object value) {
    if (!(value instanceof String text)) return false;
    try {
      UUID.fromString(text);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @PostMapping("/gates/{gateId}/close")
  public Map<String, Object> close(@PathVariable String gateId) {
    state.close(gateId);
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
  public ResponseEntity<Map<String, Object>> setFault(
      @RequestBody(required = false) FaultRequest request) {
    if (request == null || request.mode() == null || request.delayMs() < 0) {
      return ResponseEntity.badRequest().body(Map.of("result", "INVALID_FAULT"));
    }
    state.setFault(request.gateId(), new GateState.Fault(request.mode(), request.delayMs()));
    return ResponseEntity.ok(
        Map.of(
            "gate",
            request.gateId() == null ? "*" : request.gateId(),
            "mode",
            request.mode().name()));
  }

  @DeleteMapping("/admin/fault")
  public Map<String, String> clear() {
    state.clearFaults();
    return Map.of("status", "cleared");
  }
}
