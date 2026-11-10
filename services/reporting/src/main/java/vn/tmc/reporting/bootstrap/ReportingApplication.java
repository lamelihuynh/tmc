package vn.tmc.reporting.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vn.tmc.reporting")
public class ReportingApplication {
  public static void main(String[] args) {
    SpringApplication.run(ReportingApplication.class, args);
  }
}
