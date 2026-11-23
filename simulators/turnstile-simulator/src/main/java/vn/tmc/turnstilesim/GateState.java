package vn.tmc.turnstilesim;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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

  public record OpenResult(
      int httpStatus, String result, String commandKey, boolean replayed, long delayMs) {}

  private record GateKey(String gateId, String idempotencyKey) {}

  private record StoredResponse(int httpStatus, String result, String commandKey, long delayMs) {}

  private final Map<String, Fault> faults = new ConcurrentHashMap<>();
  private volatile Fault globalFault = Fault.none();
  private final Map<String, CommandRecord> lastCommand = new ConcurrentHashMap<>();
  private final Map<GateKey, StoredResponse> byIdempotencyKey = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> passCount = new ConcurrentHashMap<>();

  public void setFault(String gateId, Fault fault) {
    if (fault == null || fault.mode() == null) fault = Fault.none();
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

  /** Claims and executes a command once per gate and idempotency key, including failures. */
  public OpenResult open(String gateId, String idempotencyKey, String commandKey) {
    var replayed = new AtomicBoolean();
    var response =
        byIdempotencyKey.compute(
            new GateKey(gateId, idempotencyKey),
            (key, old) -> {
              if (old != null) {
                replayed.set(true);
                return old;
              }
              var fault = faultFor(gateId);
              int status;
              String result;
              String observed;
              switch (fault.mode()) {
                case JAM -> {
                  status = 409;
                  result = "MECHANICAL_ARM_JAMMED";
                  observed = result;
                }
                case OFFLINE -> {
                  status = 503;
                  result = "CONTROLLER_OFFLINE";
                  observed = "NOT_OPENED";
                }
                case GATEWAY_TIMEOUT -> {
                  status = 504;
                  result = "GATEWAY_TIMEOUT";
                  observed = "NOT_OPENED";
                }
                case DELAY_NOT_OPENED -> {
                  status = 200;
                  result = "NOT_OPENED";
                  observed = result;
                }
                default -> {
                  status = 200;
                  result = "UNLOCKED";
                  observed = result;
                }
              }
              lastCommand.put(gateId, new CommandRecord(commandKey, observed));
              if ("UNLOCKED".equals(result)) {
                passCount.computeIfAbsent(gateId, k -> new AtomicInteger()).incrementAndGet();
              }
              long delayMs =
                  fault.mode() == FaultMode.DELAY_OPENED
                          || fault.mode() == FaultMode.DELAY_NOT_OPENED
                      ? Math.max(0, fault.delayMs())
                      : 0;
              return new StoredResponse(status, result, commandKey, delayMs);
            });
    return new OpenResult(
        response.httpStatus(),
        response.result(),
        response.commandKey(),
        replayed.get(),
        replayed.get() ? 0 : response.delayMs());
  }

  public void close(String gateId) {
    lastCommand.put(gateId, new CommandRecord(null, "LOCKED"));
  }

  public CommandRecord last(String gateId) {
    return lastCommand.get(gateId);
  }

  public int passes(String gateId) {
    var c = passCount.get(gateId);
    return c == null ? 0 : c.get();
  }
}
