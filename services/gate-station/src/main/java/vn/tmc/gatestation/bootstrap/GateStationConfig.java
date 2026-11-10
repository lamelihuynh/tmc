package vn.tmc.gatestation.bootstrap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryEmployeeDirectory;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryGateConfigRepository;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryHeartbeatStore;
import vn.tmc.gatestation.adapter.out.inmem.LoggingAuditLog;
import vn.tmc.gatestation.application.port.in.IGateAccess;
import vn.tmc.gatestation.application.port.in.IGateManualAccess;
import vn.tmc.gatestation.application.port.in.IGateObservation;
import vn.tmc.gatestation.application.port.in.IKioskSupport;
import vn.tmc.gatestation.application.port.out.IAntiPassbackStore;
import vn.tmc.gatestation.application.port.out.IAttendanceGateway;
import vn.tmc.gatestation.application.port.out.IAuditLog;
import vn.tmc.gatestation.application.port.out.IEmployeeDirectory;
import vn.tmc.gatestation.application.port.out.IGateConfigRepository;
import vn.tmc.gatestation.application.port.out.IHeartbeatStore;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;
import vn.tmc.gatestation.application.port.out.IQRValidation;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.gatestation.application.port.out.ITurnstileControlPort;
import vn.tmc.gatestation.application.usecase.KioskSupportService;
import vn.tmc.gatestation.application.usecase.ManualOpenUseCase;
import vn.tmc.gatestation.application.usecase.ObserveGateUseCase;
import vn.tmc.gatestation.application.usecase.RecordScanUseCase;
import vn.tmc.gatestation.application.usecase.ScanSettings;
import vn.tmc.gatestation.domain.policy.AccessPolicy;
import vn.tmc.gatestation.domain.policy.AccessPolicyChain;
import vn.tmc.gatestation.domain.policy.ActiveEmployeeRule;
import vn.tmc.gatestation.domain.policy.AntiPassbackRule;
import vn.tmc.gatestation.domain.policy.FactoryPermissionRule;
import vn.tmc.platform.kernel.IClock;

/**
 * Composition root of Gate Station. Rules are collected from the context: to add a rule, declare
 * another AccessPolicy bean (OCP). No business class carries a Spring annotation.
 */
@Configuration
public class GateStationConfig {

  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  AccessPolicy activeEmployeeRule() {
    return new ActiveEmployeeRule();
  }

  @Bean
  AccessPolicy factoryPermissionRule() {
    return new FactoryPermissionRule();
  }

  @Bean
  AccessPolicy antiPassbackRule() {
    return new AntiPassbackRule();
  }

  /** Extra rules (for example a shift window) join the chain after the three standard ones. */
  @Bean
  AccessPolicyChain accessPolicyChain(List<AccessPolicy> rules) {
    var ordered = new ArrayList<AccessPolicy>();
    rules.stream().filter(r -> r instanceof ActiveEmployeeRule).forEach(ordered::add);
    rules.stream().filter(r -> r instanceof FactoryPermissionRule).forEach(ordered::add);
    rules.stream().filter(r -> r instanceof AntiPassbackRule).forEach(ordered::add);
    rules.stream()
        .filter(
            r ->
                !(r instanceof ActiveEmployeeRule
                    || r instanceof FactoryPermissionRule
                    || r instanceof AntiPassbackRule))
        .forEach(ordered::add);
    return new AccessPolicyChain(ordered);
  }

  @Bean
  ScanSettings scanSettings(GateProperties props) {
    var s = props.scan();
    if (s == null) {
      return ScanSettings.defaults();
    }
    return new ScanSettings(
        Duration.ofSeconds(s.reservationTtlSeconds()),
        Duration.ofSeconds(s.pendingTtlSeconds()),
        Duration.ofSeconds(s.tokenBlockSeconds()));
  }

  @Bean
  IGateConfigRepository gateConfigRepository(GateProperties props) {
    return new InMemoryGateConfigRepository(
        props.gates().stream().map(GateProperties.Gate::toConfig).toList());
  }

  @Bean
  IEmployeeDirectory employeeDirectory(GateProperties props) {
    return new InMemoryEmployeeDirectory(
        props.employees().stream().map(GateProperties.Employee::toAccess).toList());
  }

  @Bean
  IHeartbeatStore heartbeatStore() {
    return new InMemoryHeartbeatStore();
  }

  @Bean
  IAuditLog auditLog() {
    return new LoggingAuditLog();
  }

  @Bean
  IGateAccess gateAccess(
      IGateConfigRepository gates,
      IQRValidation qr,
      IEmployeeDirectory employees,
      AccessPolicyChain policies,
      ITokenReservation tokens,
      IAntiPassbackStore antiPassback,
      IAttendanceGateway attendance,
      ITurnstileControlPort turnstile,
      ScanSettings settings,
      IClock clock) {
    return new RecordScanUseCase(
        gates,
        qr,
        employees,
        policies,
        tokens,
        antiPassback,
        attendance,
        turnstile,
        settings,
        clock);
  }

  @Bean
  IGateManualAccess gateManualAccess(
      IGateConfigRepository gates,
      IEmployeeDirectory employees,
      IAttendanceGateway attendance,
      ITurnstileControlPort turnstile,
      IAuditLog audit,
      IClock clock) {
    return new ManualOpenUseCase(gates, employees, attendance, turnstile, audit, clock);
  }

  @Bean
  IGateObservation gateObservation(IGateConfigRepository gates, ITurnstileControlPort turnstile) {
    return new ObserveGateUseCase(gates, turnstile);
  }

  @Bean
  IKioskSupport kioskSupport(
      IGateConfigRepository gates,
      IKioskSyncSource source,
      IHeartbeatStore heartbeats,
      IAttendanceGateway attendance,
      IClock clock) {
    return new KioskSupportService(gates, source, heartbeats, attendance, clock);
  }
}
