package vn.tmc.userqr.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vn.tmc.userqr")
public class UserQrApplication {
  public static void main(String[] args) {
    SpringApplication.run(UserQrApplication.class, args);
  }
}
