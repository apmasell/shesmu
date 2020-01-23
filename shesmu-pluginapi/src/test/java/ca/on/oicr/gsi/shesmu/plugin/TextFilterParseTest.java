package ca.on.oicr.gsi.shesmu.plugin;

import ca.on.oicr.gsi.shesmu.plugin.action.ActionState;
import ca.on.oicr.gsi.shesmu.plugin.filter.FilterBuilder;
import ca.on.oicr.gsi.shesmu.plugin.filter.FilterJson;
import ca.on.oicr.gsi.shesmu.plugin.filter.LocationJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class TextFilterParseTest {
  private abstract static class TestFilterBuilder implements FilterBuilder<Boolean> {
    @Override
    public Boolean added(Optional<Instant> start, Optional<Instant> end) {
      return false;
    }

    @Override
    public Boolean and(Stream<Boolean> filters) {
      return filters.allMatch(x -> x);
    }

    @Override
    public Boolean checked(Optional<Instant> start, Optional<Instant> end) {
      return false;
    }

    @Override
    public Boolean external(Optional<Instant> start, Optional<Instant> end) {
      return false;
    }

    @Override
    public Boolean fromFile(String... files) {
      return false;
    }

    @Override
    public Boolean fromSourceLocation(Stream<LocationJson> locations) {
      return false;
    }

    @Override
    public Boolean isState(ActionState... states) {
      return false;
    }

    @Override
    public Boolean negate(Boolean filter) {
      return filter;
    }

    @Override
    public Boolean or(Stream<Boolean> filters) {
      return filters.allMatch(x -> x);
    }

    @Override
    public Boolean statusChanged(Optional<Instant> start, Optional<Instant> end) {
      return false;
    }

    @Override
    public Boolean tags(Stream<String> tags) {
      return false;
    }

    @Override
    public Boolean type(String... types) {
      return false;
    }
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void testGoodBoth() {
    Assert.assertTrue(
        FilterJson.extractFromText(
                "Random stuff \nshesmu:799AEF722968FF2D5433515989dc565e5ab54AAA shesmusearch:W3sidHlwZSI6InRleHQiLCJtYXRjaENhc2UiOmZhbHNlLCJ0ZXh0IjoidGVzdCJ9XQ==  ",
                MAPPER)
            .map(
                filter ->
                    filter.convert(
                        new TestFilterBuilder() {
                          @Override
                          public Boolean ids(List<String> ids) {
                            return ids.size() == 1
                                && ids.get(0)
                                    .equals("shesmu:799AEF722968FF2D5433515989DC565E5AB54AAA");
                          }

                          @Override
                          public Boolean textSearch(Pattern pattern) {
                            return pattern.matcher("test").matches();
                          }
                        }))
            .orElse(false));
  }

  @Test
  public void testGoodID() {
    Assert.assertTrue(
        FilterJson.extractFromText(
                "Hot garbage shesmu:799AEF722968FF2D5433515989DC565E5AB54AAA  ", MAPPER)
            .map(
                filter ->
                    filter.convert(
                        new TestFilterBuilder() {
                          @Override
                          public Boolean ids(List<String> ids) {
                            return ids.size() == 1
                                && ids.get(0)
                                    .equals("shesmu:799AEF722968FF2D5433515989DC565E5AB54AAA");
                          }

                          @Override
                          public Boolean textSearch(Pattern pattern) {
                            return false;
                          }
                        }))
            .orElse(false));
  }

  @Test
  public void testGoodQuery() {
    Assert.assertTrue(
        FilterJson.extractFromText(
                "So much garbage shesmusearch:W3sidHlwZSI6InRleHQiLCJtYXRjaENhc2UiOmZhbHNlLCJ0ZXh0IjoidGVzdCJ9XQ==  ",
                MAPPER)
            .map(
                filter ->
                    filter.convert(
                        new TestFilterBuilder() {
                          @Override
                          public Boolean ids(List<String> ids) {
                            return false;
                          }

                          @Override
                          public Boolean textSearch(Pattern pattern) {
                            return pattern.matcher("test").matches();
                          }
                        }))
            .orElse(false));
  }

  @Test
  public void testNothing() {
    Assert.assertFalse(
        FilterJson.extractFromText("NOTHING OF VALUE!!! SADNESS!!! DESPAIR!!!", MAPPER)
            .isPresent());
  }
}