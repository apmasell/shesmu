package ca.on.oicr.gsi.shesmu.compiler;

import ca.on.oicr.gsi.shesmu.compiler.definitions.FunctionDefinition;
import ca.on.oicr.gsi.shesmu.plugin.types.Imyhat;
import java.util.function.Consumer;
import java.util.function.Function;

public class ImyhatNodeUncontainer extends ImyhatNode {
  private final ImyhatNode outer;

  public ImyhatNodeUncontainer(ImyhatNode outer) {
    super();
    this.outer = outer;
  }

  @Override
  public Imyhat render(
      Function<String, Imyhat> definedTypes,
      Function<String, FunctionDefinition> definedFunctions,
      Consumer<String> errorHandler) {
    final Imyhat type = outer.render(definedTypes, definedFunctions, errorHandler);
    if (type instanceof Imyhat.ListImyhat) {
      return ((Imyhat.ListImyhat) type).inner();
    }
    if (type instanceof Imyhat.OptionalImyhat) {
      return ((Imyhat.OptionalImyhat) type).inner();
    }
    errorHandler.accept(
        String.format("Type %s must be list or optional to have something inside.", type.name()));
    return Imyhat.BAD;
  }
}