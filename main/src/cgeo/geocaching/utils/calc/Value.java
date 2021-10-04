package cgeo.geocaching.utils.calc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.Format;

/** Encapsulates a single value for handling in {@link cgeo.geocaching.utils.calc.Calculator}. Provides e.g. type conversions. */
public class Value {

    public static final Value EMPTY = Value.of(null);

    private static final double DOUBLE_DELTA = 0.000000001d;
    private static final Format DOUBLE_TO_STRING_FORMAT = new DecimalFormat("#.######");


    private final Object raw;

    //some caching
    private String asString;
    private Double asDouble;
    private Integer asInteger;

    public static Value of(final Object value) {
        return new Value(value);
    }

    private Value(final Object raw) {
        this.raw = raw;
    }

    @Nullable
    public Object getRaw() {
        return raw;
    }

    public boolean isDouble() {
        getAsDouble();
        return !asDouble.isNaN();
    }

    public boolean isInteger() {
        getAsInt();
        return !asInteger.equals(Integer.MIN_VALUE);
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
                asString = raw.toString();
            }
        }
        return asString;
    }

    public int getAsInt() {
        if (asInteger == null) {
            if (raw instanceof Integer) {
                asInteger = ((Number) raw).intValue();
            } else if (raw instanceof Number && Math.abs(Math.round(((Number) raw).doubleValue()) - ((Number) raw).doubleValue()) < DOUBLE_DELTA) {
                asInteger = ((Number) raw).intValue();
            } else {
                final double d = getAsDouble();
                if (Math.abs(Math.round(d) - d) < DOUBLE_DELTA) {
                    asInteger = (int) Math.round(d);
                } else {
                    asInteger = Integer.MIN_VALUE;
                }
            }
        }
        return asInteger.equals(Integer.MIN_VALUE) ? 0 : asInteger;
    }

    public double getAsDouble() {
        if (asDouble == null) {
            if (raw instanceof Number) {
                asDouble = ((Number) raw).doubleValue();
            } else {
                try {
                    asDouble = Double.parseDouble(getAsString());
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

    @NonNull
    @Override
    public String toString() {
        return getAsString();
    }

    public String getType() {
        return raw == null ? "null" : raw.getClass().getSimpleName();
    }

}
