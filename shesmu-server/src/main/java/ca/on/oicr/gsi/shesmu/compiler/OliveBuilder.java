package ca.on.oicr.gsi.shesmu.compiler;

import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import ca.on.oicr.gsi.shesmu.ActionGenerator;
import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;

/**
 * An olive that will result in an action being performed
 */
public final class OliveBuilder extends BaseOliveBuilder {
	private static final Type A_ACTION_GENERATOR_TYPE = Type.getType(ActionGenerator.class);

	private static final Type A_CHILD_TYPE = Type.getType(Gauge.Child.class);

	private static final Type A_CONSUMER_TYPE = Type.getType(Consumer.class);

	private static final Type A_GAUGE_TYPE = Type.getType(Gauge.class);

	private static final Type A_STRING_TYPE = Type.getType(String.class);

	private static final Type A_SUPPLIER_TYPE = Type.getType(Supplier.class);
	private static final Type A_SYSTEM_TYPE = Type.getType(System.class);
	private static final Method METHOD_CHILD__SET = new Method("set", VOID_TYPE, new Type[] { DOUBLE_TYPE });
	private static final Method METHOD_CONSUMER__ACCEPT = new Method("accept", VOID_TYPE, new Type[] { A_OBJECT_TYPE });

	private static final Method METHOD_GAUGE__LABELS = new Method("labels", Type.getType(Object.class),
			new Type[] { Type.getType(String[].class) });

	private static final Method METHOD_STREAM__FOR_EACH = new Method("forEach", VOID_TYPE,
			new Type[] { A_CONSUMER_TYPE });

	private static final Method METHOD_SUPPLIER__GET = new Method("get", A_OBJECT_TYPE, new Type[] {});

	private static final Method METHOD_SYSTEM__NANO_TIME = new Method("nanoTime", LONG_TYPE, new Type[] {});

	private final int line;

	public OliveBuilder(RootBuilder owner, int oliveId, Type initialType, int line) {
		super(owner, oliveId, initialType);
		this.line = line;
	}

	/**
	 * Run an action
	 *
	 * Consume an action from the stack and queue to be executed by the server
	 *
	 * @param methodGen
	 *            the method generator, which must be the method generator produced
	 *            by {@link #finish()}
	 */
	public void emitAction(GeneratorAdapter methodGen, int local) {
		methodGen.loadArg(0);
		methodGen.loadLocal(local);
		methodGen.invokeInterface(A_CONSUMER_TYPE, METHOD_CONSUMER__ACCEPT);
	}

	/**
	 * Generate bytecode for the olive and create a method to consume the result.
	 */
	public final Renderer finish() {
		final GeneratorAdapter runMethod = owner.rootRenderer().methodGen();
		final int startTime = runMethod.newLocal(LONG_TYPE);
		runMethod.invokeStatic(A_SYSTEM_TYPE, METHOD_SYSTEM__NANO_TIME);
		runMethod.storeLocal(startTime);

		runMethod.loadArg(1);
		runMethod.invokeInterface(A_SUPPLIER_TYPE, METHOD_SUPPLIER__GET);
		runMethod.checkCast(A_STREAM_TYPE);

		steps.forEach(step -> step.accept(owner.rootRenderer()));

		runMethod.loadThis();
		runMethod.loadArg(0);
		final Method method = new Method(String.format("olive_%d_consume", oliveId), VOID_TYPE,
				new Type[] { A_CONSUMER_TYPE, currentType() });
		final Handle handle = new Handle(Opcodes.H_INVOKEVIRTUAL, owner.selfType().getInternalName(), method.getName(),
				method.getDescriptor(), false);
		runMethod.invokeDynamic("accept", Type.getMethodDescriptor(A_CONSUMER_TYPE, owner.selfType(), A_CONSUMER_TYPE),
				LAMBDA_METAFACTORY_BSM, Type.getMethodType(VOID_TYPE, A_OBJECT_TYPE), handle,
				Type.getMethodType(VOID_TYPE, currentType()));
		runMethod.invokeInterface(A_STREAM_TYPE, METHOD_STREAM__FOR_EACH);

		runMethod.getStatic(A_ACTION_GENERATOR_TYPE, "OLIVE_RUN_TIME", A_GAUGE_TYPE);
		runMethod.push(2);
		runMethod.newArray(A_STRING_TYPE);
		runMethod.dup();
		runMethod.push(0);
		runMethod.push(owner.sourcePath());
		runMethod.arrayStore(A_STRING_TYPE);
		runMethod.dup();
		runMethod.push(1);
		runMethod.push(Integer.toString(line));
		runMethod.arrayStore(A_STRING_TYPE);
		runMethod.invokeVirtual(A_GAUGE_TYPE, METHOD_GAUGE__LABELS);
		runMethod.checkCast(A_CHILD_TYPE);
		runMethod.invokeStatic(A_SYSTEM_TYPE, METHOD_SYSTEM__NANO_TIME);
		runMethod.loadLocal(startTime);
		runMethod.math(GeneratorAdapter.SUB, LONG_TYPE);
		runMethod.cast(LONG_TYPE, DOUBLE_TYPE);
		runMethod.push(Collector.NANOSECONDS_PER_SECOND);
		runMethod.math(GeneratorAdapter.DIV, DOUBLE_TYPE);
		runMethod.invokeVirtual(A_CHILD_TYPE, METHOD_CHILD__SET);

		return new Renderer(owner, new GeneratorAdapter(Opcodes.ACC_PRIVATE, method, null, null, owner.classVisitor), 1,
				currentType(), loadableValues());
	}

	@Override
	public Stream<LoadableValue> loadableValues() {
		return owner.constants();
	}

}
