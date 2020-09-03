package ca.on.oicr.gsi.shesmu.runtime;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.shesmu.plugin.SourceLocation;
import ca.on.oicr.gsi.shesmu.plugin.action.Action;
import ca.on.oicr.gsi.shesmu.plugin.dumper.Dumper;
import ca.on.oicr.gsi.shesmu.plugin.types.Imyhat;
import io.prometheus.client.Gauge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class MonitoredOliveServices implements OliveServices, AutoCloseable {
  private static class AlertInfo {
    List<String> annotations = new ArrayList<>();
    Set<SourceLocation> locations = new HashSet<>();
    long ttl;
  }

  private static final Gauge actionCount =
      Gauge.build(
              "shesmu_olive_action_count",
              "The number of unique actions produced during the last run of a script.")
          .labelNames("filename")
          .register();
  private static final Gauge alertCount =
      Gauge.build(
              "shesmu_olive_alert_count",
              "The number of unique alerts produced during the last run of a script.")
          .labelNames("filename")
          .register();
  private static final Gauge newActionCount =
      Gauge.build(
              "shesmu_olive_new_action_count",
              "The number of unique actions produced during the last run of a script that were previously unknown to the scheduler.")
          .labelNames("filename")
          .register();
  private static final Gauge newAlertCount =
      Gauge.build(
              "shesmu_olive_new_alert_count",
              "The number of unique alerts produced during the last run of a script that were previously unknown to the scheduler.")
          .labelNames("filename")
          .register();
  private final Map<Action, Pair<Set<String>, Set<SourceLocation>>> actions = new HashMap<>();
  private final Map<List<String>, AlertInfo> alerts = new HashMap<>();
  private final OliveServices backing;
  private final String filename;

  public MonitoredOliveServices(OliveServices backing, String filename) {
    this.backing = backing;
    this.filename = filename;
  }

  @Override
  public boolean accept(
      Action action, String filename, int line, int column, String hash, String[] tags) {
    final Pair<Set<String>, Set<SourceLocation>> pair =
        actions.computeIfAbsent(action, k -> new Pair<>(new TreeSet<>(), new HashSet<>()));
    pair.first().addAll(Arrays.asList(tags));
    return pair.second().add(new SourceLocation(filename, line, column, hash));
  }

  @Override
  public boolean accept(
      String[] labels,
      String[] annotation,
      long ttl,
      String filename,
      int line,
      int column,
      String hash)
      throws Exception {
    final AlertInfo alert = alerts.computeIfAbsent(Arrays.asList(labels), k -> new AlertInfo());
    // This is going to massively duplicate the annotations, but the action processor will
    // de-duplicate them and resolve any conflicts
    alert.annotations.addAll(Arrays.asList(annotation));
    alert.ttl = Math.max(alert.ttl, ttl);
    return alert.locations.add(new SourceLocation(filename, line, column, hash));
  }

  public void close() throws Exception {
    int newActions = 0;
    for (final Entry<Action, Pair<Set<String>, Set<SourceLocation>>> entry : actions.entrySet()) {
      final String[] tags = entry.getValue().first().stream().toArray(String[]::new);
      for (final SourceLocation location : entry.getValue().second()) {
        if (!backing.accept(
            entry.getKey(),
            location.fileName(),
            location.line(),
            location.column(),
            location.hash(),
            tags)) {
          newActions++;
        }
      }
    }

    int newAlerts = 0;
    for (final Entry<List<String>, AlertInfo> entry : alerts.entrySet()) {
      final String[] labels = entry.getKey().stream().toArray(String[]::new);
      final String[] annotations = entry.getValue().annotations.stream().toArray(String[]::new);
      for (final SourceLocation location : entry.getValue().locations) {
        if (!backing.accept(
            labels,
            annotations,
            entry.getValue().ttl,
            location.fileName(),
            location.line(),
            location.column(),
            location.hash())) {
          newAlerts++;
        }
      }
    }

    actionCount.labels(filename).set(actions.size());
    newActionCount.labels(filename).set(newActions);
    alertCount.labels(filename).set(alerts.size());
    newAlertCount.labels(filename).set(newAlerts);
  }

  @Override
  public Dumper findDumper(String name, Imyhat... types) {
    return backing.findDumper(name, types);
  }

  @Override
  public boolean isOverloaded(String... services) {
    return backing.isOverloaded(services);
  }

  @Override
  public <T> Stream<T> measureFlow(
      Stream<T> input, String filename, int line, int column, int oliveLine, int oliveColumn) {
    return backing.measureFlow(input, filename, line, column, oliveLine, oliveColumn);
  }

  @Override
  public void oliveRuntime(String filename, int line, int column, long timeInNs) {
    backing.oliveRuntime(filename, line, column, timeInNs);
  }
}