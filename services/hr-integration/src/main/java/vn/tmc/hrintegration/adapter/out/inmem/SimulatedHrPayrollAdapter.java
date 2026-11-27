package vn.tmc.hrintegration.adapter.out.inmem;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;

/**
 * Stand-in for HR/Payroll: remembers what it was sent and answers whatever the test or demo asks
 * for. Unless told otherwise, a lookup answers ACCEPTED for a batch it accepted and NOT_FOUND for
 * one it never received, like a real HR would.
 */
public class SimulatedHrPayrollAdapter implements IHrPayrollPort {
  private final List<Submission> received = new CopyOnWriteArrayList<>();
  private final List<UUID> accepted = new CopyOnWriteArrayList<>();
  private volatile SubmitResult next = SubmitResult.ACCEPTED;
  private volatile LookupResult lookupResult;

  /** Every later call answers with this result until it is changed again. */
  public void respondWith(SubmitResult result) {
    this.next = result;
  }

  /** Every later lookup answers with this result; null goes back to the default behaviour. */
  public void respondToLookupWith(LookupResult result) {
    this.lookupResult = result;
  }

  public List<Submission> received() {
    return List.copyOf(received);
  }

  @Override
  public LookupResult lookup(UUID batchId) {
    var forced = lookupResult;
    if (forced != null) {
      return forced;
    }
    return accepted.contains(batchId) ? LookupResult.ACCEPTED : LookupResult.NOT_FOUND;
  }

  @Override
  public SubmitResult submit(Submission submission) {
    received.add(submission);
    var result = next;
    if (result == SubmitResult.ACCEPTED) {
      accepted.add(submission.batchId());
    }
    return result;
  }
}
