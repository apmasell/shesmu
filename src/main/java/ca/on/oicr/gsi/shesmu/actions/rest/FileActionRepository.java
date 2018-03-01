package ca.on.oicr.gsi.shesmu.actions.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.kohsuke.MetaInfServices;

import ca.on.oicr.gsi.shesmu.ActionDefinition;
import ca.on.oicr.gsi.shesmu.ActionRepository;
import ca.on.oicr.gsi.shesmu.Pair;
import ca.on.oicr.gsi.shesmu.RuntimeSupport;

@MetaInfServices
public final class FileActionRepository implements ActionRepository {

	public static Stream<ActionDefinition> of(Optional<String> input) {
		return roots(input).flatMap(FileActionRepository::queryActionsCatalog);
	}

	private static Stream<ActionDefinition> queryActionsCatalog(Path file) {
		try {
			final FileDefinition fileDef = RuntimeSupport.MAPPER.readValue(Files.readAllBytes(file),
					FileDefinition.class);
			return Arrays.stream(fileDef.getDefinitions()).map(def -> def.toDefinition(fileDef.getUrl()));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return Stream.empty();
	}

	private static Stream<Path> roots(Optional<String> input) {
		return input.<Stream<Path>>map(directory -> {
			try (Stream<Path> files = Files.walk(Paths.get(directory), 1)) {
				final List<Path> list = files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".actions"))
						.collect(Collectors.toList());
				return list.stream();
			} catch (final IOException e) {
				e.printStackTrace();
				return Stream.empty();
			}
		}).orElse(Stream.empty());
	}

	@Override
	public Stream<Pair<String, Map<String, String>>> listConfiguration() {
		return RuntimeSupport.environmentVariable().map(path -> {
			final Map<String, String> map = new TreeMap<>();
			map.put("path", path);
			return Stream.of(new Pair<>("File Action Repositories", map));
		}).orElseGet(Stream::empty);
	}

	@Override
	public Stream<ActionDefinition> query() {
		return of(RuntimeSupport.environmentVariable());
	}

}
