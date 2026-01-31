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
    public void testExpandSingleLetterRange() {
        final List<String> resultUppercase = SumUtils.expandVariableRange("A", "D");
        assertThat(resultUppercase).isEqualTo(Arrays.asList("A", "B", "C", "D"));

        final List<String> resultLowercase = SumUtils.expandVariableRange("a", "d");
        assertThat(resultLowercase).isEqualTo(Arrays.asList("a", "b", "c", "d"));

        final List<String> resultDollar = SumUtils.expandVariableRange("$A", "$D");
        assertThat(resultDollar).isEqualTo(Arrays.asList("A", "B", "C", "D"));

        final List<String> resultSingleRange = SumUtils.expandVariableRange("X", "X");
        assertThat(resultSingleRange).isEqualTo(Arrays.asList("X"));

        final List<String> resultDollarMix = SumUtils.expandVariableRange("A", "$D");
        assertThat(resultDollarMix).isEqualTo(Arrays.asList("A", "B", "C", "D"));
    }

    @Test
    public void testExpandLetterSuffixRange() {
        final List<String> resultUppercase = SumUtils.expandVariableRange("$NA", "$ND");
        assertThat(resultUppercase).isEqualTo(Arrays.asList("NA", "NB", "NC", "ND"));

        final List<String> resultLowercase = SumUtils.expandVariableRange("$na", "$nd");
        assertThat(resultLowercase).isEqualTo(Arrays.asList("na", "nb", "nc", "nd"));
    }

    @Test
    public void testExpandDollarMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("A1", "A5"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("UNEXPECTED_TOKEN");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$", "$A"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("UNEXPECTED_TOKEN");
    }

    @Test
    public void testExpandNumericSuffixRange() {
        final List<String> resultX = SumUtils.expandVariableRange("$X8", "$X12");
        assertThat(resultX).isEqualTo(Arrays.asList("X8", "X9", "X10", "X11", "X12"));

        final List<String> resultX0 = SumUtils.expandVariableRange("$X08", "$X12");
        assertThat(resultX0).isEqualTo(Arrays.asList("X08", "X09", "X10", "X11", "X12"));

        final List<String> resultXX = SumUtils.expandVariableRange("$XX10", "$XX12");
        assertThat(resultXX).isEqualTo(Arrays.asList("XX10", "XX11", "XX12"));
    }

    @Test
    public void testExpandLetterSuffixRangeLongerPrefix() {
        final List<String> resultChar = SumUtils.expandVariableRange("$VARA", "$VARC");
        assertThat(resultChar).isEqualTo(Arrays.asList("VARA", "VARB", "VARC"));

        final List<String> resultNumber = SumUtils.expandVariableRange("$VAR11", "$VAR13");
        assertThat(resultNumber).isEqualTo(Arrays.asList("VAR11", "VAR12", "VAR13"));
    }

    @Test
    public void testExpandPrefixMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("a", "D"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$NA", "$nb"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$NA", "$MB"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A1", "$B5"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A1", "$a5"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$VARA", "$VarC"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testExpandOrderMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("D", "A"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A5", "$A1"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testExpandNumericSuffixMismatch() {
        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A1", "$AB"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$NA", "$Nc"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A05", "$A8"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A5", "$A08"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> SumUtils.expandVariableRange("$A05", "$A111"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
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
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("WRONG_TYPE");
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
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testSumMixedTypes() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1));
        valueList.add(Value.of("text"));

        assertThatThrownBy(() -> SumUtils.sum(valueList))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("WRONG_TYPE");
    }

    @Test
    public void testSumNonIntegerRange() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1.5));
        valueList.add(Value.of(5.5));

        assertThatThrownBy(() -> SumUtils.sum(valueList))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("WRONG_TYPE");
    }

    @Test
    public void testSumIncorrectParameterCount() {
        final ValueList valueList = new ValueList();
        valueList.add(Value.of(1));

        assertThatThrownBy(() -> SumUtils.sum(valueList))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("WRONG_PARAMETER_COUNT");
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
