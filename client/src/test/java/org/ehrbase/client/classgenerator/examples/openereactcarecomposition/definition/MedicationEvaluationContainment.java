package org.ehrbase.client.classgenerator.examples.openereactcarecomposition.definition;

import com.nedap.archie.rm.datastructures.Cluster;
import com.nedap.archie.rm.generic.PartyProxy;
import org.ehrbase.client.aql.containment.Containment;
import org.ehrbase.client.aql.field.AqlFieldImp;
import org.ehrbase.client.aql.field.ListAqlFieldImp;
import org.ehrbase.client.aql.field.ListSelectAqlField;
import org.ehrbase.client.aql.field.SelectAqlField;
import org.ehrbase.client.classgenerator.examples.shareddefinition.Language;

public class MedicationEvaluationContainment extends Containment {
  public SelectAqlField<MedicationEvaluation> MEDICATION_EVALUATION = new AqlFieldImp<MedicationEvaluation>(MedicationEvaluation.class, "", "MedicationEvaluation", MedicationEvaluation.class, this);

  public SelectAqlField<PartyProxy> SUBJECT = new AqlFieldImp<PartyProxy>(MedicationEvaluation.class, "/subject", "subject", PartyProxy.class, this);

  public SelectAqlField<String> SYNOPSIS_VALUE = new AqlFieldImp<String>(MedicationEvaluation.class, "/data[at0001]/items[at0002]/value|value", "synopsisValue", String.class, this);

  public SelectAqlField<Language> LANGUAGE = new AqlFieldImp<Language>(MedicationEvaluation.class, "/language", "language", Language.class, this);

  public ListSelectAqlField<Cluster> EXTENSION = new ListAqlFieldImp<Cluster>(MedicationEvaluation.class, "/protocol[at0003]/items[at0004]", "extension", Cluster.class, this);

  private MedicationEvaluationContainment() {
    super("openEHR-EHR-EVALUATION.clinical_synopsis.v1");
  }

  public static MedicationEvaluationContainment getInstance() {
    return new MedicationEvaluationContainment();
  }
}
