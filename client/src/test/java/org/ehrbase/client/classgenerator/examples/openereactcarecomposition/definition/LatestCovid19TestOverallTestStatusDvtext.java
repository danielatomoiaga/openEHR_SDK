package org.ehrbase.client.classgenerator.examples.openereactcarecomposition.definition;

import org.ehrbase.client.annotations.Entity;
import org.ehrbase.client.annotations.OptionFor;
import org.ehrbase.client.annotations.Path;

@Entity
@OptionFor("DV_TEXT")
public class LatestCovid19TestOverallTestStatusDvtext implements LatestCovid19TestOverallTestStatusChoice {
    @Path("|value")
    private String overallTestStatusValue;

    public void setOverallTestStatusValue(String overallTestStatusValue) {
        this.overallTestStatusValue = overallTestStatusValue;
    }

    public String getOverallTestStatusValue() {
        return this.overallTestStatusValue;
    }
}
