package vn.tmc.turnstilesim;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SimulatorConfig {
  @Bean
  GateState gateState() {
    return new GateState();
  }
}
