package org.ehrbase.client.classgenerator.examples.openereactcarecomposition.definition;

import org.ehrbase.client.annotations.Entity;
import org.ehrbase.client.annotations.OptionFor;
import org.ehrbase.client.annotations.Path;

@Entity
@OptionFor("DV_CODED_TEXT")
public class DenwisDvcodedtext2 implements DenwisChoice2 {
    @Path("|defining_code")
    private ValueDefiningcode3 valueDefiningcode;

    public void setValueDefiningcode(ValueDefiningcode3 valueDefiningcode) {
        this.valueDefiningcode = valueDefiningcode;
    }

    public ValueDefiningcode3 getValueDefiningcode() {
        return this.valueDefiningcode;
    }
}
