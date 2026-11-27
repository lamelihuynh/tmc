package vn.tmc.userqr.adapter.out.inmem;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import vn.tmc.userqr.application.port.out.ITransactionRunner;

/**
 * Transaction runner for the inmem and local profiles: one global lock, so units of work run one
 * after the other. There is no rollback: a unit of work that throws midway keeps what it already
 * wrote. Good enough for tests and the demo; the real guarantee is {@code SpringTransactionRunner}
 * on PostgreSQL.
 */
public class InMemoryTransactionRunner implements ITransactionRunner {
  private final ReentrantLock lock = new ReentrantLock();

  @Override
  public <T> T inTransaction(Supplier<T> work) {
    lock.lock();
    try {
      return work.get();
    } finally {
      lock.unlock();
    }
  }
}
