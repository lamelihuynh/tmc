package vn.tmc.gatestation.support;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import vn.tmc.gatestation.adapter.out.inmem.FakeAttendanceGateway;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryAntiPassbackStore;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryEmployeeDirectory;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryGateConfigRepository;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryTokenReservation;
import vn.tmc.gatestation.adapter.out.inmem.LoggingAuditLog;
import vn.tmc.gatestation.adapter.out.inmem.ProgrammableQrValidation;
import vn.tmc.gatestation.adapter.out.simulated.SimulatedTurnstileAdapter;
import vn.tmc.gatestation.application.port.in.IGateAccess.ScanRequest;
import vn.tmc.gatestation.application.usecase.ManualOpenUseCase;
import vn.tmc.gatestation.application.usecase.ObserveGateUseCase;
import vn.tmc.gatestation.application.usecase.RecordScanUseCase;
import vn.tmc.gatestation.application.usecase.ScanSettings;
import vn.tmc.gatestation.domain.EmployeeAccess;
import vn.tmc.gatestation.domain.GateConfig;
import vn.tmc.gatestation.domain.GateMode;
import vn.tmc.gatestation.domain.policy.AccessPolicyChain;
import vn.tmc.platform.kernel.Ids;

/** The whole gate-station application on in-memory adapters; no Spring. */
public class GateFixture {
  public static final UUID E1 = UUID.fromString("0192a1c0-0000-7000-8000-000000000001"); // BD + LA
  public static final UUID E2 = UUID.fromString("0192a1c0-0000-7000-8000-000000000002"); // LA only
  public static final UUID E3 = UUID.fromString("0192a1c0-0000-7000-8000-000000000003"); // inactive

  public final MutableClock clock = new MutableClock();
  public final InMemoryGateConfigRepository gates =
      new InMemoryGateConfigRepository(
          List.of(
              new GateConfig("BD-GATE-01", "BD", "BD-HW-01", GateMode.ENTRY_ONLY, false),
              new GateConfig("BD-GATE-02", "BD", "BD-HW-02", GateMode.EXIT_ONLY, false),
              new GateConfig("BD-GATE-03", "BD", "BD-HW-03", GateMode.BIDIRECTIONAL, false),
              new GateConfig("BD-GATE-MT", "BD", "BD-HW-MT", GateMode.ENTRY_ONLY, true)));
  public final InMemoryEmployeeDirectory employees =
      new InMemoryEmployeeDirectory(
          List.of(
              new EmployeeAccess(E1, true, Set.of("BD", "LA")),
              new EmployeeAccess(E2, true, Set.of("LA")),
              new EmployeeAccess(E3, false, Set.of("BD"))));
  public final ProgrammableQrValidation qr = new ProgrammableQrValidation();
  public final InMemoryTokenReservation tokens = new InMemoryTokenReservation(clock);
  public final InMemoryAntiPassbackStore antiPassback = new InMemoryAntiPassbackStore(clock);
  public final FakeAttendanceGateway attendance = new FakeAttendanceGateway();
  public final SimulatedTurnstileAdapter turnstile = new SimulatedTurnstileAdapter();
  public final LoggingAuditLog audit = new LoggingAuditLog();

  public final RecordScanUseCase scan;
  public final ManualOpenUseCase manual;
  public final ObserveGateUseCase observe;

  public GateFixture() {
    this(AccessPolicyChain.standard());
  }

  public GateFixture(AccessPolicyChain chain) {
    scan =
        new RecordScanUseCase(
            gates,
            qr,
            employees,
            chain,
            tokens,
            antiPassback,
            attendance,
            turnstile,
            ScanSettings.defaults(),
            clock);
    manual = new ManualOpenUseCase(gates, employees, attendance, turnstile, audit, clock);
    observe = new ObserveGateUseCase(gates, turnstile);
  }

  /** Registers a token the fake QR verifier will accept. */
  public String token(String name, UUID employee, String jti) {
    qr.accept(name, employee, jti, clock.now().plusSeconds(30));
    return name;
  }

  public ScanRequest request(UUID attemptId, String gateId, String token) {
    return new ScanRequest(attemptId, gateId, token, clock.now(), "KIOSK-1", "corr-1");
  }

  public ScanRequest request(String gateId, String token) {
    return request(Ids.uuidV7(), gateId, token);
  }
}
