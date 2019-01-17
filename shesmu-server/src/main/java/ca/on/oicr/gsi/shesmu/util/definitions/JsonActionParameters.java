package ca.on.oicr.gsi.shesmu.util.definitions;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface JsonActionParameters {
  JsonActionParameter[] value();
}
