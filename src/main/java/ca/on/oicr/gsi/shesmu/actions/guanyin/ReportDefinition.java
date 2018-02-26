package ca.on.oicr.gsi.shesmu.actions.guanyin;

import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.on.oicr.gsi.shesmu.ActionDefinition;
import ca.on.oicr.gsi.shesmu.Imyhat;
import ca.on.oicr.gsi.shesmu.ParameterDefinition;
import ca.on.oicr.gsi.shesmu.actions.rest.LaunchRemote;
import ca.on.oicr.gsi.shesmu.actions.util.JsonParameter;

public class ReportDefinition extends ActionDefinition {
	static final Type A_LAUNCH_REMOTE_TYPE = Type.getType(LaunchRemote.class);
	private static final Type A_STRING_TYPE = Type.getType(String.class);
	private static final Type ACTION_TYPE = Type.getType(RunReport.class);

	private static final Method CTOR = new Method("<init>", Type.VOID_TYPE,
			new Type[] { A_STRING_TYPE, A_STRING_TYPE, A_STRING_TYPE, A_STRING_TYPE, A_STRING_TYPE });

	private static Stream<ParameterDefinition> transform(ObjectNode parameters) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(parameters.fields(), 0), false).map(entry -> {
			final Imyhat type = Imyhat.parse(entry.getValue().asText());
			return new JsonParameter(entry.getKey(), type, true);
		});
	}

	private final String category;
	private final String drmaaUrl;
	private final String name;
	private final String version;

	private final String 观音Url;

	public ReportDefinition(String drmaaUrl, String 观音Url, String category, String name, String version,
			ObjectNode parameters) {
		super(name + "_" + version.replaceAll("[^A-Za-z0-9_]", "_"), ACTION_TYPE, transform(parameters));
		this.drmaaUrl = drmaaUrl;
		this.观音Url = 观音Url;
		this.category = category;
		this.name = name;
		this.version = version;
	}

	@Override
	public void initialize(GeneratorAdapter methodGen) {
		methodGen.newInstance(ACTION_TYPE);
		methodGen.dup();
		methodGen.push(观音Url);
		methodGen.push(drmaaUrl);
		methodGen.push(category);
		methodGen.push(name);
		methodGen.push(version);
		methodGen.invokeConstructor(ACTION_TYPE, CTOR);
	}

}
