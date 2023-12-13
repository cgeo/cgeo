package cgeo.geocaching.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class to construct a value-to-enum-type mapper.
 * Use as static var in enum and initialize in static part of enum class
 */
public class EnumValueMapper<T, E extends Enum<E>> {

    private final Map<T, E> valueMap = new HashMap<>();
    private final boolean ignoreCase;

    public EnumValueMapper() {
        this(true);
    }

    public EnumValueMapper(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /** register a value-enum-mapping. Call in static enum class initializer only */
    public void add(final E enumValue, final T ... values) {
        for (T value : values) {
            final T pValue = process(value);
            final E existingEnumValue = valueMap.get(pValue);
            if (existingEnumValue != null && existingEnumValue != enumValue) {
                throw new IllegalArgumentException("Duplicate mapping for '" + value +
                        "': maps to both '" + existingEnumValue + "' and '" + enumValue + "'");
            }
            valueMap.put(pValue, enumValue);
        }
    }

    public E get(final T value) {
        return get(value, null);
    }

    public E get(final T value, final E defaultValue) {
        final E enumValue = valueMap.get(process(value));
        return enumValue == null ? defaultValue : enumValue;
    }

    @SuppressWarnings("unchecked")
    private T process(final T value) {
        if (value instanceof String && this.ignoreCase) {
            return (T) ((String) value).toLowerCase(Locale.US);
        }
        return value;
    }
}
