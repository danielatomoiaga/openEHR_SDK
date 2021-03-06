package org.ehrbase.client.classgenerator.examples.virologischerbefundcomposition.definition;

import org.ehrbase.client.aql.containment.Containment;
import org.ehrbase.client.aql.field.AqlFieldImp;
import org.ehrbase.client.aql.field.ListAqlFieldImp;
import org.ehrbase.client.aql.field.ListSelectAqlField;
import org.ehrbase.client.aql.field.SelectAqlField;

public class KulturClusterContainment extends Containment {
    public SelectAqlField<KulturCluster> KULTUR_CLUSTER = new AqlFieldImp<KulturCluster>(KulturCluster.class, "", "KulturCluster", KulturCluster.class, this);

    public ListSelectAqlField<ProVirusCluster> PRO_VIRUS = new ListAqlFieldImp<ProVirusCluster>(KulturCluster.class, "/items[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1 and name/value='Pro Virus']", "proVirus", ProVirusCluster.class, this);

    private KulturClusterContainment() {
        super("openEHR-EHR-CLUSTER.laboratory_test_panel.v0");
    }

    public static KulturClusterContainment getInstance() {
        return new KulturClusterContainment();
    }
}
