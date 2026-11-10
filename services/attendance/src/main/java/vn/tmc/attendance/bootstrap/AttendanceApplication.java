package vn.tmc.attendance.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "vn.tmc.attendance")
@EnableScheduling
public class AttendanceApplication {
  public static void main(String[] args) {
    SpringApplication.run(AttendanceApplication.class, args);
  }
}
