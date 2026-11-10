package vn.tmc.attendance.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.tmc.attendance.application.port.in.AttemptView;
import vn.tmc.attendance.application.port.in.GateResult;
import vn.tmc.attendance.application.port.in.IAttendanceCommand;
import vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordAttemptCommand;
import vn.tmc.attendance.application.port.in.IAttendanceCommand.RecordDeniedCommand;
import vn.tmc.attendance.application.port.in.IAttendanceQuery;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.LocalStatus;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.OfflineRecord;
import vn.tmc.attendance.application.port.in.IOfflineAttendanceIngest.OfflineResult;
import vn.tmc.attendance.domain.AttemptSource;
import vn.tmc.attendance.domain.Direction;
import vn.tmc.platform.kernel.ReasonCode;

/**
 * Service-to-service API used by Gate Station on the same edge node (IAttendanceCommand / Query /
 * IOfflineAttendanceIngest). Not exposed through the public gateway; protected by mTLS at the edge.
 */
@RestController
@RequestMapping("/internal/v1")
public class InternalAttendanceController {
  private final IAttendanceCommand command;
  private final IAttendanceQuery query;
  private final IOfflineAttendanceIngest offline;

  public InternalAttendanceController(
      IAttendanceCommand command, IAttendanceQuery query, IOfflineAttendanceIngest offline) {
    this.command = command;
    this.query = query;
    this.offline = offline;
  }

  public record AttemptRequest(
      @NotNull UUID attemptId,
      @NotNull UUID employeeUuid,
      @NotNull String gateId,
      @NotNull String factoryCode,
      @NotNull Direction direction,
      @NotNull String jti,
      AttemptSource source,
      @NotNull Instant scannedAt,
      String correlationId) {}

  public record DeniedRequest(
      @NotNull UUID attemptId,
      UUID employeeUuid,
      @NotNull String gateId,
      @NotNull String factoryCode,
      Direction direction,
      String jti,
      @NotNull Instant scannedAt,
      @NotNull ReasonCode reasonCode,
      String correlationId) {}

  public record GateResultRequest(@NotNull GateResult result, String correlationId) {}

  public record OfflineBatchRequest(
      @NotNull String kioskId, @NotNull List<@Valid OfflineItem> records) {}

  public record OfflineItem(
      @NotNull UUID attemptId,
      long journalSeq,
      UUID employeeUuid,
      @NotNull String gateId,
      @NotNull String factoryCode,
      Direction direction,
      String jti,
      @NotNull Instant scannedAt,
      @NotNull LocalStatus localStatus,
      ReasonCode deniedReason) {}

  @PostMapping("/attempts")
  public AttemptView record(@Valid @RequestBody AttemptRequest r) {
    return command.recordAttempt(
        new RecordAttemptCommand(
            r.attemptId(),
            r.employeeUuid(),
            r.gateId(),
            r.factoryCode(),
            r.direction(),
            r.jti(),
            r.source() == null ? AttemptSource.ONLINE : r.source(),
            r.scannedAt(),
            r.correlationId()));
  }

  @PostMapping("/attempts/denied")
  public AttemptView denied(@Valid @RequestBody DeniedRequest r) {
    return command.recordDenied(
        new RecordDeniedCommand(
            r.attemptId(),
            r.employeeUuid(),
            r.gateId(),
            r.factoryCode(),
            r.direction(),
            r.jti(),
            r.scannedAt(),
            r.reasonCode(),
            r.correlationId()));
  }

  @PostMapping("/attempts/{id}/gate-commanded")
  public AttemptView gateCommanded(@PathVariable UUID id) {
    return command.markGateCommanded(id);
  }

  @PostMapping("/attempts/{id}/gate-result")
  public AttemptView gateResult(@PathVariable UUID id, @Valid @RequestBody GateResultRequest r) {
    return command.recordGateResult(id, r.result(), r.correlationId());
  }

  @GetMapping("/attempts/{id}")
  public ResponseEntity<AttemptView> find(@PathVariable UUID id) {
    return query.findAttempt(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/offline-batch")
  public List<OfflineResult> offlineBatch(@Valid @RequestBody OfflineBatchRequest r) {
    var records =
        r.records().stream()
            .map(
                i ->
                    new OfflineRecord(
                        i.attemptId(),
                        i.journalSeq(),
                        i.employeeUuid(),
                        i.gateId(),
                        i.factoryCode(),
                        i.direction(),
                        i.jti(),
                        i.scannedAt(),
                        i.localStatus(),
                        i.deniedReason()))
            .toList();
    return offline.ingest(r.kioskId(), records);
  }
}
