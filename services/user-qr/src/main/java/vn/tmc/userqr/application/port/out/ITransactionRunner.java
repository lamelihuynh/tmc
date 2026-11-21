package vn.tmc.userqr.application.port.out;

import java.util.function.Supplier;

/**
 * Driven port: runs a unit of work in one local transaction, so the use cases stay free of Spring.
 * A device change and the audit row that describes it (outbox) commit or roll back together
 * (ADR-04, C-08).
 */
public interface ITransactionRunner {
  <T> T inTransaction(Supplier<T> work);

  /** No transaction at all: for unit tests that wire the use case by hand. */
  static ITransactionRunner none() {
    return Supplier::get;
  }
}
