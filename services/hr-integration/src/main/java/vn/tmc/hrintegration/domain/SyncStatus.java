package vn.tmc.hrintegration.domain;

/** Sync states of document.md section 5.9. UNKNOWN is not a failure and not a success. */
public enum SyncStatus {
  PENDING,
  IN_PROGRESS,
  SYNCED,
  FAILED,
  RETRY_PENDING,
  UNKNOWN
}
