package vn.tmc.attendance.adapter.out.inmem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import vn.tmc.attendance.application.port.out.IAttendanceCorrectionRepository;
import vn.tmc.attendance.domain.AttendanceCorrection;

/** Process-memory corrections with the same uniqueness rule as the table. */
public class InMemoryAttendanceCorrectionRepository implements IAttendanceCorrectionRepository {
  private final List<AttendanceCorrection> rows = new CopyOnWriteArrayList<>();

  @Override
  public Optional<AttendanceCorrection> findById(UUID correctionId) {
    return rows.stream().filter(c -> c.correctionId().equals(correctionId)).findFirst();
  }

  @Override
  public synchronized boolean add(AttendanceCorrection correction) {
    boolean taken =
        rows.stream()
            .anyMatch(
                r ->
                    r.correctionId().equals(correction.correctionId())
                        || (r.originalAttemptId().equals(correction.originalAttemptId())
                            && r.revision() == correction.revision()));
    if (taken) {
      return false;
    }
    rows.add(correction);
    return true;
  }

  @Override
  public List<AttendanceCorrection> forAttempt(UUID originalAttemptId) {
    return rows.stream()
        .filter(r -> r.originalAttemptId().equals(originalAttemptId))
        .sorted((a, b) -> Integer.compare(a.revision(), b.revision()))
        .toList();
  }
}
