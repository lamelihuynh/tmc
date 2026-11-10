package vn.tmc.gatestation.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "vn.tmc.gatestation")
@EnableConfigurationProperties(GateProperties.class)
public class GateStationApplication {
  public static void main(String[] args) {
    SpringApplication.run(GateStationApplication.class, args);
  }
}
