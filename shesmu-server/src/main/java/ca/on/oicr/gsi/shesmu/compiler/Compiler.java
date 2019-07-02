package ca.on.oicr.gsi.shesmu.compiler;

import ca.on.oicr.gsi.shesmu.compiler.definitions.ActionDefinition;
import ca.on.oicr.gsi.shesmu.compiler.definitions.ConstantDefinition;
import ca.on.oicr.gsi.shesmu.compiler.definitions.FunctionDefinition;
import ca.on.oicr.gsi.shesmu.compiler.definitions.InputFormatDefinition;
import ca.on.oicr.gsi.shesmu.compiler.definitions.SignatureDefinition;
import ca.on.oicr.gsi.shesmu.compiler.description.FileTable;
import ca.on.oicr.gsi.shesmu.plugin.ErrorConsumer;
import ca.on.oicr.gsi.shesmu.runtime.ActionGenerator;
import ca.on.oicr.gsi.shesmu.runtime.OliveServices;
import ca.on.oicr.gsi.shesmu.server.plugins.PluginManager;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

/** A shell of a compiler that can output bytecode */
public abstract class Compiler {

  private class MaxParseError implements ErrorConsumer {
    private int column;
    private int line;
    private String message = "No error.";

    @Override
    public void raise(int line, int column, String errorMessage) {
      if (this.line < line || this.line == line && this.column <= column) {
        this.line = line;
        this.column = column;
        message = errorMessage;
      }
    }

    public void write() {
      errorHandler(String.format("%d:%d: %s", line, column, message));
    }
  }

  private static final Type A_OLIVE_SERVICES = Type.getType(OliveServices.class);
  private static final Method METHOD_OLIVE_SERVICES__IS_OVERLOADED_ARRAY =
      new Method("isOverloaded", Type.BOOLEAN_TYPE, new Type[] {Type.getType(String[].class)});

  private static final Handle SERVICES_FOR_PLUGINS_BSM =
      new Handle(
          Opcodes.H_INVOKESTATIC,
          Type.getInternalName(PluginManager.class),
          "bootstrapServices",
          Type.getMethodDescriptor(
              Type.getType(CallSite.class),
              Type.getType(MethodHandles.Lookup.class),
              Type.getType(String.class),
              Type.getType(MethodType.class),
              Type.getType(String[].class)),
          false);

  private final boolean skipRender;

  /**
   * Create a new instance of this compiler
   *
   * @param skipRender if true, no bytecode will be generated when compiler is called; only parsing
   *     and checking
   */
  public Compiler(boolean skipRender) {
    super();
    this.skipRender = skipRender;
  }

  /**
   * Compile a program
   *
   * @param input the bytes in the script
   * @param name the internal name of the class to generate; it will extend {@link ActionGenerator}
   * @param path the source file's path for debugging information
   * @return whether compilation was successful
   */
  public final boolean compile(
      byte[] input,
      String name,
      String path,
      Supplier<Stream<ConstantDefinition>> constants,
      Supplier<Stream<SignatureDefinition>> signatures,
      Consumer<FileTable> dashboardOutput) {
    return compile(
        new String(input, StandardCharsets.UTF_8),
        name,
        path,
        constants,
        signatures,
        dashboardOutput);
  }
  /**
   * Compile a program
   *
   * @param input the bytes in the script
   * @param name the internal name of the class to generate; it will extend {@link ActionGenerator}
   * @param path the source file's path for debugging information
   * @return whether compilation was successful
   */
  public final boolean compile(
      String input,
      String name,
      String path,
      Supplier<Stream<ConstantDefinition>> constants,
      Supplier<Stream<SignatureDefinition>> signatures,
      Consumer<FileTable> dashboardOutput) {
    final AtomicReference<ProgramNode> program = new AtomicReference<>();
    final MaxParseError maxParseError = new MaxParseError();
    final boolean parseOk = ProgramNode.parseFile(input, program::set, maxParseError);
    if (!parseOk) {
      maxParseError.write();
    }
    if (parseOk
        && program
            .get()
            .validate(
                this::getInputFormats,
                this::getFunction,
                this::getAction,
                this::errorHandler,
                constants,
                signatures)) {
      final Instant compileTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
      if (dashboardOutput != null && skipRender) {
        dashboardOutput.accept(
            program.get().dashboard(path, compileTime, "Bytecode not available."));
      }
      if (skipRender) {
        return true;
      }
      final List<Textifier> bytecode = new ArrayList<>();
      final RootBuilder builder =
          new RootBuilder(
              compileTime,
              name,
              path,
              program.get().inputFormatDefinition(),
              program.get().timeout(),
              constants,
              signatures) {
            @Override
            protected ClassVisitor createClassVisitor() {
              final ClassVisitor outputVisitor = Compiler.this.createClassVisitor();
              if (dashboardOutput == null) {
                return outputVisitor;
              }
              final Textifier writer = new Textifier();
              bytecode.add(writer);
              return new TraceClassVisitor(outputVisitor, writer, null);
            }
          };
      final Set<Path> pluginFilenames = new TreeSet<>();
      program.get().collectPlugins(pluginFilenames);
      builder.addGuard(
          methodGen -> {
            methodGen.loadArg(0);
            methodGen.invokeDynamic(
                "services",
                Type.getMethodDescriptor(Type.getType(String[].class)),
                SERVICES_FOR_PLUGINS_BSM,
                pluginFilenames.stream().map(Path::toString).toArray());
            methodGen.invokeInterface(A_OLIVE_SERVICES, METHOD_OLIVE_SERVICES__IS_OVERLOADED_ARRAY);
          });
      program.get().render(builder);
      builder.finish();
      if (dashboardOutput != null) {
        dashboardOutput.accept(
            program
                .get()
                .dashboard(
                    path,
                    compileTime,
                    bytecode
                        .stream()
                        .flatMap(t -> t.getText().stream())
                        .flatMap(
                            new Function<Object, Stream<String>>() {

                              @Override
                              public Stream<String> apply(Object object) {
                                if (object instanceof List) {
                                  return ((List<?>) object).stream().flatMap(this);
                                }
                                return Stream.of(object.toString());
                              }
                            })
                        .collect(Collectors.joining())));
      }
      return true;
    }
    return false;
  }

  /** Create a new class visitor for bytecode generation. */
  protected abstract ClassVisitor createClassVisitor();

  /** Report an error to the user. */
  protected abstract void errorHandler(String message);

  /**
   * Get an action by name.
   *
   * @param name the name of the action
   * @return the action definition, or null if no action is available
   */
  protected abstract ActionDefinition getAction(String name);

  /**
   * Get a function by name.
   *
   * @param name the name of the function
   * @return the function or null if no function is available
   */
  protected abstract FunctionDefinition getFunction(String name);

  /**
   * Get a format by name as specified by the “Input” statement at the start of the source file.
   *
   * @param name the name of the input format
   * @return the format definition, or null if no format is available
   */
  protected abstract InputFormatDefinition getInputFormats(String name);
}
