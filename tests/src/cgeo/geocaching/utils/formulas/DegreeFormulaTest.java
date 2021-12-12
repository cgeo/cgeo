package cgeo.geocaching.utils.formulas;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DegreeFormulaTest {

    @Test
    public void parseSimpleDDMMMMM() {
        assertThat(DegreeFormula.compile("N48 12.345").evaluateToString(Formula.toVarProvider())).isEqualTo("N48°12.345'");
        assertThat(DegreeFormula.compile("N48 1A.345").evaluateToString(Formula.toVarProvider("A", 2))).isEqualTo("N48°12.345'");
    }

    @Test
    public void neededVars() {
        assertThat(DegreeFormula.compile("NAB CD.EFG").getNeededVars()).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G");
    }

    @Test
    public void parseToString() {
        assertThat(DegreeFormula.compile("E 053° 33.06(4**A)'").evaluateToString(null)).isEqualTo("E053°33.06(4**A)'");
    }
}
