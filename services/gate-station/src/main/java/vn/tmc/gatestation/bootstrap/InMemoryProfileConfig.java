package vn.tmc.gatestation.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import vn.tmc.gatestation.adapter.out.inmem.FakeAttendanceGateway;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryAntiPassbackStore;
import vn.tmc.gatestation.adapter.out.inmem.InMemoryTokenReservation;
import vn.tmc.gatestation.adapter.out.inmem.ProgrammableQrValidation;
import vn.tmc.gatestation.adapter.out.inmem.StaticKioskSyncSource;
import vn.tmc.gatestation.adapter.out.simulated.SimulatedTurnstileAdapter;
import vn.tmc.gatestation.application.port.out.IAntiPassbackStore;
import vn.tmc.gatestation.application.port.out.IKioskSyncSource;
import vn.tmc.gatestation.application.port.out.ITokenReservation;
import vn.tmc.platform.kernel.IClock;

/** Profile "inmem": runs with zero infrastructure and zero peers (tests, first look at the API). */
@Configuration
@Profile("inmem")
public class InMemoryProfileConfig {
  @Bean
  ITokenReservation tokenReservation(IClock clock) {
    return new InMemoryTokenReservation(clock);
  }

  @Bean
  IAntiPassbackStore antiPassbackStore(IClock clock) {
    return new InMemoryAntiPassbackStore(clock);
  }

  @Bean
  FakeAttendanceGateway attendanceGateway() {
    return new FakeAttendanceGateway();
  }

  @Bean
  ProgrammableQrValidation qrValidation() {
    return new ProgrammableQrValidation();
  }

  @Bean
  SimulatedTurnstileAdapter turnstile() {
    return new SimulatedTurnstileAdapter();
  }

  @Bean
  IKioskSyncSource kioskSyncSource() {
    return new StaticKioskSyncSource();
  }
}
