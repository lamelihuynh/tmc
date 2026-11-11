package vn.tmc.attendance.adapter.out.inmem;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import vn.tmc.attendance.application.port.out.ITransactionRunner;

/**
 * Transaction runner for the in-memory profile: one global lock, so units of work are serialized
 * like SERIALIZABLE transactions. There is no rollback: a unit of work that throws midway leaves
 * what it already wrote. That is acceptable for tests and the local demo only; the real guarantee
 * comes from {@code SpringTransactionRunner} on PostgreSQL.
 */
public class InMemoryStore implements ITransactionRunner {
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
