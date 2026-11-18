package vn.tmc.attendance.application.port.out;

import java.util.function.Supplier;

/**
 * Driven port: runs a unit of work in one local transaction, so use cases stay free of Spring. All
 * repository calls inside {@code work} commit or roll back together (ADR-04).
 */
public interface ITransactionRunner {
  <T> T inTransaction(Supplier<T> work);
}
