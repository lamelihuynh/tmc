package vn.tmc.turnstilesim;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** State of the simulated controllers, keyed by hardware gate id. Idempotent per command key. */
public class GateState {

  public enum FaultMode {
    NONE,
    /** 409: mechanical arm jammed, nothing opens. */
    JAM,
    /** 503: controller offline, nothing opens. */
    OFFLINE,
    /** 504 from an upstream proxy: unknown whether it opened (it did not). */
    GATEWAY_TIMEOUT,
    /**
     * Opens, but answers only after delayMs (use > client read timeout to lose the
     * acknowledgement).
     */
    DELAY_OPENED,
    /** Does not open and answers only after delayMs. */
    DELAY_NOT_OPENED
  }

  public record Fault(FaultMode mode, long delayMs) {
    public static Fault none() {
      return new Fault(FaultMode.NONE, 0);
    }
  }

  public record CommandRecord(String commandKey, String result) {}

  private final Map<String, Fault> faults = new ConcurrentHashMap<>();
  private volatile Fault globalFault = Fault.none();
  private final Map<String, CommandRecord> lastCommand = new ConcurrentHashMap<>();
  private final Map<String, String> byIdempotencyKey = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> passCount = new ConcurrentHashMap<>();

  public void setFault(String gateId, Fault fault) {
    if (gateId == null || gateId.isBlank() || "*".equals(gateId)) {
      globalFault = fault;
    } else {
      faults.put(gateId, fault);
    }
  }

  public void clearFaults() {
    faults.clear();
    globalFault = Fault.none();
  }

  public Fault faultFor(String gateId) {
    return faults.getOrDefault(gateId, globalFault);
  }

  /**
   * @return the stored result when this idempotency key was already handled, else null
   */
  public String replay(String idempotencyKey) {
    return idempotencyKey == null ? null : byIdempotencyKey.get(idempotencyKey);
  }

  public void record(String gateId, String idempotencyKey, String commandKey, String result) {
    if (idempotencyKey != null) {
      byIdempotencyKey.put(idempotencyKey, result);
    }
    lastCommand.put(gateId, new CommandRecord(commandKey, result));
    if ("UNLOCKED".equals(result)) {
      passCount.computeIfAbsent(gateId, k -> new AtomicInteger()).incrementAndGet();
    }
  }

  public CommandRecord last(String gateId) {
    return lastCommand.get(gateId);
  }

  public int passes(String gateId) {
    var c = passCount.get(gateId);
    return c == null ? 0 : c.get();
  }
}
