package ca.on.oicr.gsi.shesmu.compiler;

import ca.on.oicr.gsi.shesmu.compiler.InformationNodeTable.ColumnNode;
import ca.on.oicr.gsi.shesmu.compiler.definitions.DefinitionRepository;
import ca.on.oicr.gsi.shesmu.plugin.Parser;
import ca.on.oicr.gsi.shesmu.plugin.Parser.ParseDispatch;
import ca.on.oicr.gsi.shesmu.plugin.action.ActionState;
import ca.on.oicr.gsi.shesmu.plugin.filter.ActionFilter;
import ca.on.oicr.gsi.shesmu.plugin.filter.ActionFilter.ActionFilterNode;
import ca.on.oicr.gsi.shesmu.plugin.filter.AlertFilter;
import ca.on.oicr.gsi.shesmu.plugin.filter.AlertFilter.AlertFilterNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class InformationNode {
  private interface SimulationConstructor {
    InformationNode create(List<ObjectElementNode> constants);
  }

  private static final Parser.ParseDispatch<List<ObjectElementNode>> CONSTANTS =
      new ParseDispatch<>();
  private static final Parser.ParseDispatch<InformationNode> DISPATCH = new ParseDispatch<>();
  private static final Parser.ParseDispatch<Optional<ExpressionNode>> MIME_TYPE =
      new ParseDispatch<>();
  private static final Parser.ParseDispatch<SimulationConstructor> SIMULATION =
      new ParseDispatch<>();

  static {
    CONSTANTS.addKeyword("Let", (p, o) -> p.list(o, ObjectElementNode::parse));
    CONSTANTS.addRaw("nothing", Parser.just(Collections.emptyList()));
    MIME_TYPE.addKeyword(
        "MimeType",
        (p, o) -> p.whitespace().then(ExpressionNode::parse, v -> o.accept(Optional.of(v))));
    MIME_TYPE.addRaw("nothing", Parser.just(Optional.empty()));
    SIMULATION.addKeyword(
        "Existing",
        (p, o) ->
            p.then(
                    ActionFilter.PARSE_STRING,
                    s -> o.accept(c -> new InformationNodeSimulationExisting(c, s)))
                .whitespace());
    SIMULATION.addRaw(
        "script",
        (p, o) -> {
          final AtomicReference<List<RefillerDefinitionNode>> refillers = new AtomicReference<>();
          final AtomicReference<ProgramNode> script = new AtomicReference<>();
          final Parser start =
              p.whitespace()
                  .listEmpty(refillers::set, RefillerDefinitionNode::parse, ';')
                  .whitespace();
          final Parser end = start.then(ProgramNode::parse, script::set);
          if (end.isGood()) {
            final String raw = start.slice(end);
            o.accept(
                c ->
                    new InformationNodeSimulation(
                        p.line(), p.column(), c, refillers.get(), raw, script.get()));
          }
          return end;
        });
    DISPATCH.addKeyword(
        "Alerts",
        (p, o) -> {
          final AtomicReference<AlertFilterNode<InformationParameterNode<String>>> filter =
              new AtomicReference<>();
          final Parser result =
              AlertFilter.parse(p.whitespace(), InformationParameterNode.STRING, filter::set)
                  .whitespace();
          if (result.isGood()) {
            o.accept(new InformationNodeAlerts(filter.get()));
          }
          return result;
        });
    DISPATCH.addKeyword(
        "Actions",
        (p, o) -> {
          final AtomicReference<
                  ActionFilterNode<
                      InformationParameterNode<ActionState>,
                      InformationParameterNode<String>,
                      InformationParameterNode<Instant>,
                      InformationParameterNode<Long>>>
              filter = new AtomicReference<>();
          final Parser result =
              ActionFilter.parse(
                      p.whitespace(),
                      InformationParameterNode.ACTION_STATE,
                      InformationParameterNode.STRINGS,
                      InformationParameterNode.INSTANT,
                      InformationParameterNode.OFFSET,
                      filter::set)
                  .whitespace();
          if (result.isGood()) {
            o.accept(new InformationNodeActions(filter.get()));
          }
          return result;
        });
    DISPATCH.addKeyword(
        "Download",
        (p, o) -> {
          final AtomicReference<ExpressionNode> fileName = new AtomicReference<>();
          final AtomicReference<Optional<ExpressionNode>> mimeType = new AtomicReference<>();
          final AtomicReference<ExpressionNode> contents = new AtomicReference<>();
          final Parser result =
              p.whitespace()
                  .then(ExpressionNode::parse, contents::set)
                  .whitespace()
                  .keyword("To")
                  .whitespace()
                  .then(ExpressionNode::parse, fileName::set)
                  .whitespace()
                  .dispatch(MIME_TYPE, mimeType::set);
          if (result.isGood()) {
            o.accept(new InformationNodeDownload(fileName.get(), mimeType.get(), contents.get()));
          }
          return result;
        });
    DISPATCH.addKeyword(
        "Repeat",
        (p, o) -> {
          final AtomicReference<DestructuredArgumentNode> name = new AtomicReference<>();
          final AtomicReference<SourceNode> source = new AtomicReference<>();
          final AtomicReference<List<ListNode>> transforms = new AtomicReference<>();
          final AtomicReference<List<InformationNode>> collectors = new AtomicReference<>();
          final Parser result =
              p.whitespace()
                  .then(DestructuredArgumentNode::parse, name::set)
                  .whitespace()
                  .then(SourceNode::parse, source::set)
                  .whitespace()
                  .symbol(":")
                  .whitespace()
                  .list(transforms::set, ListNode::parse)
                  .keyword("Begin")
                  .whitespace()
                  .list(collectors::set, InformationNode::parse)
                  .whitespace()
                  .keyword("End")
                  .whitespace();
          if (result.isGood()) {
            o.accept(
                new InformationNodeRepeat(
                    name.get(), source.get(), transforms.get(), collectors.get()));
          }
          return result;
        });
    DISPATCH.addKeyword(
        "Simulate",
        (p, o) -> {
          final AtomicReference<List<ObjectElementNode>> constants = new AtomicReference<>();
          final AtomicReference<SimulationConstructor> simulation = new AtomicReference<>();

          final Parser result =
              p.whitespace()
                  .dispatch(CONSTANTS, constants::set)
                  .dispatch(SIMULATION, simulation::set);
          if (result.isGood()) {
            o.accept(simulation.get().create(constants.get()));
          }
          return result;
        });
    DISPATCH.addKeyword(
        "Table",
        (p, o) -> {
          final AtomicReference<DestructuredArgumentNode> name = new AtomicReference<>();
          final AtomicReference<SourceNode> source = new AtomicReference<>();
          final AtomicReference<List<ListNode>> transforms = new AtomicReference<>();
          final AtomicReference<List<ColumnNode>> columns = new AtomicReference<>();
          final Parser result =
              p.whitespace()
                  .then(DestructuredArgumentNode::parse, name::set)
                  .whitespace()
                  .then(SourceNode::parse, source::set)
                  .whitespace()
                  .symbol(":")
                  .whitespace()
                  .list(transforms::set, ListNode::parse)
                  .whitespace()
                  .list(
                      columns::set,
                      (pc, po) -> {
                        final AtomicReference<String> header = new AtomicReference<>();
                        final AtomicReference<List<DisplayNode>> contents = new AtomicReference<>();
                        final Parser cResult =
                            pc.whitespace()
                                .keyword("Column")
                                .whitespace()
                                .symbol("\"")
                                .regex(
                                    ActionFilter.STRING_CONTENTS,
                                    m -> header.set(m.group(0)),
                                    "string contents")
                                .symbol("\"")
                                .whitespace()
                                .list(contents::set, DisplayNode::parse)
                                .whitespace();
                        if (cResult.isGood()) {
                          po.accept(new ColumnNode(header.get(), contents.get()));
                        }
                        return cResult;
                      })
                  .whitespace();
          if (result.isGood()) {
            o.accept(
                new InformationNodeTable(
                    name.get(), source.get(), transforms.get(), columns.get()));
          }
          return result;
        });
    DISPATCH.addRaw(
        "display",
        (p, o) -> {
          final List<DisplayNode> elements = new ArrayList<>();
          final Parser result =
              p.whitespace()
                  .then(DisplayNode::parse, elements::add)
                  .list(elements::addAll, DisplayNode::parse);
          if (result.isGood()) {
            o.accept(new InformationNodeDisplay(elements));
          }
          return result;
        });
  }

  public static Parser parse(Parser parser, Consumer<InformationNode> output) {
    return parser.whitespace().dispatch(DISPATCH, output).whitespace();
  }

  public abstract String renderEcma(EcmaScriptRenderer renderer);

  public abstract boolean resolve(NameDefinitions defs, Consumer<String> errorHandler);

  public abstract boolean resolveDefinitions(
      ExpressionCompilerServices expressionCompilerServices,
      DefinitionRepository nativeDefinitions,
      Consumer<String> errorHandler);

  public abstract boolean typeCheck(Consumer<String> errorHandler);
}