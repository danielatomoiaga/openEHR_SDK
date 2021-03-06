package org.ehrbase.client.classgenerator.examples.openereactcarecomposition.definition;

import org.ehrbase.client.annotations.Entity;
import org.ehrbase.client.annotations.Path;

@Entity
public class DenwisBreathingIndicatorElement {
    @Path("/value|defining_code")
    private Definingcode7 definingcode;

    public void setDefiningcode(Definingcode7 definingcode) {
        this.definingcode = definingcode;
    }

    public Definingcode7 getDefiningcode() {
        return this.definingcode;
    }
}
