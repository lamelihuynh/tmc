package vn.tmc.platform.kernel;

import java.util.function.Function;

/**
 * Minimal sum type for expected business outcomes (use exceptions only for unexpected failures).
 */
public sealed interface Result<T, E> {

  record Ok<T, E>(T value) implements Result<T, E> {}

  record Err<T, E>(E error) implements Result<T, E> {}

  static <T, E> Result<T, E> ok(T value) {
    return new Ok<>(value);
  }

  static <T, E> Result<T, E> err(E error) {
    return new Err<>(error);
  }

  default boolean isOk() {
    return this instanceof Ok<T, E>;
  }

  default <R> R fold(Function<T, R> onOk, Function<E, R> onErr) {
    return switch (this) {
      case Ok<T, E> ok -> onOk.apply(ok.value());
      case Err<T, E> err -> onErr.apply(err.error());
    };
  }
}
