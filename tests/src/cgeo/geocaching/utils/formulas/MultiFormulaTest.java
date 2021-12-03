package cgeo.geocaching.utils.formulas;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class MultiFormulaTest {

    @Test
    public void coordFormatDDMMMMM() {
        final MultiFormula.MultiFormulaConfig mcConfig =
            new MultiFormula.MultiFormulaConfig("p[N|E|W|S|O]", "f° ", "p°?", "f' ", "p'?");

        assertMultiFormat(mcConfig, "N48 52.154", "N48 A2._(A+10)(A-1)", "A", 5);
        assertMultiFormat(mcConfig, "  N 48° 52.154 ", "  N 48° A2._(A+10)(A-1) ", "A", 5);
    }

    @Test
    public void coordFormatDDMMSS() {
        final MultiFormula.MultiFormulaConfig mcConfig = new MultiFormula.MultiFormulaConfig("p[N|E|W|S|O]", "f° ", "p°?", "f' ", "p'?", "f' ", "p('')?");

        assertMultiFormat(mcConfig, "N48 52 33.154", "N48 A2 33._(A+10)(A-1)", "A", 5);
        assertMultiFormat(mcConfig, "  N 48° 52' 33.154'' ", "  N 48° A2' 33._(A+10)(A-1)'' ", "A", 5);
        assertThat(MultiFormula.compile(mcConfig, "N48 A2 33._(B+10)(C-1)").getNeededVars()).containsExactlyInAnyOrder("A", "B", "C");
    }

    private void assertMultiFormat(final MultiFormula.MultiFormulaConfig mcConfig, final String expectedResult, final String expression, final Object ... vars) {
        final MultiFormula mf = MultiFormula.compile(mcConfig, expression);
        final String result = mf.evaluateToString(Formula.toVarProvider(vars));
        assertThat(result).isEqualTo(expectedResult);
    }

}
