package vn.tmc.hrintegration.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.application.usecase.PayrollDeadlineCheck;
import vn.tmc.platform.kernel.IClock;

@Configuration
@EnableScheduling
public class PayrollDeadlineConfig {
  private static final Logger LOG = LoggerFactory.getLogger(PayrollDeadlineConfig.class);

  @Bean
  PayrollDeadlineCheck payrollDeadlineCheck(ISyncLogRepository log, IClock clock) {
    return new PayrollDeadlineCheck(log, clock);
  }

  @Bean
  DeadlineJob payrollDeadlineJob(PayrollDeadlineCheck check) {
    return new DeadlineJob(check);
  }

  public static class DeadlineJob {
    private final PayrollDeadlineCheck check;

    public DeadlineJob(PayrollDeadlineCheck check) {
      this.check = check;
    }

    @Scheduled(cron = "0 0 8,12 1 * *", zone = PayrollDeadlineCheck.ZONE)
    public void run() {
      check
          .check()
          .ifPresent(
              alert -> {
                var message =
                    "HR payroll deadline level={} period={} outstanding={} recipients=HR_STAFF,SYSTEM_ADMIN";

                if (alert.level() == PayrollDeadlineCheck.Level.OVERDUE) {
                  LOG.error(message, alert.level(), alert.period(), alert.outstanding());
                } else {
                  LOG.warn(message, alert.level(), alert.period(), alert.outstanding());
                }
              });
    }
  }
}
