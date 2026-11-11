package vn.tmc.hrintegration.adapter.out.inmem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import vn.tmc.hrintegration.application.port.out.IHrPayrollPort;

/**
 * Stand-in for HR/Payroll: remembers what it was sent and answers whatever the test or demo asks
 * for. TODO Team D: HttpHrPayrollAdapter against docs/api/hr-payroll.openapi.yaml.
 */
public class SimulatedHrPayrollAdapter implements IHrPayrollPort {
  private final List<Submission> received = new CopyOnWriteArrayList<>();
  private volatile SubmitResult next = SubmitResult.ACCEPTED;

  /** Every later call answers with this result until it is changed again. */
  public void respondWith(SubmitResult result) {
    this.next = result;
  }

  public List<Submission> received() {
    return List.copyOf(received);
  }

  @Override
  public SubmitResult submit(Submission submission) {
    received.add(submission);
    return next;
  }
}
