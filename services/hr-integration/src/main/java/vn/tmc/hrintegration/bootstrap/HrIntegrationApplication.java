package vn.tmc.hrintegration.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vn.tmc.hrintegration")
public class HrIntegrationApplication {
  public static void main(String[] args) {
    SpringApplication.run(HrIntegrationApplication.class, args);
  }
}
