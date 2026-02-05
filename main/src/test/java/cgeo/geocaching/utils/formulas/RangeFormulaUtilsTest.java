package cgeo.geocaching.utils.formulas;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class RangeFormulaUtilsTest {

    @Test
    public void testNumericRangeSimple() {
        assertThat(expandNumericRanges("1-5")).isEqualTo(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void testNumericRangeNegative() {
        assertThat(expandNumericRanges("(-3)-5")).isEqualTo(Arrays.asList(-3, -2, -1, 0, 1, 2, 3, 4, 5));
        assertThat(expandNumericRanges("(-5)-(-3)")).isEqualTo(Arrays.asList(-5, -4, -3));
    }

    @Test
    public void testNumericRangeWithLeadingZeros() {
        assertThat(expandNumericRanges("01-12")).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        assertThat(expandNumericRanges("001-112").size()).isEqualTo(112);
    }

    @Test
    public void testSingleLetterRange() {
        assertThat(expandVariableRanges("A-D")).isEqualTo(Arrays.asList("A", "B", "C", "D"));
        assertThat(expandVariableRanges("a-d")).isEqualTo(Arrays.asList("a", "b", "c", "d"));
        assertThat(expandVariableRanges("$A-$D")).isEqualTo(Arrays.asList("A", "B", "C", "D"));
        assertThat(expandVariableRanges("$A-$Z").size()).isEqualTo(26);
        assertThat(expandVariableRanges("A-$D")).isEqualTo(Arrays.asList("A", "B", "C", "D"));
    }

    @Test
    public void testSingleRangeReverse() {
        assertThatThrownBy(() -> expandNumericRanges("265-1"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> expandVariableRanges("D-A"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> expandVariableRanges("$A12-$A1"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testVariableWithNumericSuffix() {
        assertThat(expandVariableRanges("$A1-$A3")).isEqualTo(Arrays.asList("A1", "A2", "A3"));

        final List<String> list1 = expandVariableRanges("$nA1-$nA12");
        assertThat(list1.size()).isEqualTo(12);
        assertThat(list1.get(0)).isEqualTo("nA1");
        assertThat(list1.get(11)).isEqualTo("nA12");

        final List<String> list2 = expandVariableRanges("$Na001-$Na112");
        assertThat(list2.size()).isEqualTo(112);
        assertThat(list2.get(0)).isEqualTo("Na001");
        assertThat(list2.get(98)).isEqualTo("Na099");
        assertThat(list2.get(111)).isEqualTo("Na112");
    }

    @Test
    public void testInvalidRangeMixed() {
        assertThatThrownBy(() -> expandVariableRanges("A-d"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> expandVariableRanges("A-5"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");


        assertThatThrownBy(() -> expandVariableRanges("$A1-$A"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> expandVariableRanges("$A1-$B1"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");

        assertThatThrownBy(() -> expandVariableRanges("$A1-$a3"))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("INVALID_RANGE");
    }

    @Test
    public void testInvalidVariable() {
        assertThat(expandVariableRanges("ABC").isEmpty()).isEqualTo(true);
        assertThat(expandVariableRanges("").isEmpty()).isEqualTo(true);
        assertThat(expandVariableRanges(null).isEmpty()).isEqualTo(true);
    }


    private static List<Integer> expandNumericRanges(final String rangeString) {
        final List<Value> range = RangeParser.createFromString(rangeString);
        return range.stream().filter(Value::isNumeric).map(vv -> vv.getAsInteger().intValue()).collect(Collectors.toList());
    }

    private static List<String> expandVariableRanges(final String rangeString) {
        final List<Value> range = RangeParser.createFromString(rangeString);
        return range.stream().filter(vv -> !vv.isNumeric()).map(Value::getAsString).collect(Collectors.toList());
    }
}
