package ca.on.oicr.gsi.shesmu.niassa;

import ca.on.oicr.gsi.provenance.model.LimsKey;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

public class Bcl2FastqSampleEntry implements LimsKey {
  private final String barcode;
  private final String groupId;
  private final String limsId;
  private final String limsProvider;
  private final ZonedDateTime limsTimestamp;
  private final String limsVersion;
  private final String libraryName;

  public Bcl2FastqSampleEntry(String barcode, String libraryName, LimsKey limsKey, String groupId) {
    this.barcode = barcode;
    this.libraryName = libraryName;
    limsId = limsKey.getId();
    limsProvider = limsKey.getProvider();
    limsVersion = limsKey.getVersion();
    limsTimestamp = limsKey.getLastModified();
    this.groupId = groupId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Bcl2FastqSampleEntry that = (Bcl2FastqSampleEntry) o;
    return barcode.equals(that.barcode)
        && groupId.equals(that.groupId)
        && limsId.equals(that.limsId)
        && limsProvider.equals(that.limsProvider)
        && limsTimestamp.equals(that.limsTimestamp)
        && limsVersion.equals(that.limsVersion)
        && libraryName.equals(that.libraryName);
  }

  @Override
  public String getId() {
    return limsId;
  }

  @Override
  public ZonedDateTime getLastModified() {
    return limsTimestamp;
  }

  @Override
  public String getProvider() {
    return limsProvider;
  }

  @Override
  public String getVersion() {
    return limsVersion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        barcode, groupId, limsId, limsProvider, limsTimestamp, limsVersion, libraryName);
  }

  public boolean matches(Pattern query) {
    return query.matcher(limsId).matches()
        || query.matcher(limsProvider).matches()
        || query.matcher(limsVersion).matches()
        || query.matcher(libraryName).matches()
        || query.matcher(groupId).matches()
        || query.matcher(barcode).matches();
  }

  public String prepare(ToIntFunction<LimsKey> createIusLimsKey) {
    return String.format(
        "%s,%d,%s,%s", barcode, createIusLimsKey.applyAsInt(this), libraryName, groupId);
  }
}