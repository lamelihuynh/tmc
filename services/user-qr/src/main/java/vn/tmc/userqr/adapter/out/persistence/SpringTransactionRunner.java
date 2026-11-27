package vn.tmc.userqr.adapter.out.persistence;

import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tmc.userqr.application.port.out.ITransactionRunner;

/**
 * One PostgreSQL transaction per unit of work; an exception rolls everything back. The device
 * registry and the outbox use JdbcTemplates over the same DataSource, so both join it.
 */
public class SpringTransactionRunner implements ITransactionRunner {
  private final TransactionTemplate template;

  public SpringTransactionRunner(TransactionTemplate template) {
    this.template = template;
  }

  @Override
  public <T> T inTransaction(Supplier<T> work) {
    return template.execute(status -> work.get());
  }
}
