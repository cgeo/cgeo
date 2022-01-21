package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;


import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DegreeFormulaTest {

    @Test
    public void parseSimpleDDMMMMM() {
        assertThat(DegreeFormula.compile("N48 12.345", false).evaluateToString(Formula.toVarProvider())).isEqualTo("N48°12.345'");
        assertThat(DegreeFormula.compile("N48 1A.345", false).evaluateToString(Formula.toVarProvider("A", 2))).isEqualTo("N48°12.345'");
    }

    @Test
    public void neededVars() {
        assertThat(DegreeFormula.compile("NAB CD.EFG", false).getNeededVars()).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G");
    }

    @Test
    public void parseNonparseable() {
        assertThat(DegreeFormula.compile("E 053° 33.06(4**A)'", true).evaluateToString(null)).isEqualTo("E053°33.06(4**A)'?");
    }

    @Test
    public void parseDegreeFormulas() {
        assertParse("N 053° 33.06(4*A)'", null, null, "N053°[33].<06(4 * [?A])>'");
        assertParse("90. 3.45", null, 90 + 3.045 / 60, "90.0°3.<0>45'");
        assertParse("90. 3.45 S", null, -90 - 3.045 / 60, "90.0°3.<0>45'S");
        assertParse("S53 33.6", null, -53d - 33.006 / 60, "S53°33.<00>6'");
    }

    private void assertParse(final String expression, final Func1<String, Value> varMap, final Double expectedValue, final String expectedAnnotatedString) {
        final ImmutableTriple<Double, CharSequence, Boolean> result = DegreeFormula.compile(expression, false).evaluate(varMap);
        assertThat(TextUtils.annotateSpans(result.middle, s -> {
                if (s instanceof ForegroundColorSpan) {
                    final ForegroundColorSpan fgs = (ForegroundColorSpan) s;
                    return fgs.getForegroundColor() == Color.RED ? new Pair<>("[", "]") : new Pair<>("<", ">");
                }
                return new Pair<>("{", "}");
            })).as("CS of " + expression).isEqualTo(expectedAnnotatedString);
        assertThat(result.left).as("Value of " + expression).isEqualTo(expectedValue);
    }

    @Test
    public void parseToDouble() {
        assertThat(DegreeFormula.compile("E13.456", true).evaluateToDouble(null)).isEqualTo(13.456d);
        assertThat(DegreeFormula.compile("W13.456", true).evaluateToDouble(null)).isEqualTo(-13.456d);

        assertThat(DegreeFormula.compile("N48", false).evaluateToDouble(null)).isEqualTo(48d);
        assertThat(DegreeFormula.compile("N48 12", false).evaluateToDouble(null)).isEqualTo(48d + 12d / 60);
        assertThat(DegreeFormula.compile("N48 12.345", false).evaluateToDouble(null)).isEqualTo(48d + 12.345d / 60);
        assertThat(DegreeFormula.compile("N48 12.A45", false).evaluateToDouble(x -> Value.of(3))).isEqualTo(48.20575d);
    }
}
