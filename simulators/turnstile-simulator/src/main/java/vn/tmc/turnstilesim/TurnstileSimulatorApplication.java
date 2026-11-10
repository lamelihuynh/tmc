package vn.tmc.turnstilesim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fake of the existing Turnstile Control System (document.md section 16.3) so the edge stack can be
 * run and tested without hardware. Faults are switched at runtime through /admin/fault.
 */
@SpringBootApplication
public class TurnstileSimulatorApplication {
  public static void main(String[] args) {
    SpringApplication.run(TurnstileSimulatorApplication.class, args);
  }
}
