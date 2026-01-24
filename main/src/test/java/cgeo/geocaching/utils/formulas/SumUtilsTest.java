package cgeo.geocaching.utils.formulas;

import android.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class SumUtilsTest {

    // ========== expandVariableRange() tests ==========

    @Test
    public void testExpandSingleLetterRangeUppercase() {
        final List<String> result = SumUtils.expandVariableRange("A", "D");
        assertThat(result).isEqualTo(Arrays.asList("A", "B", "C", "D"));
    }

    @Test
    public void testExpandSingleLetterRangeLowercase() {
        final List<String> result = SumUtils.expandVariableRange("a", "d");
        assertThat(result).isEqualTo(Arrays.asList("a", "b", "c", "d"));
    }

    @Test
    public void testExpandSingleLetterRangeWithDollar() {
        final List<String> result = SumUtils.expandVariableRange("$A", "$D");
        assertThat(result).isEqualTo(Arrays.asList("A", "B", "C", "D"));
    }

    @Test
    public void testExpandSingleLetterRangeSingleVariable() {
        final List<String> result = SumUtils.expandVariableRange("X", "X");
        assertThat(result).isEqualTo(Arrays.asList("X"));
    }

    @Test
    public void testExpandNumericSuffixRange() {
        final List<String> result = SumUtils.expandVariableRange("$A1", "$A5");
        assertThat(result).isEqualTo(Arrays.asList("A1", "A2", "A3", "A4", "A5"));
    }

    @Test
    public void testExpandNumericSuffixRangeMultipleDigits() {
        final List<String> result = SumUtils.expandVariableRange("$X10", "$X12");
        assertThat(result).isEqualTo(Arrays.asList("X10", "X11", "X12"));
    }

    @Test
    public void testExpandLetterSuffixRange() {
        final List<String> result = SumUtils.expandVariableRange("$NA", "$ND");
        assertThat(result).isEqualTo(Arrays.asList("NA", "NB", "NC", "ND"));
    }

    @Test
    public void testExpandLetterSuffixRangeLowercase() {
        final List<String> result = SumUtils.expandVariableRange("$na", "$nd");
        assertThat(result).isEqualTo(Arrays.asList("na", "nb", "nc", "nd"));
    }

    @Test
    public void testExpandLetterSuffixRangeLongerPrefix() {
        final List<String> result = SumUtils.expandVariableRange("$VARA", "$VARC");
        assertThat(result).isEqualTo(Arrays.asList("VARA", "VARB", "VARC"));
    }

    @Test
    public void testExpandMixedCase() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("a", "D"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandReverseOrder() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("D", "A"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandNoDollarForMultiChar() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("A1", "A5"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandOnlyDollar() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$", "$A"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandPrefixMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A1", "$B1"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandPrefixCaseMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A1", "$a5"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandLetterSuffixPrefixMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$NA", "$MB"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandLetterSuffixCaseMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$NA", "$Nc"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandInvalidMixedTypes() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A1", "$AB"))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testExpandNumericRangeReversed() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A5", "$A1"))
            .isInstanceOf(FormulaException.class);
    }

    // ========== sumVariables() tests ==========

    @Test
    public void testSumVariablesBasic() {
        final List<String> vars = Arrays.asList("A", "B", "C");
        final Function<String, Value> provider = varName -> {
            switch (varName) {
                case "A": return Value.of(1);
                case "B": return Value.of(2);
                case "C": return Value.of(3);
                default: return null;
            }
        };

        final Pair<BigDecimal, List<String>> result = SumUtils.sumVariables(vars, provider);
        assertThat(result.first).isEqualTo(new BigDecimal("6"));
        assertThat(result.second).isEmpty();
    }

    @Test
    public void testSumVariablesWithDecimals() {
        final List<String> vars = Arrays.asList("X", "Y");
        final Function<String, Value> provider = varName -> {
            switch (varName) {
                case "X": return Value.of(1.5);
                case "Y": return Value.of(2.5);
                default: return null;
            }
        };

        final Pair<BigDecimal, List<String>> result = SumUtils.sumVariables(vars, provider);
        assertThat(result.first).isEqualTo(new BigDecimal("4.0"));
        assertThat(result.second).isEmpty();
    }

    @Test
    public void testSumVariablesWithMissing() {
        final List<String> vars = Arrays.asList("A", "B", "C", "D");
        final Function<String, Value> provider = varName -> {
            switch (varName) {
                case "A": return Value.of(1);
                case "B": return null;
                case "C": return Value.of(3);
                case "D": return null;
                default: return null;
            }
        };

        final Pair<BigDecimal, List<String>> result = SumUtils.sumVariables(vars, provider);
        assertThat(result.first).isEqualTo(new BigDecimal("4"));
        assertThat(result.second).isEqualTo(Arrays.asList("B", "D"));
    }

    @Test
    public void testSumVariablesEmpty() {
        final List<String> vars = new ArrayList<>();
        final Function<String, Value> provider = varName -> Value.of(1);

        final Pair<BigDecimal, List<String>> result = SumUtils.sumVariables(vars, provider);
        assertThat(result.first).isEqualTo(BigDecimal.ZERO);
        assertThat(result.second).isEmpty();
    }

    @Test
    public void testSumVariablesAllMissing() {
        final List<String> vars = Arrays.asList("A", "B", "C");
        final Function<String, Value> provider = varName -> null;

        final Pair<BigDecimal, List<String>> result = SumUtils.sumVariables(vars, provider);
        assertThat(result.first).isEqualTo(BigDecimal.ZERO);
        assertThat(result.second).isEqualTo(Arrays.asList("A", "B", "C"));
    }

    @Test
    public void testSumVariablesNonNumeric() {
        final List<String> vars = Arrays.asList("A", "B");
        final Function<String, Value> provider = varName -> {
            switch (varName) {
                case "A": return Value.of(1);
                case "B": return Value.of("text");
                default: return null;
            }
        };

        assertThatThrownBy(() -> SumUtils.sumVariables(vars, provider))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testSumVariablesLargeNumbers() {
        final List<String> vars = Arrays.asList("A", "B");
        final Function<String, Value> provider = varName -> {
            switch (varName) {
                case "A": return Value.of(999999999);
                case "B": return Value.of(888888888);
                default: return null;
            }
        };

        final Pair<BigDecimal, List<String>> result = SumUtils.sumVariables(vars, provider);
        assertThat(result.first).isEqualTo(new BigDecimal("1888888887"));
        assertThat(result.second).isEmpty();
    }

    // ========== sum() function tests ==========

    @Test
    public void testSumNumericRange() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1));
        valueList.add(Value.of(5));

        final Value result = SumUtils.sum(valueList);
        assertThat(result.getAsDecimal()).isEqualTo(new BigDecimal("15"));
    }

    @Test
    public void testSumNumericRangeSingleValue() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(7));
        valueList.add(Value.of(7));

        final Value result = SumUtils.sum(valueList);
        assertThat(result.getAsDecimal()).isEqualTo(new BigDecimal("7"));
    }

    @Test
    public void testSumNumericRangeLarge() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(10));
        valueList.add(Value.of(100));

        final Value result = SumUtils.sum(valueList);
        // sum(10..100) = (10 + 100) * 91 / 2 = 5005
        assertThat(result.getAsDecimal()).isEqualTo(new BigDecimal("5005"));
    }

    @Test
    public void testSumNumericRangeVeryLarge() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1));
        valueList.add(Value.of(1000));

        final Value result = SumUtils.sum(valueList);
        // sum(1..1000) = 1000 * 1001 / 2 = 500500
        assertThat(result.getAsDecimal()).isEqualTo(new BigDecimal("500500"));
    }

    @Test
    public void testSumNumericRangeNegativeValues() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(-3));
        valueList.add(Value.of(3));

        final Value result = SumUtils.sum(valueList);
        // sum(-3, -2, -1, 0, 1, 2, 3) = 0
        assertThat(result.getAsDecimal()).isEqualTo(new BigDecimal("0"));
    }

    @Test
    public void testSumReversedRange() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(5));
        valueList.add(Value.of(1));

        assertThatThrownBy(() -> SumUtils.sum(valueList))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testSumMixedTypes() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1));
        valueList.add(Value.of("text"));

        assertThatThrownBy(() -> SumUtils.sum(valueList))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testSumNonIntegerRange() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1.5));
        valueList.add(Value.of(5.5));

        assertThatThrownBy(() -> SumUtils.sum(valueList))
            .isInstanceOf(FormulaException.class);
    }

    @Test
    public void testSumIncorrectParameterCount() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1));

        assertThatThrownBy(() -> SumUtils.sum(valueList))
            .isInstanceOf(FormulaException.class);
    }

    // ========== Edge case tests ==========

    @Test
    public void testExpandEntireAlphabet() {
        final List<String> result = SumUtils.expandVariableRange("A", "Z");
        assertThat(result).hasSize(26);
        assertThat(result.get(0)).isEqualTo("A");
        assertThat(result.get(25)).isEqualTo("Z");
    }

    @Test
    public void testExpandEntireAlphabetLowercase() {
        final List<String> result = SumUtils.expandVariableRange("a", "z");
        assertThat(result).hasSize(26);
        assertThat(result.get(0)).isEqualTo("a");
        assertThat(result.get(25)).isEqualTo("z");
    }

    @Test
    public void testSumZeroToZero() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(0));
        valueList.add(Value.of(0));

        final Value result = SumUtils.sum(valueList);
        assertThat(result.getAsDecimal()).isEqualTo(new BigDecimal("0"));
    }

    @Test
    public void testExpandNumericRangeWithLeadingZeros() {
        // Numbers with leading zeros should still be parsed correctly
        final List<String> result = SumUtils.expandVariableRange("$X02", "$X04");
        // Note: Integer.parseInt removes leading zeros
        assertThat(result).isEqualTo(Arrays.asList("X2", "X3", "X4"));
    }

    @Test
    public void testSumVariablesPreservesOrder() {
        final List<String> vars = Arrays.asList("C", "A", "B");
        final Function<String, Value> provider = varName -> {
            switch (varName) {
                case "A": return Value.of(1);
                case "B": return Value.of(2);
                case "C": return Value.of(3);
                default: return null;
            }
        };

        final Pair<BigDecimal, List<String>> result = SumUtils.sumVariables(vars, provider);
        // Sum should be 3+1+2 = 6 (respecting the order C,A,B)
        assertThat(result.first).isEqualTo(new BigDecimal("6"));
        assertThat(result.second).isEmpty();
    }
}
