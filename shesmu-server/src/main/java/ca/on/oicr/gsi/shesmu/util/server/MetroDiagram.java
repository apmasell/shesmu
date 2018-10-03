package ca.on.oicr.gsi.shesmu.util.server;

import java.awt.Canvas;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ca.on.oicr.gsi.shesmu.ActionGenerator;
import ca.on.oicr.gsi.shesmu.Imyhat;
import ca.on.oicr.gsi.shesmu.InputFormatDefinition;
import ca.on.oicr.gsi.shesmu.Pair;
import ca.on.oicr.gsi.shesmu.SourceLocation;
import ca.on.oicr.gsi.shesmu.compiler.Target;
import ca.on.oicr.gsi.shesmu.compiler.description.OliveClauseRow;
import ca.on.oicr.gsi.shesmu.compiler.description.OliveTable;
import ca.on.oicr.gsi.shesmu.compiler.description.VariableInformation;

public class MetroDiagram {
	private static final class DeathChecker implements Predicate<OliveClauseRow> {
		private boolean done;

		public Stream<VariableInformation> liveVariables(OliveTable olive) {
			final List<VariableInformation> clauseInput = olive.clauses()//
					.filter(this)//
					.flatMap(OliveClauseRow::variables)//
					.collect(Collectors.toList());
			if (done) {
				return clauseInput.stream();
			}
			return Stream.concat(clauseInput.stream(), olive.variables());
		}

		@Override
		public boolean test(OliveClauseRow clause) {
			if (done) {
				return false;
			}
			done = clause.deadly();
			return true;
		}
	}

	private static final String[] COLOURS = new String[] { "#d09c2e", "#5b7fee", "#bacd4c", "#503290", "#8bc151",
			"#903691", "#46ca79", "#db64c3", "#63bb5b", "#af74db", "#9fa627", "#5a5dc0", "#72891f", "#578ae2",
			"#c96724", "#38b3eb", "#c34f32", "#34d3ca", "#be2e68", "#4ec88c", "#be438d", "#53d1a8", "#d54a4a",
			"#319466", "#d486d8", "#417c25", "#4b2f75", "#c3b857", "#3b5ba0", "#e09c4e", "#6d95db", "#9f741f",
			"#826bb9", "#78bb73", "#802964", "#a8bd69", "#b995e2", "#346e2e", "#d97eb8", "#6e6f24", "#e36f96",
			"#c29b59", "#862644", "#da8b57", "#d2506f", "#8d4e19", "#d34b5b", "#832520", "#d06c72", "#ce7058" };
	private static final long SVG_CONTROL_DISTANCE = 15;
	private static final long SVG_COUNT_START = 90;
	private static final long SVG_METRO_WIDTH = 25;
	private static final long SVG_RADIUS = 3;
	private static final long SVG_ROW_HEIGHT = 64;
	private static final long SVG_SMALL_TEXT = 10;
	private static final long SVG_TEXT_BASELINE = 30;
	private static final long SVG_TITLE_START = 100;
	private static final String CLAUSE_HEADER = "Clause (Line:Column)";

	public static void draw(PrintStream writer, String filename, Instant timestamp, OliveTable olive, long inputCount,
			InputFormatDefinition format) {
		final long metroStart = 120 + Stream.concat(//
				olive.clauses()//
						.map(OliveClauseRow::syntax), //
				Stream.of(olive.syntax(), //
						CLAUSE_HEADER))//
				.mapToLong(new Canvas().getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 18))::stringWidth)//
				.max().orElse(100);
		final long height = SVG_ROW_HEIGHT * (olive.clauses().count() + 3); // Padding + Input + Clauses + Output
		final long width = metroStart + SVG_METRO_WIDTH * (Stream.<VariableInformation>concat(//
				olive.clauses()//
						.flatMap(OliveClauseRow::variables), //
				olive.variables())//
				.flatMap(variable -> Stream.concat(Stream.of(variable.name()), variable.inputs()))//
				.distinct()//
				.count() + 1);
		writer.print("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"");
		writer.print(width);
		writer.print("\" height=\"");
		writer.print(height);
		writer.print("\" viewBox=\"0 0 ");
		writer.print(width);
		writer.print(" ");
		writer.print(height);
		writer.print(
				"\" version=\"1.1\"><defs><filter id=\"blur\" x=\"0\" y=\"0\"><feFlood flood-color=\"white\"/><feComposite in2=\"SourceGraphic\" operator=\"in\"/><feGaussianBlur stdDeviation=\"2\"/><feComponentTransfer><feFuncA type=\"gamma\" exponent=\".5\" amplitude=\"2\"/></feComponentTransfer><feComposite in=\"SourceGraphic\"/><feComposite in=\"SourceGraphic\"/></filter></defs>");
		writer.printf(
				"<text text-anchor=\"end\" x=\"%2$d\" y=\"%1$d\" style=\"font-weight:bold\">Records</text><text x=\"%3$d\" y=\"%1$d\" style=\"font-weight:bold\">%4$s</text>",
				SVG_ROW_HEIGHT, SVG_COUNT_START, SVG_TITLE_START, CLAUSE_HEADER);
		final AtomicInteger idGen = new AtomicInteger();
		final AtomicInteger row = new AtomicInteger(2);
		final ByteArrayOutputStream textLayerBuffer = new ByteArrayOutputStream();
		try (PrintStream textLayer = new PrintStream(textLayerBuffer, true, "UTF-8")) {

			final Map<String, MetroDiagram> initialVariables = new DeathChecker().liveVariables(olive)//
					.flatMap(VariableInformation::inputs)//
					.distinct()//
					.sorted()//
					.collect(Collectors.toMap(Function.identity(), name -> {
						final int colour = idGen.getAndIncrement();
						return new MetroDiagram(textLayer, writer, name, format.baseStreamVariables()//
								.filter(var -> var.name().equals(name))//
								.map(Target::type)//
								.findFirst()//
								.orElse(Imyhat.BAD), "", colour, 1, colour, metroStart);
					}));

			final SourceLocation source = new SourceLocation(filename, olive.line(), olive.column(), timestamp);
			writeClause(writer, 1, "Input", inputCount, source);

			final Map<String, MetroDiagram> terminalVariables = olive.clauses().reduce(initialVariables,
					(variables, clause) -> {
						final int currentRow = row.getAndIncrement();
						writeClause(writer, currentRow, clause.syntax(),
								clause.measuredFlow()
										? (long) ActionGenerator.OLIVE_FLOW.labels(filename,
												Integer.toString(clause.line()), Integer.toString(clause.column()))
												.get()
										: null,
								new SourceLocation(filename, clause.line(), clause.column(), timestamp));

						return drawVariables(textLayer, writer, metroStart, idGen, variables, clause::variables,
								currentRow);
					}, (a, b) -> {
						throw new UnsupportedOperationException();
					});
			writeClause(writer, row.get(), olive.syntax(), null, source);
			drawVariables(textLayer, writer, metroStart, idGen, terminalVariables, olive::variables, row.get());
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		writer.println(new String(textLayerBuffer.toByteArray(), StandardCharsets.UTF_8));
		writer.print("</svg>");

	}

	private static Map<String, MetroDiagram> drawVariables(PrintStream textLayer, PrintStream connectorLayer,
			long metroStart, AtomicInteger idGen, Map<String, MetroDiagram> variables,
			Supplier<Stream<VariableInformation>> information, int row) {
		final Map<String, Integer> outputVariableColumns = Stream.concat(//
				information.get().map(VariableInformation::name), //
				variables.keySet().stream())//
				.sorted()//
				.distinct()//
				.map(Pair.number())//
				.collect(Collectors.toMap(Pair::second, p -> (int) p.first()));

		final Map<String, MetroDiagram> outputVariables = new HashMap<>();

		information.get().forEach(variable -> {
			final Pair<Integer, Integer> currentPoint = new Pair<>(outputVariableColumns.get(variable.name()), row);
			switch (variable.behaviour()) {
			case DEFINITION:
				// If we have a defined variable, then it always needs to be drawn
				final MetroDiagram newVariable = new MetroDiagram(textLayer, connectorLayer, variable.name(),
						variable.type(), from(variable, variables), idGen.getAndIncrement(), row,
						outputVariableColumns.get(variable.name()), metroStart);
				variable.inputs().forEach(input -> variables.get(input).drawConnector(newVariable.start()));
				outputVariables.put(variable.name(), newVariable);
				break;
			case OBSERVER:
				final MetroDiagram observedVariable = variables.get(variable.name());
				observedVariable.drawDot(currentPoint, "used");
				break;
			case PASSTHROUGH:
				final MetroDiagram passthroughVariable = variables.get(variable.name());
				passthroughVariable.drawSquare(currentPoint);
				break;
			default:
				break;

			}
		});

		for (final Entry<String, Integer> entry : outputVariableColumns.entrySet()) {
			if (outputVariables.containsKey(entry.getKey())) {
				continue;
			}
			final MetroDiagram variable = variables.get(entry.getKey());
			variable.append(new Pair<>(entry.getValue(), row));
			outputVariables.put(entry.getKey(), variable);
		}

		return outputVariables;
	}

	private static String from(VariableInformation variable, Map<String, MetroDiagram> variables) {
		if (variable.inputs().count() == 0) {
			return " de novo";
		}
		return variable.inputs()//
				.map(variables::get)//
				.map(v -> v.title)//
				.collect(Collectors.joining(", ", " from ", ""));
	}

	private static void writeClause(PrintStream writer, int row, String title, Long count, SourceLocation location) {
		if (count != null) {
			writer.printf("<text text-anchor=\"end\" x=\"%d\" y=\"%d\">%,d</text>", SVG_COUNT_START,
					SVG_ROW_HEIGHT * row + SVG_TEXT_BASELINE, count);
		}
		final Optional<String> url = location.url();
		url.ifPresent(u -> writer.printf("<a xlink:href=\"%s\" xlink:title=\"View Source\" xlink:show=\"new\">", u));
		writer.printf("<text x=\"%d\" y=\"%d\">%s (%d:%d)</text>", SVG_TITLE_START,
				SVG_ROW_HEIGHT * row + SVG_TEXT_BASELINE, title, location.line(), location.column());
		url.ifPresent(u -> writer.println("🔗</a>"));
	}

	private final String colour;

	private final PrintStream connectorLayer;

	private final long metroStart;

	private final Queue<Pair<Integer, Integer>> segments = new LinkedList<>();
	private final Pair<Integer, Integer> start;
	private final PrintStream textLayer;
	private final String title;

	private MetroDiagram(PrintStream textLayer, PrintStream connectorLayer, String name, Imyhat type, String from,
			int colour, int row, int column, long metroStart) {
		this.textLayer = textLayer;
		this.connectorLayer = connectorLayer;
		title = name + " (" + type.name() + ")";
		this.metroStart = metroStart;
		this.colour = COLOURS[colour % COLOURS.length];
		start = new Pair<>(column, row);
		segments.offer(start);
		drawDot(start, "defined" + from);
		final long x = metroStart + column * SVG_METRO_WIDTH + SVG_METRO_WIDTH / 2;
		final long y = SVG_ROW_HEIGHT * row + SVG_ROW_HEIGHT / 2;
		textLayer.printf(
				"<text transform=\"rotate(-45, %1$d, %2$d)\" x=\"%3$d\" y=\"%4$d\" fill=\"#000\" filter=\"url(#blur)\" font-size=\"%5$d\"><title>%7$s</title>%6$s</text>",
				x, y, x + SVG_RADIUS * 2, y + SVG_RADIUS * 2, SVG_SMALL_TEXT, name, type.name());
	}

	public void append(Pair<Integer, Integer> point) {
		segments.add(point);
	}

	private void drawConnector(Pair<Integer, Integer> output) {
		if (segments.size() == 1 && segments.peek().equals(output)) {
			return;
		}
		connectorLayer.printf("<path stroke=\"%s\" fill=\"none\" d=\"M %d %d", colour, xCoordinate(segments.peek()),
				yCoordinate(segments.peek()));
		while (segments.size() > 1) {
			drawSegment(segments.poll(), segments.peek());
		}
		drawSegment(segments.peek(), output);
		connectorLayer.printf("\"><title>%s</title></path>\n", title);
	}

	private void drawDot(Pair<Integer, Integer> point, String verb) {
		drawConnector(point);
		textLayer.printf("<circle r=\"%d\" cx=\"%d\" cy=\"%d\" fill=\"%s\"><title>%s %s</title></circle>", SVG_RADIUS,
				metroStart + point.first() * SVG_METRO_WIDTH + SVG_METRO_WIDTH / 2,
				SVG_ROW_HEIGHT * point.second() + SVG_ROW_HEIGHT / 2, colour, title, verb);
	}

	private void drawSegment(Pair<Integer, Integer> input, Pair<Integer, Integer> output) {
		final long inputX = xCoordinate(input);
		final long outputX = xCoordinate(output);
		final long inputY = yCoordinate(input);
		final long outputY = yCoordinate(output);
		if (inputX == outputX) {
			connectorLayer.printf("L %d %d ", outputX, outputY);
		} else {
			connectorLayer.printf("C %d %d %d %d %d %d ", //
					inputX, inputY + SVG_CONTROL_DISTANCE, // control point
					outputX, outputY - SVG_CONTROL_DISTANCE, // control point
					outputX, outputY); // final point
		}
	}

	private void drawSquare(Pair<Integer, Integer> point) {
		drawConnector(point);
		textLayer.printf(
				"<rect width=\"%d\" height=\"%d\" x=\"%d\" y=\"%d\" fill=\"%s\"><title>Group By %s</title></rect>",
				SVG_RADIUS * 4, SVG_RADIUS * 4,
				metroStart + point.first() * SVG_METRO_WIDTH + SVG_METRO_WIDTH / 2 - SVG_RADIUS * 2,
				SVG_ROW_HEIGHT * point.second() + SVG_ROW_HEIGHT / 2 - SVG_RADIUS * 2, colour, title);
	}

	private Pair<Integer, Integer> start() {
		return start;
	}

	private long xCoordinate(Pair<Integer, Integer> point) {
		return metroStart + point.first() * SVG_METRO_WIDTH + SVG_METRO_WIDTH / 2;
	}

	private long yCoordinate(Pair<Integer, Integer> point) {
		return point.second() * SVG_ROW_HEIGHT + SVG_ROW_HEIGHT / 2;
	}
}