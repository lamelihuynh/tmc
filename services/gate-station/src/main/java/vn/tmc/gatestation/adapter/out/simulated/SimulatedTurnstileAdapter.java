package vn.tmc.gatestation.adapter.out.simulated;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort;

/**
 * In-process turnstile for tests and for running without the turnstile-simulator service
 * (tmc.gate.turnstile.mode=simulated). It remembers what each command really did, so the "answer
 * was lost but the gate opened" case can be reproduced and then resolved through {@link
 * #queryCommand}.
 */
public class SimulatedTurnstileAdapter implements ITurnstileControlPort {

  public enum Fault {
    NONE,
    /** The controller refuses: nothing opens, the caller gets ERROR. */
    ERROR,
    /** The gate opens but the answer is lost: the caller gets TIMEOUT. */
    TIMEOUT_BUT_OPENED,
    /** The gate does not open and no answer arrives: the caller gets TIMEOUT. */
    TIMEOUT_NOT_OPENED
  }

  private final Map<String, Boolean> openedByCommandKey = new ConcurrentHashMap<>();
  private final AtomicInteger openCalls = new AtomicInteger();
  private volatile Fault fault = Fault.NONE;
  private volatile boolean statusAvailable = true;
  private volatile OpenGateRequest lastRequest;

  public void setFault(Fault fault) {
    this.fault = fault;
  }

  /** When false, the status query cannot tell what a command did (controller unreachable). */
  public void setStatusAvailable(boolean available) {
    this.statusAvailable = available;
  }

  /** How many open commands were sent. A correct flow never sends two for the same attempt. */
  public int openCalls() {
    return openCalls.get();
  }

  public OpenGateRequest lastRequest() {
    return lastRequest;
  }

  @Override
  public CommandResult openGate(OpenGateRequest request) {
    openCalls.incrementAndGet();
    lastRequest = request;
    return switch (fault) {
      case NONE -> {
        openedByCommandKey.put(request.commandKey(), true);
        yield CommandResult.OPENED;
      }
      case ERROR -> {
        openedByCommandKey.put(request.commandKey(), false);
        yield CommandResult.ERROR;
      }
      case TIMEOUT_BUT_OPENED -> {
        openedByCommandKey.put(request.commandKey(), true);
        yield CommandResult.TIMEOUT;
      }
      case TIMEOUT_NOT_OPENED -> {
        openedByCommandKey.put(request.commandKey(), false);
        yield CommandResult.TIMEOUT;
      }
    };
  }

  @Override
  public CommandObservation queryCommand(String hardwareGateId, String commandKey) {
    if (!statusAvailable) {
      return CommandObservation.UNDETERMINED;
    }
    var opened = openedByCommandKey.get(commandKey);
    if (opened == null) {
      return CommandObservation.UNDETERMINED;
    }
    return opened ? CommandObservation.OPENED : CommandObservation.NOT_OPENED;
  }
}
