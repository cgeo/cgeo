package cgeo.geocaching.utils.formulas;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class IntegerRangeTest {

    @Test
    public void testSingleValue() {
        final IntegerRange range = IntegerRange.createFromConfig("5");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(1);
        assertThat(range.getValue(0)).isEqualTo(5);
    }

    @Test
    public void testSimpleRange() {
        final IntegerRange range = IntegerRange.createFromConfig("1-3");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(3);
        assertThat(range.getValue(0)).isEqualTo(1);
        assertThat(range.getValue(1)).isEqualTo(2);
        assertThat(range.getValue(2)).isEqualTo(3);
    }

    @Test
    public void testMultipleValues() {
        final IntegerRange range = IntegerRange.createFromConfig("1,3,5");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(3);
        assertThat(range.getValue(0)).isEqualTo(1);
        assertThat(range.getValue(1)).isEqualTo(3);
        assertThat(range.getValue(2)).isEqualTo(5);
    }

    @Test
    public void testMixedRangeAndValues() {
        final IntegerRange range = IntegerRange.createFromConfig("1-3,8,5-7");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(7);
        assertThat(range.getValue(0)).isEqualTo(1);
        assertThat(range.getValue(1)).isEqualTo(2);
        assertThat(range.getValue(2)).isEqualTo(3);
        assertThat(range.getValue(3)).isEqualTo(8);
        assertThat(range.getValue(4)).isEqualTo(5);
        assertThat(range.getValue(5)).isEqualTo(6);
        assertThat(range.getValue(6)).isEqualTo(7);
    }

    @Test
    public void testReverseRange() {
        final IntegerRange range = IntegerRange.createFromConfig("5-3");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(3);
        assertThat(range.getValue(0)).isEqualTo(3);
        assertThat(range.getValue(1)).isEqualTo(4);
        assertThat(range.getValue(2)).isEqualTo(5);
    }

    @Test
    public void testOutOfBoundsNegative() {
        final IntegerRange range = IntegerRange.createFromConfig("1-3");
        assertThat(range).isNotNull();
        assertThat(range.getValue(-1)).isEqualTo(0);
    }

    @Test
    public void testOutOfBoundsPositive() {
        final IntegerRange range = IntegerRange.createFromConfig("1-3");
        assertThat(range).isNotNull();
        assertThat(range.getValue(5)).isEqualTo(0);
    }

    @Test
    public void testInvalidConfig() {
        final IntegerRange range = IntegerRange.createFromConfig("abc");
        assertThat(range).isNull();
    }

    @Test
    public void testEmptyConfig() {
        final IntegerRange range = IntegerRange.createFromConfig("");
        assertThat(range).isNull();
    }

    @Test
    public void testRangeWithSpaces() {
        // Assuming spaces are treated as part of config and will fail parsing
        final IntegerRange range = IntegerRange.createFromConfig("1, 2, 3");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(3);
    }

    @Test
    public void testSingleValueRange() {
        final IntegerRange range = IntegerRange.createFromConfig("5-5");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(1);
        assertThat(range.getValue(0)).isEqualTo(5);
    }

    @Test
    public void testLargeRange() {
        final IntegerRange range = IntegerRange.createFromConfig("1-100");
        assertThat(range).isNotNull();
        assertThat(range.getSize()).isEqualTo(100);
        assertThat(range.getValue(0)).isEqualTo(1);
        assertThat(range.getValue(50)).isEqualTo(51);
        assertThat(range.getValue(99)).isEqualTo(100);
    }
}
