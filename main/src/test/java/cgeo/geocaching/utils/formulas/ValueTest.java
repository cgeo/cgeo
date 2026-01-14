package cgeo.geocaching.utils.formulas;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ValueTest {

    @Test
    public void testOfNull() {
        final Value value = Value.of(null);
        assertThat(value.getAsString()).isEmpty();
        assertThat(value.isNumeric()).isFalse();
    }

    @Test
    public void testOfInteger() {
        final Value value = Value.of(42);
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.isInteger()).isTrue();
        assertThat(value.getAsLong()).isEqualTo(42);
        assertThat(value.getAsString()).isEqualTo("42");
    }

    @Test
    public void testOfLong() {
        final Value value = Value.of(123456789L);
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.isInteger()).isTrue();
        assertThat(value.getAsLong()).isEqualTo(123456789L);
    }

    @Test
    public void testOfDouble() {
        final Value value = Value.of(3.14);
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.isInteger()).isFalse();
        assertThat(value.getAsDouble()).isEqualTo(3.14, within(0.001));
    }

    @Test
    public void testOfBigDecimal() {
        final Value value = Value.of(new BigDecimal("123.456"));
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.isInteger()).isFalse();
        assertThat(value.getAsString()).isEqualTo("123.456");
    }

    @Test
    public void testOfBigInteger() {
        final Value value = Value.of(BigInteger.valueOf(999));
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.isInteger()).isTrue();
        assertThat(value.getAsLong()).isEqualTo(999);
    }

    @Test
    public void testOfString() {
        final Value value = Value.of("hello");
        assertThat(value.isNumeric()).isFalse();
        assertThat(value.getAsString()).isEqualTo("hello");
    }

    @Test
    public void testOfNumericString() {
        final Value value = Value.of("42");
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.isInteger()).isTrue();
        assertThat(value.getAsLong()).isEqualTo(42);
    }

    @Test
    public void testOfDecimalString() {
        final Value value = Value.of("3.14");
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.isInteger()).isFalse();
        assertThat(value.getAsDouble()).isEqualTo(3.14, within(0.001));
    }

    @Test
    public void testOfDecimalStringWithComma() {
        final Value value = Value.of("3,14");
        assertThat(value.isNumeric()).isTrue();
        assertThat(value.getAsDouble()).isEqualTo(3.14, within(0.001));
    }

    @Test
    public void testIsNumericZero() {
        final Value value = Value.of(0);
        assertThat(value.isNumericZero()).isTrue();
        assertThat(value.isNumericPositive()).isFalse();
        assertThat(value.isNumericNegative()).isFalse();
    }

    @Test
    public void testIsNumericPositive() {
        final Value value = Value.of(5);
        assertThat(value.isNumericPositive()).isTrue();
        assertThat(value.isNumericZero()).isFalse();
        assertThat(value.isNumericNegative()).isFalse();
    }

    @Test
    public void testIsNumericNegative() {
        final Value value = Value.of(-5);
        assertThat(value.isNumericNegative()).isTrue();
        assertThat(value.isNumericZero()).isFalse();
        assertThat(value.isNumericPositive()).isFalse();
    }

    @Test
    public void testGetAsBooleanNumeric() {
        assertThat(Value.of(0).getAsBoolean()).isFalse();
        assertThat(Value.of(1).getAsBoolean()).isTrue();
        assertThat(Value.of(-1).getAsBoolean()).isFalse();
        assertThat(Value.of(42).getAsBoolean()).isTrue();
    }

    @Test
    public void testGetAsBooleanString() {
        assertThat(Value.of("").getAsBoolean()).isFalse();
        assertThat(Value.of("  ").getAsBoolean()).isFalse();
        assertThat(Value.of("hello").getAsBoolean()).isTrue();
    }

    @Test
    public void testCompareToNumeric() {
        final Value v1 = Value.of(10);
        final Value v2 = Value.of(20);
        final Value v3 = Value.of(10);
        
        assertThat(v1.compareTo(v2)).isLessThan(0);
        assertThat(v2.compareTo(v1)).isGreaterThan(0);
        assertThat(v1.compareTo(v3)).isEqualTo(0);
    }

    @Test
    public void testCompareToString() {
        final Value v1 = Value.of("apple");
        final Value v2 = Value.of("banana");
        final Value v3 = Value.of("apple");
        
        assertThat(v1.compareTo(v2)).isLessThan(0);
        assertThat(v2.compareTo(v1)).isGreaterThan(0);
        assertThat(v1.compareTo(v3)).isEqualTo(0);
    }

    @Test
    public void testCompareToMixed() {
        final Value numeric = Value.of(10);
        final Value text = Value.of("hello");
        
        // Numeric values are considered less than text values
        assertThat(numeric.compareTo(text)).isLessThan(0);
        assertThat(text.compareTo(numeric)).isGreaterThan(0);
    }

    @Test
    public void testEqualsNumeric() {
        final Value v1 = Value.of(42);
        final Value v2 = Value.of(42.0);
        final Value v3 = Value.of(new BigDecimal("42"));
        
        assertThat(v1.equals(v2)).isTrue();
        assertThat(v1.equals(v3)).isTrue();
    }

    @Test
    public void testEqualsString() {
        final Value v1 = Value.of("hello");
        final Value v2 = Value.of("hello");
        final Value v3 = Value.of("world");
        
        assertThat(v1.equals(v2)).isTrue();
        assertThat(v1.equals(v3)).isFalse();
    }

    @Test
    public void testGetType() {
        assertThat(Value.of(null).getType()).isEqualTo("null");
        assertThat(Value.of(0).getType()).isEqualTo("Zero");
        assertThat(Value.of(5).getType()).isEqualTo("+Integer");
        assertThat(Value.of(-5).getType()).isEqualTo("-Integer");
        assertThat(Value.of(3.14).getType()).isEqualTo("+Decimal");
        assertThat(Value.of(-3.14).getType()).isEqualTo("-Decimal");
        assertThat(Value.of("hello").getType()).isEqualTo("Text");
    }

    @Test
    public void testIsDouble() {
        assertThat(Value.of(100).isDouble()).isTrue();
        assertThat(Value.of(3.14).isDouble()).isTrue();
        assertThat(Value.of(new BigDecimal("999999999")).isDouble()).isFalse();
    }

    @Test
    public void testIsLong() {
        assertThat(Value.of(100).isLong()).isTrue();
        assertThat(Value.of(3.14).isLong()).isFalse();
        assertThat(Value.of(Long.MAX_VALUE).isLong()).isTrue();
    }

    @Test
    public void testIsLongBetween() {
        final Value value = Value.of(50);
        assertThat(value.isLongBetween(0, 100)).isTrue();
        assertThat(value.isLongBetween(51, 100)).isFalse();
        assertThat(value.isLongBetween(0, 49)).isFalse();
    }

    @Test
    public void testToString() {
        final Value value = Value.of(42);
        assertThat(value.toString()).isEqualTo("42");
    }

    @Test
    public void testHashCode() {
        final Value v1 = Value.of(42);
        final Value v2 = Value.of(42);
        final Value v3 = Value.of(43);
        
        assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        assertThat(v1.hashCode()).isNotEqualTo(v3.hashCode());
    }

    @Test
    public void testEmptyValue() {
        assertThat(Value.EMPTY).isNotNull();
        assertThat(Value.EMPTY.getAsString()).isEmpty();
        assertThat(Value.EMPTY.isNumeric()).isFalse();
    }

    @Test
    public void testCompareStatic() {
        final Value v1 = Value.of(10);
        final Value v2 = Value.of(20);
        
        assertThat(Value.compare(v1, v2)).isLessThan(0);
        assertThat(Value.compare(v2, v1)).isGreaterThan(0);
        assertThat(Value.compare(v1, v1)).isEqualTo(0);
        assertThat(Value.compare(null, v1)).isLessThan(0);
        assertThat(Value.compare(v1, null)).isGreaterThan(0);
        assertThat(Value.compare(null, null)).isEqualTo(0);
    }

    // Helper method for floating point comparison
    private static org.assertj.core.data.Offset<Double> within(final double offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}
