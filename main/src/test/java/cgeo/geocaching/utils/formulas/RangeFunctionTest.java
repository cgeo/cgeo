package cgeo.geocaching.utils.formulas;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class RangeFunctionTest {

    @Test
    public void rangeFunctions() {
        assertThat(Formula.evaluate("add('0-5')").getAsLong()).isEqualTo(1 + 2 + 3 + 4 + 5);
        assertThat(Formula.evaluate("multiply('1-5')").getAsLong()).isEqualTo(2 * 3 * 4 * 5);
        assertThat(Formula.evaluate("min(-99;1;3;99)").getAsLong()).isEqualTo(-99);
        assertThat(Formula.evaluate("max(-99;1;3;99)").getAsLong()).isEqualTo(99);
        assertThat(Formula.evaluate("count(10;'1-5')").getAsLong()).isEqualTo(6);
        assertThat(Formula.evaluate("avg(10;'1-4')").getAsDouble()).isEqualTo((10 + 1 + 2 + 3 + 4) / 5d);
    }

    @Test
    public void testAddSingleValue() {
        assertThat(Formula.evaluate("add(42)").getAsLong()).isEqualTo(42);
        assertThat(Formula.evaluate("add(10.5)").getAsDouble()).isEqualTo(10.5);
        assertThat(Formula.evaluate("add(A)", "A", 10).getAsLong()).isEqualTo(10);
    }

    @Test
    public void testAddMultipleValues() {
        assertThat(Formula.evaluate("add(1; 5; 10)").getAsLong()).isEqualTo(1 + 5 + 10);
        assertThat(Formula.evaluate("add(1.5; 2.5; 3.5)").getAsDouble()).isEqualTo(1.5 + 2.5 + 3.5);
        assertThat(Formula.evaluate("add(A; B; C)", "A", 1, "B", 2, "C", 3).getAsLong()).isEqualTo(1 + 2 + 3);
    }

    @Test
    public void testAddDecimalValue() {
        assertThat(Formula.evaluate("add(10.5)").getAsDouble()).isEqualTo(10.5);
        assertThat(Formula.evaluate("add(1.5; 2.5; 3.5)").getAsDouble()).isEqualTo(1.5 + 2.5 + 3.5);
    }

    @Test
    public void testAddRanges() {
        assertThat(Formula.evaluate("add('1-5')").getAsLong()).isEqualTo(1 + 2 + 3 + 4 + 5);
        assertThat(Formula.evaluate("add('10-100')").getAsLong()).isEqualTo(5005);
        assertThat(Formula.evaluate("add('5-5')").getAsLong()).isEqualTo(5);
        assertThat(Formula.evaluate("add('A-D')", "A", 1, "B", 2, "C", 3, "D", 4).getAsLong()).isEqualTo(1 + 2 + 3 + 4);
        assertThat(Formula.evaluate("add('$A-$D')", "A", 1, "B", 2, "C", 3, "D", 4).getAsLong()).isEqualTo(1 + 2 + 3 + 4);
        assertThat(Formula.evaluate("add('$A1-$A3')", "A1", 5, "A2", 10, "A3", 15).getAsLong()).isEqualTo(5 + 10 + 15);
        assertThat(Formula.evaluate("add('$NA-$NC')", "NA", 1, "NB", 2, "NC", 3).getAsLong()).isEqualTo(1 + 2 + 3);
        assertThat(Formula.evaluate("add('A-D'; 'C-E')", "A", 1, "B", 2, "C", 3, "D", 4, "E", 5).getAsLong()).isEqualTo((1 + 2 + 3 + 4) + (3 + 4 + 5));
        assertThat(Formula.evaluate("add('a-d')", "a", 1, "b", 2, "c", 3, "d", 4).getAsLong()).isEqualTo(1 + 2 + 3 + 4);
        assertThat(Formula.evaluate("add('A-C')", "A", 1.5, "B", 2.5, "C", 3.0).getAsDouble()).isEqualTo(1.5 + 2.5 + 3);
    }

    @Test
    public void testAddNegativeRanges() {
        // Test -3-3 format (without parentheses)
        assertThat(Formula.evaluate("add('-3-3')").getAsLong()).isEqualTo(-3 - 2 - 1 + 1 + 2 + 3);
        assertThat(Formula.evaluate("add('(-3)-(-1)')").getAsLong()).isEqualTo(-3 - 2 - 1);
        assertThat(Formula.evaluate("add('(-5)-(-2)')").getAsLong()).isEqualTo(-5 - 4 - 3 - 2);
        assertThat(Formula.evaluate("add('(-3)-3')").getAsLong()).isEqualTo(-3 - 2 - 1 + 1 + 2 + 3);
        assertThat(Formula.evaluate("add('-5-(-2)')").getAsLong()).isEqualTo(-5 - 4 - 3 - 2);
        assertThat(Formula.evaluate("add('-2-3')").getAsLong()).isEqualTo(-2 - 1 + 1 + 2 + 3);
        assertThat(Formula.evaluate("add('-10-(-5)')").getAsLong()).isEqualTo(-10 - 9 - 8 - 7 - 6 - 5);
    }

    @Test
    public void testAddMixedValues() {
        assertThat(Formula.evaluate("add(1; 2.5; 3; 4.5)").getAsDouble()).isEqualTo(1 + 2.5 + 3 + 4.5);
        assertThat(Formula.evaluate("add('1-3'; 10.5; 4; 20.5; 'A-B')", "A", 5, "B", 2).getAsDouble())
                .isEqualTo((1 + 2 + 3) + 10.5 + 20.5 + 4 + (5 + 2));
        assertThat(Formula.evaluate("add('1-3'; 'A-B'; '10-12'; 'X-Y')", "A", 5, "B", 6, "X", 20, "Y", 21).getAsLong())
                .isEqualTo((1 + 2 + 3) + (5 + 6) + (10 + 11 + 12) + (20 + 21));

    }

    @Test
    public void testAddErrorMixedTypes() {
        assertThatThrownBy(() -> Formula.evaluate("add('A-5')", "A", 1))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testAddErrorStartGreaterThanEnd() {
        assertThatThrownBy(() -> Formula.evaluate("add('5-1')"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> Formula.evaluate("add('Z-A')"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testAddErrorNonNumericVariable() {
        assertThatThrownBy(() -> Formula.evaluate("add('A-C')", "A", 1, "B", "text", "C", 3))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("WRONG_TYPE");
    }

    @Test
    public void testAddNumericRangeNotInteger() {
        assertThatThrownBy(() -> Formula.evaluate("add('1.5-5')"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testAddErrorMissingVariables() {
        assertThatThrownBy(() -> Formula.evaluate("add('A-D')", "A", 1, "B", 2))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("Missing")
                .hasMessageContaining("C")
                .hasMessageContaining("D");

        assertThatThrownBy(() -> Formula.evaluate("add('$NA-$ND')", "NA", 1, "NB", 2))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("Missing")
                .hasMessageContaining("NC")
                .hasMessageContaining("ND");
    }

    @Test
    public void testAddDependencyTracking() {
        assertThat(Formula.compile("add(A; B; C)").getNeededVariables()).containsExactlyInAnyOrder("A", "B", "C");
        assertThat(Formula.compile("add('A-D')").getNeededVariables()).containsExactlyInAnyOrder("A", "B", "C", "D");
        assertThat(Formula.compile("add('$A1-$A3')").getNeededVariables()).containsExactlyInAnyOrder("A1", "A2", "A3");
        assertThat(Formula.compile("add('$NA-$NC')").getNeededVariables()).containsExactlyInAnyOrder("NA", "NB", "NC");
        assertThat(Formula.compile("add('A-C'; 'X-Z')").getNeededVariables()).containsExactlyInAnyOrder("A", "B", "C", "X", "Y", "Z");
        assertThat(Formula.compile("add('A-C'; '1-5'; 'X-Z')").getNeededVariables()).containsExactlyInAnyOrder("A", "B", "C", "X", "Y", "Z");
    }

    @Test
    public void testAddInFormula() {
        assertThat(Formula.evaluate("add('1-3') + 10").getAsLong()).isEqualTo((1 + 2 + 3) + 10);
        assertThat(Formula.evaluate("add('A-B') * 2", "A", 3, "B", 4).getAsLong()).isEqualTo((3 + 4) * 2);
    }

    @Test
    public void testAddForgottenApostrophe() {
        assertThat(Formula.evaluate("add(1-3)").getAsInteger()).isEqualTo(1 - 3);
        assertThat(Formula.evaluate("add(A-B)", "A", 5, "B", 2).getAsInteger()).isEqualTo(5 - 2);
    }
}
