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
    public void parseDMM() {

        assertParse("N48 12.345", null, 48d + 12.345d / 60d, "N48°12.345'");
        assertParse("N48 1A.345", s -> Value.of(3d), 48d + 13.345d / 60d, "N48°13.345'");
        assertParse("S48 1A.345", s -> Value.of(3d), -48d - 13.345d / 60d, "S48°13.345'");

        assertParse("N 053° 33.06(3*A)'", s -> Value.of(3d), 53d + 33.069d / 60, "N053°33.069'");
        assertParse("N 053° 33.6(3*A)'", s -> Value.of(3d), 53d + 33.069d / 60, "N053°33.<0>69'");
        assertParse("N 053° 33.06(3*A)'", null, null, "N053°[33].<06(3 * [?A])>'");
        assertParse("N 053° 33.6(3*A)'", s -> Value.of(3d), 53d + 33.069d / 60, "N053°33.<0>69'");

        assertParse("N 053 33.1 2 3", null, 53d + 33.123d / 60, "N053°33.123'");
        assertParse("N 053 33. 1 2 3", null, 53d + 33.123d / 60, "N053°33.123'");
        assertParse("N 053 33. 1 2", null, 53d + 33.012d / 60, "N053°33.<0>12'");

        assertParse("53° 33. A+B+C'S", s -> Value.of(2d), -53d - 33.006d / 60, "53°33.<00>6'S");
        assertParse("53° 33. A+B+C 'S", s -> Value.of(2d), -53d - 33.006d / 60, "53°33.<00>6'S");
        assertParse("53.1° 33. A+B+C 'S", s -> Value.of(2d), null, "53.1°[33. A+B+C 'S?]");

        assertParse("NA 12.345", s -> Value.of(40d), 40d + 12.345 / 60, "N40°12.345'");
        assertParse("NA 12.345", s -> Value.of(91d), null, "N[91]°12.345'");
        assertParse("NA 12.345", s -> Value.of(40.1d), null, "N40.1°[12.345]'");
        assertParse("NA 12", s -> Value.of(40.1d), null, "N40.1°[12]'");

        assertParse("NA A/100", s -> Value.of(40d), 40d + 40d / 100 / 60, "N40°0.4'");
        assertParse("NA A/100", s -> Value.of(40.5d), null, "N40.5°[0.405]'");

        assertParse("N48° 12.1 A + 2 3", s -> Value.of(5d), 48d + (12.173d) / 60, "N48°12.173'");

    }

    @Test
    public void parseDMS() {
        assertParse("A/100\"", s -> Value.of(40d), 0.4d / 3600, "0.4\"");
        assertParse("NA A/10 A/100", s -> Value.of(40d), 40d + 4d / 60 + 0.4d / 3600, "N40°4'0.4\"");
    }

    @Test
    public void parseDegree() {
        assertParse("-A/100", s -> Value.of(40d), -0.4d, "-0.4°");
        assertParse("NA/100", s -> Value.of(-40d), null, "N[-0.4]°");
    }

    @Test
    public void parseReverseDMS() {
        assertParse("13°24'57.8\"S", null, -13d - 24d / 60 - 57.008 / 3600d, "13°24'57.<00>8\"S");
        assertParse("13°24'57.8''S", null, -13d - 24d / 60 - 57.008 / 3600d, "13°24'57.<00>8\"S");
    }

    @Test
    public void parselatLonHemi() {
        assertThat(DegreeFormula.compile("N48", false).evaluateToDouble(null)).isEqualTo(48);
        assertThat(DegreeFormula.compile("S48", false).evaluateToDouble(null)).isEqualTo(-48);
        assertThat(DegreeFormula.compile("48 N", false).evaluateToDouble(null)).isEqualTo(48);

        assertThat(DegreeFormula.compile("W48", false).evaluateToDouble(null)).isEqualTo(null);
        assertThat(DegreeFormula.compile("W48", true).evaluateToDouble(null)).isEqualTo(-48);

        assertThat(DegreeFormula.compile("W179.9", true).evaluateToDouble(null)).isEqualTo(-179.9);
        assertThat(DegreeFormula.compile("W180", true).evaluateToDouble(null)).isEqualTo(-180);
        assertThat(DegreeFormula.compile("W180.1", true).evaluateToDouble(null)).isEqualTo(null);

        assertThat(DegreeFormula.compile("N89.9", false).evaluateToDouble(null)).isEqualTo(89.9);
        assertThat(DegreeFormula.compile("N90", false).evaluateToDouble(null)).isEqualTo(90);
        assertThat(DegreeFormula.compile("N90.1", false).evaluateToDouble(null)).isEqualTo(null);

        assertParse("48 S", null, -48d, "48°S");
        assertParse("S-48", null, null, "S[-48]°");
        assertParse("-48 S", null, null, "-48°[S]");
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
    public void parseVariousFormulas() {
        assertParse("N 053° 33.06(4*A)'", null, null, "N053°[33].<06(4 * [?A])>'");
        assertParse("90 3.45", null, 90 + 3.045 / 60, "90°3.<0>45'");
        assertParse("88. 3.45", null, null, "[88. 3.45?]");
        assertParse("90. 3.45", null, null, "[90. 3.45?]");
        assertParse("89. 345", null, 89.345, "89.345°");
        assertParse("90. 345", null, null, "[90.345]°");
        assertParse("S90 3.45", null, -90 - 3.045 / 60, "S90°3.<0>45'");
        assertParse("90 3.45'S", null, -90 - 3.045 / 60, "90°3.<0>45'S");
        assertParse("S53 33.6", null, -53d - 33.006 / 60, "S53°33.<00>6'");
    }

    @Test
    public void parseFormulaWithWhitespaces() {
        assertParse("N\f46°\u00A0(C+E+F+1).\n0\t(C+F-E)", s -> Value.of(0), 46.0166666666666666666d, "N46°1.<0>00'");
    }


    //this is a test to copy/paste single tests into for local analysis e.g. using Debugging
    @Test
    public void parseSingleForDebug() {
        assertParse("88. 3.45", null, null, "[88. 3.45?]");
    }

    private void assertParse(final String expression, final Func1<String, Value> varMap, final Double expectedValue, final String expectedAnnotatedString) {
        final ImmutableTriple<Double, CharSequence, Boolean> result = DegreeFormula.compile(expression, false).evaluate(varMap);
        final Double resultValue = result.left;
        final String annotatedCss = TextUtils.annotateSpans(result.middle, s -> {
            if (s instanceof ForegroundColorSpan) {
                final ForegroundColorSpan fgs = (ForegroundColorSpan) s;
                return fgs.getForegroundColor() == Color.RED ? new Pair<>("[", "]") : new Pair<>("<", ">");
            }
            return new Pair<>("{", "}");
        }).replaceAll("]\\[", "").replaceAll("><", "");
        final String asString = " (Exp:" + expression + ", result:" + resultValue + ", annotatedCss:" + annotatedCss + ")";
        if (expectedValue == null) {
            assertThat(resultValue).as("Value not null" + asString).isNull();
        } else {
            assertThat(resultValue).as("Value wrong" + asString).isEqualTo(expectedValue);
        }
        assertThat(annotatedCss).as("CSS wrong" + asString).isEqualTo(expectedAnnotatedString);
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

    @Test
    public void removeSpaces() {
        //formula includes all sorts of whitespace
        assertThat(DegreeFormula.removeSpaces("E10° 0 ( c\t ). (\ne*3\r+ d )"))
                .isEqualTo("E10°0(c).(e*3+d)");
    }
}
