package org.ehrbase.client.classgenerator.examples.smicsbefundcomposition.definition;

import org.ehrbase.client.aql.containment.Containment;
import org.ehrbase.client.aql.field.AqlFieldImp;
import org.ehrbase.client.aql.field.ListAqlFieldImp;
import org.ehrbase.client.aql.field.ListSelectAqlField;
import org.ehrbase.client.aql.field.SelectAqlField;

public class EventsummaryClusterContainment extends Containment {
  public SelectAqlField<EventsummaryCluster> EVENTSUMMARY_CLUSTER = new AqlFieldImp<EventsummaryCluster>(EventsummaryCluster.class, "", "EventsummaryCluster", EventsummaryCluster.class, this);

  public SelectAqlField<String> KOMMENTAR_VALUE = new AqlFieldImp<String>(EventsummaryCluster.class, "/items[at0006]/value|value", "kommentarValue", String.class, this);

  public SelectAqlField<String> FALL_KATEGORIE_VALUE = new AqlFieldImp<String>(EventsummaryCluster.class, "/items[at0004]/value|value", "fallKategorieValue", String.class, this);

  public SelectAqlField<String> FALLIDENTIFIKATION_VALUE = new AqlFieldImp<String>(EventsummaryCluster.class, "/items[at0001]/value|value", "fallidentifikationValue", String.class, this);

  public SelectAqlField<String> FALL_ART_VALUE = new AqlFieldImp<String>(EventsummaryCluster.class, "/items[at0002]/value|value", "fallArtValue", String.class, this);

  public ListSelectAqlField<ContextEventsummaryBeteiligtePersonenCluster> BETEILIGTE_PERSONEN = new ListAqlFieldImp<ContextEventsummaryBeteiligtePersonenCluster>(EventsummaryCluster.class, "/items[at0007]", "beteiligtePersonen", ContextEventsummaryBeteiligtePersonenCluster.class, this);

  private EventsummaryClusterContainment() {
    super("openEHR-EHR-CLUSTER.eventsummary.v0");
  }

  public static EventsummaryClusterContainment getInstance() {
    return new EventsummaryClusterContainment();
  }
}
