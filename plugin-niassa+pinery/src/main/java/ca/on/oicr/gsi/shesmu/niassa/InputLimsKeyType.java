package ca.on.oicr.gsi.shesmu.niassa;

import ca.on.oicr.gsi.shesmu.plugin.action.CustomActionParameter;
import ca.on.oicr.gsi.shesmu.plugin.types.TypeGuarantee;
import java.util.function.Function;

/**
 * Definitions of all the types of ways to create LIMS keys associated with a run.
 *
 * <p>These do not map exactly to Niassa's concept of a workflow. All this code cares about is
 * whether the types of the parameters in the INI are the same. Any workflows that take the same
 * parameters can share on entry here.
 */
public enum InputLimsKeyType {
  BAM_MERGE(
      "inputs",
      BamMergeInputLimsCollection::new,
      TypeGuarantee.list(
          TypeGuarantee.tuple(
              BamMergeGroup::new,
              TypeGuarantee.STRING, // Output name
              TypeGuarantee.list(
                  TypeGuarantee.tuple(
                      BamMergeEntry::new,
                      TypeGuarantee.STRING, // SWID
                      TypeGuarantee.STRING, // File name
                      TypeGuarantee.object(
                          SimpleLimsKey::new,
                          "id",
                          TypeGuarantee.STRING,
                          "provider",
                          TypeGuarantee.STRING,
                          "time",
                          TypeGuarantee.DATE,
                          "version",
                          TypeGuarantee.STRING), // LIMS key
                      TypeGuarantee.BOOLEAN // staleness
                      ))))),
  BCL2FASTQ(
      "lanes",
      Bcl2FastqInputLimsCollection::new,
      TypeGuarantee.list(
          TypeGuarantee.tuple(
              Bcl2FastqLaneEntry::new,
              TypeGuarantee.LONG, // lane number
              TypeGuarantee.object(
                  SimpleLimsKey::new,
                  "id",
                  TypeGuarantee.STRING,
                  "provider",
                  TypeGuarantee.STRING,
                  "time",
                  TypeGuarantee.DATE,
                  "version",
                  TypeGuarantee.STRING), // lane LIMS key
              TypeGuarantee.list(
                  TypeGuarantee.tuple(
                      Bcl2FastqSampleEntry::new,
                      TypeGuarantee.STRING, // sample barcode
                      TypeGuarantee.STRING, // sample library name
                      TypeGuarantee.object(
                          SimpleLimsKey::new,
                          "id",
                          TypeGuarantee.STRING,
                          "provider",
                          TypeGuarantee.STRING,
                          "time",
                          TypeGuarantee.DATE,
                          "version",
                          TypeGuarantee.STRING), // sample LIMS key
                      TypeGuarantee.STRING) // sample group id
                  )))),
  CELL_RANGER(
      "lanes",
      CellRangerInputLimsCollection::new,
      TypeGuarantee.list(
          TypeGuarantee.tuple(
              CellRangerIUSEntry::new,
              TypeGuarantee.tuple(
                  IusTriple::new,
                  TypeGuarantee.STRING,
                  TypeGuarantee.LONG,
                  TypeGuarantee.STRING), // IUS
              TypeGuarantee.STRING, // library name
              TypeGuarantee.object(
                  SimpleLimsKey::new,
                  "id",
                  TypeGuarantee.STRING,
                  "provider",
                  TypeGuarantee.STRING,
                  "time",
                  TypeGuarantee.DATE,
                  "version",
                  TypeGuarantee.STRING), // LIMS key
              TypeGuarantee.STRING) // group id
          )),

  FILES(
      "inputs",
      FilesInputLimsCollection::new,
      TypeGuarantee.list(
          TypeGuarantee.tuple(
              FilesInputFile::new,
              TypeGuarantee.STRING, // SWID
              TypeGuarantee.object(
                  SimpleLimsKey::new,
                  "id",
                  TypeGuarantee.STRING,
                  "provider",
                  TypeGuarantee.STRING,
                  "time",
                  TypeGuarantee.DATE,
                  "version",
                  TypeGuarantee.STRING), // LIMS key
              TypeGuarantee.BOOLEAN))); // staleness

  private final CustomActionParameter<WorkflowAction> parameter;

  <T> InputLimsKeyType(
      String name, Function<T, InputLimsCollection> create, TypeGuarantee<T> type) {
    this.parameter =
        new CustomActionParameter<WorkflowAction>(name, true, type.type()) {

          @Override
          public void store(WorkflowAction action, Object value) {
            action.limsKeyCollection(create.apply(type.unpack(value)));
          }
        };
  }

  public final CustomActionParameter<WorkflowAction> parameter() {
    return parameter;
  }
}
