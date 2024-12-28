package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.util.Locale;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates a single value for handling in {@link Formula}. Provides e.g. type conversions.
 * <br>
 * Supports raw values of type String, CharSequence, Double, Long, BigInterger and BigDecimal
 */
public class Value implements Comparable<Value> {

    public static final Value EMPTY = Value.of(null);

    private static final Format DOUBLE_TO_STRING_FORMAT = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US));

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    private static final BigDecimal DOUBLE_MAX = BigDecimal.valueOf(450000000);
    private static final BigDecimal DOUBLE_MIN = DOUBLE_MAX.negate();

    private final Object raw;

    //caching
    private String asString;

    private boolean isNumeric;
    private BigDecimal asDecimal;

    public static Value of(final Object value) {
        return new Value(value);
    }

    protected Value(final Object raw) {
        this.raw = raw;
    }

    @Nullable
    protected Object getRaw() {
        return raw;
    }

    public boolean isInteger() {
        return isNumeric() && getAsDecimal().stripTrailingZeros().scale() <= 0;
    }

    public String getAsString() {
        if (asString == null) {
            if (raw == null) {
                asString = "";
            } else if (raw instanceof Double || raw instanceof Float) {
                asString = DOUBLE_TO_STRING_FORMAT.format(((Number) raw).doubleValue());
            } else if (raw instanceof BigDecimal) {
                asString = ((BigDecimal) raw).stripTrailingZeros().toPlainString();
            } else {
                asString = raw instanceof String ? (String) raw : raw.toString();
            }
        }
        return asString;
    }

    public CharSequence getAsCharSequence() {
        return raw instanceof CharSequence ? (CharSequence) raw : getAsString();
    }

    public BigInteger getAsInteger() {
        return getAsDecimal().toBigInteger();
    }

    public BigDecimal getAsDecimal() {
        if (asDecimal == null) {
            isNumeric = true;
            try {
                if (raw instanceof BigDecimal) {
                    asDecimal = (BigDecimal) raw;
                } else if (raw instanceof Integer || raw instanceof Long || raw instanceof Byte) {
                    asDecimal = new BigDecimal(((Number) raw).longValue());
                } else if (raw instanceof Double || raw instanceof Float) {
                    asDecimal = BigDecimal.valueOf(((Number) raw).doubleValue());
                } else if (raw instanceof BigInteger) {
                    asDecimal = new BigDecimal((BigInteger) raw);
                } else {
                    asDecimal = new BigDecimal(getAsString().replace(',', '.'));
                }
            } catch (NumberFormatException ignore) {
                isNumeric = false;
                asDecimal = BigDecimal.ZERO;
            }
        }
        return asDecimal;
    }

    public boolean isNumeric() {
        getAsDecimal();
        return isNumeric;
    }

    public boolean isNumericZero() {
        return isNumeric() && getAsDecimal().signum() == 0;
    }

    public boolean isNumericPositive() {
        return isNumeric() && getAsDecimal().signum() > 0;
    }

    public boolean isNumericNegative() {
        return isNumeric() && getAsDecimal().signum() < 0;
    }

    public boolean getAsBoolean() {
        //value is boolean if it is either numeric and > 0 or non-numeric and non-empty as a string
        if (isNumeric()) {
            return getAsDecimal().compareTo(BigDecimal.ZERO) > 0;
        }
        return StringUtils.isNotBlank(getAsString());
    }

    public boolean isDouble() {
        return isNumeric() && getAsDecimal().compareTo(DOUBLE_MAX) <= 0 && getAsDecimal().compareTo(DOUBLE_MIN) >= 0;
    }

    public boolean isLong() {
        return isInteger() && getAsInteger().compareTo(LONG_MAX) <= 0 &&   getAsInteger().compareTo(LONG_MIN) >= 0;
    }

    public boolean isLongBetween(final long min, final long max) {
        return isLong() && getAsLong() >= min && getAsLong() <= max;
    }

    public double getAsDouble() {
        return getAsDecimal().doubleValue();
    }

    public long getAsLong() {
        return getAsInteger().longValue();
    }

    public String toUserDisplayableString() {
        return getAsString();
    }

    public String getType() {
        if (raw == null) {
            return "null";
        }
        if (isNumeric()) {
            if (isNumericZero()) {
                return "Zero";
            }
            final StringBuilder str = new StringBuilder(isNumericPositive() ? "+" : "-");
            if (isInteger()) {
                str.append("Integer");
            } else {
                str.append("Decimal");
            }
            return str.toString();
        }
        return "Text";
    }

    public CharSequence getAsTypedCharSequence(final boolean type) {
        if (!type || isNumeric()) {
            return getAsCharSequence();
        }
        return TextUtils.concat("'", getAsCharSequence(), "'");
    }

    public static boolean assertType(final Value value, final Predicate<Value> test, final String wantedType) {
        if (value == null) {
            throw new FormulaException(WRONG_TYPE, wantedType, "<empty>", "<empty>");
        }
        if (test != null && !test.test(value)) {
            throw new FormulaException(WRONG_TYPE, wantedType, value.toUserDisplayableString(), value.getType());
        }
        return true;
    }

    @Override
    public int compareTo(final Value other) {
        if (other == null) {
            return 1;
        }
        if (this.isNumeric() && other.isNumeric()) {
            return this.getAsDecimal().compareTo(other.getAsDecimal());
        }
        if (this.isNumeric() || other.isNumeric()) {
            return this.isNumeric() ? -1 : 1;
        }
        return this.getAsString().compareTo(other.getAsString());
    }

    public static int compare(final Value v1, final Value v2) {
        if (v1 == v2) {
            return 0;
        }
        if (v1 == null || v2 == null) {
            return v1 == null ? -1 : 1;
        }
        return v1.compareTo(v2);
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (!(obj instanceof Value)) {
            return false;
        }
        return this.compareTo((Value) obj) == 0;
    }

    @Override
    public int hashCode() {
        return isNumeric() ? getAsDecimal().hashCode() : getAsString().hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return getAsString();
    }
}
