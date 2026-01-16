package cgeo.geocaching.utils.formulas;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

import org.junit.Test;

public class SumFunctionTest {

    @Test
    public void testSumNumericRange() {
        // sum(1;5) -> 1+2+3+4+5 = 15
        final Value result = Formula.evaluate("sum(1;5)");
        assertThat(result.getAsLong()).isEqualTo(15);
    }

    @Test
    public void testSumNumericRangeLarger() {
        // sum(1;10) -> 1+2+3+...+10 = 55
        final Value result = Formula.evaluate("sum(1;10)");
        assertThat(result.getAsLong()).isEqualTo(55);
    }

    @Test
    public void testSumNumericRangeSingleValue() {
        // sum(5;5) -> 5
        final Value result = Formula.evaluate("sum(5;5)");
        assertThat(result.getAsLong()).isEqualTo(5);
    }

    @Test
    public void testSumSingleLetterVariableRange() {
        // sum("A";"D") with A=1, B=2, C=3, D=4 -> 10
        final Value result = Formula.evaluate("sum('A';'D')", "A", 1, "B", 2, "C", 3, "D", 4);
        assertThat(result.getAsLong()).isEqualTo(10);
    }

    @Test
    public void testSumSingleLetterVariableRangeWithDollar() {
        // sum("$A";"$D") with A=1, B=2, C=3, D=4 -> 10
        final Value result = Formula.evaluate("sum('$A';'$D')", "A", 1, "B", 2, "C", 3, "D", 4);
        assertThat(result.getAsLong()).isEqualTo(10);
    }

    @Test
    public void testSumTwoLetterVariableRangeNumericSuffix() {
        // sum("$A1";"$A3") with A1=5, A2=10, A3=15 -> 30
        final Value result = Formula.evaluate("sum('$A1';'$A3')", "A1", 5, "A2", 10, "A3", 15);
        assertThat(result.getAsLong()).isEqualTo(30);
    }

    @Test
    public void testSumTwoLetterVariableRangeLetterSuffix() {
        // sum("$NA";"$NC") with NA=1, NB=2, NC=3 -> 6
        final Value result = Formula.evaluate("sum('$NA';'$NC')", "NA", 1, "NB", 2, "NC", 3);
        assertThat(result.getAsLong()).isEqualTo(6);
    }

    @Test
    public void testSumPreCalculatedVariable() {
        // sum(A;5) with A=3 -> 3+4+5 = 12
        final Value result = Formula.evaluate("sum(A;5)", "A", 3);
        assertThat(result.getAsLong()).isEqualTo(12);
    }

    @Test
    public void testSumPreCalculatedVariableWithDollar() {
        // sum($A;5) with A=3 -> 3+4+5 = 12
        final Value result = Formula.evaluate("sum($A;5)", "A", 3);
        assertThat(result.getAsLong()).isEqualTo(12);
    }

    @Test
    public void testSumErrorMixedTypes() {
        // sum("A";5) -> error: mixed types
        assertThatThrownBy(() -> Formula.evaluate("sum('A';5)", "A", 1))
            .isInstanceOf(FormulaException.class)
            .hasMessageContaining("same type");
    }

    @Test
    public void testSumErrorPrefixMismatchNumeric() {
        // sum("$A1";"$B1") -> error: prefix must match
        assertThatThrownBy(() -> Formula.compile("sum('$A1';'$B1')"))
            .isInstanceOf(FormulaException.class)
            .hasMessageContaining("prefix");
    }

    @Test
    public void testSumErrorPrefixMismatchLetter() {
        // sum("A1";"B1") -> error: prefix must match
        assertThatThrownBy(() -> Formula.compile("sum('A1';'B1')"))
            .isInstanceOf(FormulaException.class)
            .hasMessageContaining("prefix");
    }

    @Test
    public void testSumErrorMissingVariables() {
        // sum("A";"D") with A=1, B=2, C and D not defined -> error
        assertThatThrownBy(() -> Formula.evaluate("sum('A';'D')", "A", 1, "B", 2))
            .isInstanceOf(FormulaException.class)
            .hasMessageContaining("C")
            .hasMessageContaining("D");
    }

    @Test
    public void testSumErrorStartGreaterThanEnd() {
        // sum(5;1) -> error: start > end
        assertThatThrownBy(() -> Formula.evaluate("sum(5;1)"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testSumErrorStartVariableGreaterThanEnd() {
        // sum("D";"A") -> error: start > end
        assertThatThrownBy(() -> Formula.compile("sum('D';'A')"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testSumDependencyTracking() {
        // Verify that sum("A";"D") declares all variables A, B, C, D as dependencies
        final Formula formula = Formula.compile("sum('A';'D')");
        assertThat(formula.getNeededVariables()).containsExactlyInAnyOrder("A", "B", "C", "D");
    }

    @Test
    public void testSumDependencyTrackingNumericSuffix() {
        // Verify that sum("$A1";"$A3") declares A1, A2, A3 as dependencies
        final Formula formula = Formula.compile("sum('$A1';'$A3')");
        assertThat(formula.getNeededVariables()).containsExactlyInAnyOrder("A1", "A2", "A3");
    }

    @Test
    public void testSumDependencyTrackingLetterSuffix() {
        // Verify that sum("$NA";"$NC") declares NA, NB, NC as dependencies
        final Formula formula = Formula.compile("sum('$NA';'$NC')");
        assertThat(formula.getNeededVariables()).containsExactlyInAnyOrder("NA", "NB", "NC");
    }

    @Test
    public void testSumWithDecimalVariables() {
        // sum("A";"C") with A=1.5, B=2.5, C=3.0 -> 7.0
        final Value result = Formula.evaluate("sum('A';'C')", "A", 1.5, "B", 2.5, "C", 3.0);
        assertThat(result.getAsDouble()).isEqualTo(7.0);
    }

    @Test
    public void testSumErrorNonNumericVariable() {
        // sum("A";"C") with A=1, B="text", C=3 -> error
        assertThatThrownBy(() -> Formula.evaluate("sum('A';'C')", "A", 1, "B", "text", "C", 3))
            .isInstanceOf(FormulaException.class)
            .hasMessageContaining("not numeric");
    }

    @Test
    public void testSumNumericRangeNotInteger() {
        // sum(1.5;5) -> error: must be integer
        assertThatThrownBy(() -> Formula.evaluate("sum(1.5;5)"))
            .isInstanceOf(FormulaException.class)
            .hasMessageContaining("Integer");
    }

    @Test
    public void testSumLowercaseVariables() {
        // sum("a";"c") with a=1, b=2, c=3 -> 6
        final Value result = Formula.evaluate("sum('a';'c')", "A", 1, "B", 2, "C", 3);
        assertThat(result.getAsLong()).isEqualTo(6);
    }

    @Test
    public void testSumInFormula() {
        // Test sum within a larger formula: sum(1;3) + 10 = 16
        final Value result = Formula.evaluate("sum(1;3) + 10");
        assertThat(result.getAsLong()).isEqualTo(16);
    }

    @Test
    public void testSumVariableRangeInFormula() {
        // Test sum with variables in larger formula: sum("A";"B") * 2
        final Value result = Formula.evaluate("sum('A';'B') * 2", "A", 3, "B", 4);
        assertThat(result.getAsLong()).isEqualTo(14);
    }
}
