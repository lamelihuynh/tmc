package vn.tmc.auditadmin.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vn.tmc.auditadmin")
public class AuditAdminApplication {
  public static void main(String[] args) {
    SpringApplication.run(AuditAdminApplication.class, args);
  }
}
