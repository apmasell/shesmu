package ca.on.oicr.gsi.shesmu.json;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.shesmu.plugin.Definer;
import ca.on.oicr.gsi.shesmu.plugin.Tuple;
import ca.on.oicr.gsi.shesmu.plugin.functions.FunctionParameter;
import ca.on.oicr.gsi.shesmu.plugin.json.JsonPluginFile;
import ca.on.oicr.gsi.shesmu.plugin.json.UnpackJson;
import ca.on.oicr.gsi.shesmu.plugin.types.Imyhat;
import ca.on.oicr.gsi.status.SectionRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;

public class StructuredConfigFile extends JsonPluginFile<Configuration> {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private Set<String> badRecords = Collections.emptySet();
  private final Definer<StructuredConfigFile> definer;

  public StructuredConfigFile(
      Path fileName, String instanceName, Definer<StructuredConfigFile> definer) {
    super(fileName, instanceName, MAPPER, Configuration.class);
    this.definer = definer;
  }

  @Override
  public void configuration(SectionRenderer renderer) throws XMLStreamException {
    for (final String badRecord : badRecords) {
      renderer.line("Bad record", badRecord);
    }
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  @Override
  protected Optional<Integer> update(Configuration value) {
    final Imyhat.ObjectImyhat type =
        new Imyhat.ObjectImyhat(
            value
                .getTypes()
                .entrySet()
                .stream()
                .map(e -> new Pair<>(e.getKey(), Imyhat.parse(e.getValue()))));
    final Set<String> badRecords = new TreeSet<>();
    final Map<String, Optional<Tuple>> values =
        value
            .getValues()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      final AtomicBoolean ok = new AtomicBoolean(true);
                      final Object[] convertedValues =
                          type.fields()
                              .sorted(Comparator.comparing(field -> field.getValue().second()))
                              .map(
                                  field -> {
                                    try {
                                      return field
                                          .getValue()
                                          .first()
                                          .apply(
                                              new UnpackJson(
                                                  e.getValue()
                                                      .getOrDefault(
                                                          field.getKey(),
                                                          value
                                                              .getDefaults()
                                                              .getOrDefault(
                                                                  field.getKey(),
                                                                  NullNode.getInstance()))));
                                    } catch (Exception ex) {
                                      ex.printStackTrace();
                                      ok.set(false);
                                      return null;
                                    }
                                  })
                              .toArray();
                      return ok.get() ? Optional.of(new Tuple(convertedValues)) : Optional.empty();
                    }));
    this.badRecords = badRecords;
    definer.clearFunctions();
    definer.defineFunction(
        this.name(),
        "JSON configuration from " + fileName(),
        type.asOptional(),
        args -> values.getOrDefault(args[0], Optional.empty()),
        new FunctionParameter("Lookup key", Imyhat.STRING));
    return Optional.empty();
  }
}