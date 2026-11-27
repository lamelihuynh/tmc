package vn.tmc.hrintegration.application.usecase;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;
import vn.tmc.hrintegration.application.port.out.ISyncLogRepository;
import vn.tmc.hrintegration.domain.SyncStatus;
import vn.tmc.platform.kernel.IClock;

public class PayrollDeadlineCheck {
  public static final String ZONE = "Asia/Ho_Chi_Minh";

  public enum Level {
    WARNING,
    OVERDUE
  }

  public record Alert(YearMonth period, Level level, long outstanding) {}

  private final ISyncLogRepository log;
  private final IClock clock;

  public PayrollDeadlineCheck(ISyncLogRepository log, IClock clock) {
    this.log = log;
    this.clock = clock;
  }

  public Optional<Alert> check() {
    var now = clock.now().atZone(ZoneId.of(ZONE));
    if (now.getDayOfMonth() != 1 || now.getHour() < 8) {
      return Optional.empty();
    }

    var period = YearMonth.from(now).minusMonths(1);
    long outstanding = 0;
    for (var status : SyncStatus.values()) {
      if (status != SyncStatus.SYNCED) {
        outstanding +=
            log.byStatus(status).stream()
                .filter(record -> period.equals(record.attendancePeriod()))
                .count();
      }
    }

    if (outstanding == 0) {
      return Optional.empty();
    }

    var level = now.getHour() < 12 ? Level.WARNING : Level.OVERDUE;
    return Optional.of(new Alert(period, level, outstanding));
  }
}
