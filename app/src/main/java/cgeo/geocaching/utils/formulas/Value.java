package cgeo.geocaching.utils.formulas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates a single value for handling in {@link Formula}. Provides e.g. type conversions.
 */
public class Value {

    public static final Value EMPTY = Value.of(null);

    private static final double DOUBLE_DELTA = 0.000000001d;
    private static final Format DOUBLE_TO_STRING_FORMAT = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US));


    private final Object raw;

    //some caching
    private String asString;
    private Double asDouble;
    private Long asInteger;

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

    public boolean isDouble() {
        getAsDouble();
        return !asDouble.isNaN();
    }

    public boolean isInteger() {
        getAsInt();
        return !asInteger.equals(Long.MIN_VALUE);
    }

    public boolean isString() {
        return (raw != null);
    }

    public String getAsString() {
        if (asString == null) {
            if (raw == null) {
                asString = "";
            } else if (raw instanceof Number && !(raw instanceof Integer)) {
                asString = DOUBLE_TO_STRING_FORMAT.format(((Number) raw).doubleValue());
            } else {
                asString = raw instanceof String ? (String) raw : raw.toString();
            }
        }
        return asString;
    }

    public CharSequence getAsCharSequence() {
        return raw instanceof CharSequence ? (CharSequence) raw : getAsString();
    }

    public boolean getAsBoolean() {
        //value is boolean if it is either numeric and > 0 or non-numeric and non-empty as a string
        return isDouble() ? getAsDouble() > 0d + DOUBLE_DELTA : !StringUtils.isBlank(getAsString());
    }

    public long getAsInt() {
        if (asInteger == null) {
            if (raw instanceof Integer || raw instanceof Long) {
                asInteger = ((Number) raw).longValue();
            } else if (raw instanceof Number && Math.abs(Math.round(((Number) raw).doubleValue()) - ((Number) raw).doubleValue()) < DOUBLE_DELTA) {
                asInteger = ((Number) raw).longValue();
            } else {
                try {
                    asInteger = Long.parseLong(getAsString());
                } catch (NumberFormatException nfe) {
                    final double d = getAsDouble();
                    if (isDouble() && d <= Long.MAX_VALUE && d >= Long.MIN_VALUE && Math.abs(Math.round(d) - d) < DOUBLE_DELTA) {
                        asInteger = Math.round(d);
                    } else {
                        asInteger = Long.MIN_VALUE;
                    }
                }
            }
        }
        return asInteger.equals(Long.MIN_VALUE) ? 0L : asInteger;
    }

    public double getAsDouble() {
        if (asDouble == null) {
            if (raw instanceof Number) {
                asDouble = ((Number) raw).doubleValue();
            } else {
                try {
                    asDouble = Double.parseDouble(getAsString().replaceAll(",", "."));
                } catch (NumberFormatException nfe) {
                    asDouble = Double.NaN;
                }
            }
        }
        return asDouble.isNaN() ? 0d : asDouble;
    }

    public String toUserDisplayableString() {
        return getAsString();
    }

    public String getType() {
        return raw == null ? "null" : raw.getClass().getSimpleName();
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (!(obj instanceof Value)) {
            return false;
        }
        final Value other = (Value) obj;
        if (other.isDouble() && this.isDouble()) {
            return Math.abs(other.getAsDouble() - this.getAsDouble()) < DOUBLE_DELTA;
        }
        return Objects.equals(getAsString(), other.getAsString());
    }

    @Override
    public int hashCode() {
        return isDouble() ? (int) Math.round(getAsDouble()) : getAsString().hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return getAsString();
    }
}
