package vn.tmc.auditadmin.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.tmc.auditadmin.adapter.out.inmem.InMemoryAuditStore;
import vn.tmc.auditadmin.application.port.in.IAuditQuery;
import vn.tmc.auditadmin.application.port.out.IAuditStore;
import vn.tmc.auditadmin.application.usecase.AuditQueryService;
import vn.tmc.auditadmin.application.usecase.RecordAuditUseCase;
import vn.tmc.platform.kernel.IClock;
import vn.tmc.platform.messaging.IdempotentConsumer;
import vn.tmc.platform.messaging.InMemoryProcessedMessageStore;
import vn.tmc.platform.messaging.ProcessedMessageStore;

@Configuration
public class AuditAdminConfig {
  @Bean
  IClock clock() {
    return IClock.system();
  }

  @Bean
  IAuditStore auditStore() {
    return new InMemoryAuditStore();
  }

  @Bean
  ProcessedMessageStore processedMessageStore() {
    return new InMemoryProcessedMessageStore();
  }

  @Bean
  RecordAuditUseCase recordAudit(IAuditStore store) {
    return new RecordAuditUseCase(store);
  }

  @Bean
  IdempotentConsumer auditConsumer(ProcessedMessageStore store, RecordAuditUseCase record) {
    return new IdempotentConsumer("audit-admin", store, record::handle);
  }

  @Bean
  IAuditQuery auditQuery(IAuditStore store, RecordAuditUseCase record, IClock clock) {
    return new AuditQueryService(store, record, clock);
  }
}
