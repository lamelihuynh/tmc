package vn.tmc.auditadmin.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tmc.auditadmin.adapter.out.inmem.InMemoryAuditStore;
import vn.tmc.auditadmin.adapter.out.persistence.JdbcAuditStore;
import vn.tmc.auditadmin.application.port.in.IAuditQuery;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.application.usecase.AuditQueryService;
import vn.tmc.auditadmin.application.usecase.RecordAuditUseCase;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.ProcessedMessageStore;

@Configuration
@Import(DedupStoreConfig.class)
public class AuditAdminConfig {
  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  RecordAuditUseCase recordAudit(IAuditStore store) {
    return new RecordAuditUseCase(store);
  }

  @Bean
  IdempotentConsumer auditConsumer(
      ProcessedMessageStore store,
      IdempotentConsumer.TransactionRunner tx,
      RecordAuditUseCase record) {
    return new IdempotentConsumer("audit-admin", store, record::handle, tx);
  }

  @Bean
  IAuditQuery auditQuery(IAuditStore store, RecordAuditUseCase record, IClock clock) {
    return new AuditQueryService(store, record, clock);
  }

  /** No database: the log lives in process memory (still hash-chained) and is lost on restart. */
  @Configuration
  @Profile({"inmem", "local"})
  static class InMemory {
    @Bean
    IAuditStore auditStore() {
      return new InMemoryAuditStore();
    }
  }

  /**
   * PostgreSQL (D-08): table audit_entries, insert-only for the service login, hash-chained. Same
   * DataSource as the dedup table, so an entry commits with its processed_messages marker.
   */
  @Configuration
  @Profile("!inmem & !local")
  static class Postgres {
    @Bean
    IAuditStore auditStore(JdbcTemplate jdbc, PlatformTransactionManager tm, ObjectMapper json) {
      return new JdbcAuditStore(jdbc, new TransactionTemplate(tm), json);
    }
  }
}
