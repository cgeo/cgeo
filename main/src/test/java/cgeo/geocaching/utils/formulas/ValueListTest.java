package cgeo.geocaching.utils.formulas;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ValueListTest {

    @Test
    public void testOfPlain() {
        final ValueList list = ValueList.ofPlain(1, 2, 3);
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0).getAsLong()).isEqualTo(1);
        assertThat(list.get(1).getAsLong()).isEqualTo(2);
        assertThat(list.get(2).getAsLong()).isEqualTo(3);
    }

    @Test
    public void testOfValues() {
        final ValueList list = ValueList.of(Value.of(1), Value.of(2));
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getAsLong()).isEqualTo(1);
        assertThat(list.get(1).getAsLong()).isEqualTo(2);
    }

    @Test
    public void testAdd() {
        final ValueList list = new ValueList();
        list.add(Value.of(1), Value.of(2), Value.of(3));
        assertThat(list.size()).isEqualTo(3);
    }

    @Test
    public void testGetOutOfBounds() {
        final ValueList list = ValueList.ofPlain(1, 2);
        assertThat(list.get(5)).isEqualTo(Value.EMPTY);
        assertThat(list.get(-1)).isEqualTo(Value.EMPTY);
    }

    @Test
    public void testGetAsString() {
        final ValueList list = ValueList.ofPlain("hello", "world");
        assertThat(list.getAsString(0, "default")).isEqualTo("hello");
        assertThat(list.getAsString(1, "default")).isEqualTo("world");
        assertThat(list.getAsString(2, "default")).isEqualTo("default");
    }

    @Test
    public void testGetAsDecimal() {
        final ValueList list = ValueList.ofPlain(42, 3.14, "not a number");
        assertThat(list.getAsDecimal(0).intValue()).isEqualTo(42);
        assertThat(list.getAsDecimal(1).doubleValue()).isEqualTo(3.14, within(0.001));
        assertThat(list.getAsDecimal(2).intValue()).isEqualTo(0); // Default for non-numeric
    }

    @Test
    public void testGetAsDecimalWithDefault() {
        final ValueList list = ValueList.ofPlain("not a number");
        assertThat(list.getAsDecimal(0, Value.of(99).getAsDecimal()).intValue()).isEqualTo(99);
    }

    @Test
    public void testSize() {
        final ValueList emptyList = new ValueList();
        assertThat(emptyList.size()).isEqualTo(0);
        
        final ValueList list = ValueList.ofPlain(1, 2, 3);
        assertThat(list.size()).isEqualTo(3);
    }

    @Test
    public void testIterator() {
        final ValueList list = ValueList.ofPlain(1, 2, 3);
        int count = 0;
        for (final Value v : list) {
            count++;
            assertThat(v.isNumeric()).isTrue();
        }
        assertThat(count).isEqualTo(3);
    }

    @Test
    public void testAssertCheckCount() {
        final ValueList list = ValueList.ofPlain(1, 2, 3);
        
        // Valid counts
        assertThat(list.assertCheckCount(3, 3, true)).isTrue();
        assertThat(list.assertCheckCount(1, 5, true)).isTrue();
        assertThat(list.assertCheckCount(3, -1, true)).isTrue(); // -1 means no max
        
        // Invalid counts
        assertThat(list.assertCheckCount(4, 5, true)).isFalse();
        assertThat(list.assertCheckCount(1, 2, true)).isFalse();
    }

    @Test
    public void testAssertCheckCountThrows() {
        final ValueList list = ValueList.ofPlain(1, 2, 3);
        
        try {
            list.assertCheckCount(4, 5, false);
            assertThat(false).as("Should have thrown FormulaException").isTrue();
        } catch (final FormulaException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    public void testAssertCheckType() {
        final ValueList list = ValueList.ofPlain(1, 2, "text");
        
        // Check that first value is numeric
        assertThat(list.assertCheckType(0, Value::isNumeric, "numeric", true)).isTrue();
        
        // Check that third value is numeric (should fail)
        assertThat(list.assertCheckType(2, Value::isNumeric, "numeric", true)).isFalse();
    }

    @Test
    public void testAssertCheckTypeThrows() {
        final ValueList list = ValueList.ofPlain("text");
        
        try {
            list.assertCheckType(0, Value::isNumeric, "numeric", false);
            assertThat(false).as("Should have thrown FormulaException").isTrue();
        } catch (final FormulaException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    public void testMixedTypes() {
        final ValueList list = ValueList.ofPlain(1, "hello", 3.14, true);
        assertThat(list.size()).isEqualTo(4);
        assertThat(list.get(0).isNumeric()).isTrue();
        assertThat(list.get(1).isNumeric()).isFalse();
        assertThat(list.get(2).isNumeric()).isTrue();
        assertThat(list.get(3).getAsString()).isEqualTo("true");
    }

    // Helper method for floating point comparison
    private static org.assertj.core.data.Offset<Double> within(final double offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}
