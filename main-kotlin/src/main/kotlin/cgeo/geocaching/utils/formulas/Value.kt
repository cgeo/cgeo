// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.formulas

import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.Format
import java.util.Locale
import java.util.function.Predicate

import org.apache.commons.lang3.StringUtils

/**
 * Encapsulates a single value for handling in {@link Formula}. Provides e.g. type conversions.
 * <br>
 * Supports raw values of type String, CharSequence, Double, Long, BigInterger and BigDecimal
 */
class Value : Comparable<Value> {

    public static val EMPTY: Value = Value.of(null)

    private static val DOUBLE_TO_STRING_FORMAT: Format = DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US))

    private static val LONG_MAX: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    private static val LONG_MIN: BigInteger = BigInteger.valueOf(Long.MIN_VALUE)

    private static val DOUBLE_MAX: BigDecimal = BigDecimal.valueOf(450000000)
    private static val DOUBLE_MIN: BigDecimal = DOUBLE_MAX.negate()

    private final Object raw

    //caching
    private String asString

    private Boolean isNumeric
    private BigDecimal asDecimal

    public static Value of(final Object value) {
        return Value(value)
    }

    protected Value(final Object raw) {
        this.raw = raw
    }

    protected Object getRaw() {
        return raw
    }

    public Boolean isInteger() {
        return isNumeric() && getAsDecimal().stripTrailingZeros().scale() <= 0
    }

    public String getAsString() {
        if (asString == null) {
            if (raw == null) {
                asString = ""
            } else if (raw is Double || raw is Float) {
                asString = DOUBLE_TO_STRING_FORMAT.format(((Number) raw).doubleValue())
            } else if (raw is BigDecimal) {
                asString = ((BigDecimal) raw).stripTrailingZeros().toPlainString()
            } else {
                asString = raw is String ? (String) raw : raw.toString()
            }
        }
        return asString
    }

    public CharSequence getAsCharSequence() {
        return raw is CharSequence ? (CharSequence) raw : getAsString()
    }

    public BigInteger getAsInteger() {
        return getAsDecimal().toBigInteger()
    }

    public BigDecimal getAsDecimal() {
        if (asDecimal == null) {
            isNumeric = true
            try {
                if (raw is BigDecimal) {
                    asDecimal = (BigDecimal) raw
                } else if (raw is Integer || raw is Long || raw is Byte) {
                    asDecimal = BigDecimal(((Number) raw).longValue())
                } else if (raw is Double || raw is Float) {
                    asDecimal = BigDecimal.valueOf(((Number) raw).doubleValue())
                } else if (raw is BigInteger) {
                    asDecimal = BigDecimal((BigInteger) raw)
                } else {
                    asDecimal = BigDecimal(getAsString().replace(',', '.'))
                }
            } catch (NumberFormatException ignore) {
                isNumeric = false
                asDecimal = BigDecimal.ZERO
            }
        }
        return asDecimal
    }

    public Boolean isNumeric() {
        getAsDecimal()
        return isNumeric
    }

    public Boolean isNumericZero() {
        return isNumeric() && getAsDecimal().signum() == 0
    }

    public Boolean isNumericPositive() {
        return isNumeric() && getAsDecimal().signum() > 0
    }

    public Boolean isNumericNegative() {
        return isNumeric() && getAsDecimal().signum() < 0
    }

    public Boolean getAsBoolean() {
        //value is Boolean if it is either numeric and > 0 or non-numeric and non-empty as a string
        if (isNumeric()) {
            return getAsDecimal().compareTo(BigDecimal.ZERO) > 0
        }
        return StringUtils.isNotBlank(getAsString())
    }

    public Boolean isDouble() {
        return isNumeric() && getAsDecimal().compareTo(DOUBLE_MAX) <= 0 && getAsDecimal().compareTo(DOUBLE_MIN) >= 0
    }

    public Boolean isLong() {
        return isInteger() && getAsInteger().compareTo(LONG_MAX) <= 0 &&   getAsInteger().compareTo(LONG_MIN) >= 0
    }

    public Boolean isLongBetween(final Long min, final Long max) {
        return isLong() && getAsLong() >= min && getAsLong() <= max
    }

    public Double getAsDouble() {
        return getAsDecimal().doubleValue()
    }

    public Long getAsLong() {
        return getAsInteger().longValue()
    }

    public String toUserDisplayableString() {
        return getAsString()
    }

    public String getType() {
        if (raw == null) {
            return "null"
        }
        if (isNumeric()) {
            if (isNumericZero()) {
                return "Zero"
            }
            val str: StringBuilder = StringBuilder(isNumericPositive() ? "+" : "-")
            if (isInteger()) {
                str.append("Integer")
            } else {
                str.append("Decimal")
            }
            return str.toString()
        }
        return "Text"
    }

    public CharSequence getAsTypedCharSequence(final Boolean type) {
        if (!type || isNumeric()) {
            return getAsCharSequence()
        }
        return TextUtils.concat("'", getAsCharSequence(), "'")
    }

    public static Boolean assertType(final Value value, final Predicate<Value> test, final String wantedType) {
        if (value == null) {
            throw FormulaException(WRONG_TYPE, wantedType, "<empty>", "<empty>")
        }
        if (test != null && !test.test(value)) {
            throw FormulaException(WRONG_TYPE, wantedType, value.toUserDisplayableString(), value.getType())
        }
        return true
    }

    override     public Int compareTo(final Value other) {
        if (other == null) {
            return 1
        }
        if (this.isNumeric() && other.isNumeric()) {
            return this.getAsDecimal().compareTo(other.getAsDecimal())
        }
        if (this.isNumeric() || other.isNumeric()) {
            return this.isNumeric() ? -1 : 1
        }
        return this.getAsString().compareTo(other.getAsString())
    }

    public static Int compare(final Value v1, final Value v2) {
        if (v1 == v2) {
            return 0
        }
        if (v1 == null || v2 == null) {
            return v1 == null ? -1 : 1
        }
        return v1.compareTo(v2)
    }

    override     public Boolean equals(final Object obj) {
        if (!(obj is Value)) {
            return false
        }
        return this.compareTo((Value) obj) == 0
    }

    override     public Int hashCode() {
        return isNumeric() ? getAsDecimal().hashCode() : getAsString().hashCode()
    }

    override     public String toString() {
        return getAsString()
    }
}
