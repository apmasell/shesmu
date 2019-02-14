package ca.on.oicr.gsi.shesmu.plugin.types;

import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Cpllect a value based on the provided information
 *
 * <p>In some cases, a Shesmu value may be passed to Java as {@link Object} and an accompanying
 * {@link Imyhat}.
 *
 * @see Imyhat#accept(ImyhatConsumer, Object)
 */
public interface ImyhatConsumer {
  void accept(boolean value);

  /** Collect a Boolean value */
  void accept(Instant value);

  /** Collect an integral value */
  void accept(long value);

  /**
   * Collect a path value
   *
   * @param value
   */
  void accept(Path value);

  /**
   * Collect items in a list
   *
   * @param values the values in a list
   * @param inner the type of the values
   */
  void accept(Stream<Object> values, Imyhat inner);

  /** Collect a string */
  void accept(String value);

  /**
   * Collect an object
   *
   * @param fields the names, values, and types in the object
   */
  void acceptObject(Stream<Field<String>> fields);

  /**
   * Collect a tuple
   *
   * @param fields the indices, values, and types in the tuple
   */
  void acceptTuple(Stream<Field<Integer>> fields);
}