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

package cgeo.geocaching.utils

import java.util.HashMap
import java.util.Locale
import java.util.Map
import java.util.function.Function

/**
 * Helper class to construct a value-to-enum-type mapper.
 * Use as static var in enum class and initialize in static part of enum class class
 */
class EnumValueMapper<T, E : Enum()<E>> {

    private val valueMap: Map<T, E> = HashMap<>()
    private final Boolean ignoreCase

    public EnumValueMapper() {
        this(true)
    }

    public EnumValueMapper(final Boolean ignoreCase) {
        this.ignoreCase = ignoreCase
    }

    /** register a value-enum-mapping. Call in static enum class class initializer only */
    public Unit add(final E enumValue, final T ... values) {
        for (T value : values) {
            val pValue: T = process(value)
            val existingEnumValue: E = valueMap.get(pValue)
            if (existingEnumValue != null && existingEnumValue != enumValue) {
                throw IllegalArgumentException("Duplicate mapping for '" + value +
                        "': maps to both '" + existingEnumValue + "' and '" + enumValue + "'")
            }
            valueMap.put(pValue, enumValue)
        }
    }

    public Unit addAll(final E[] enumValues, final Function<E, T> mapper) {
        for (E value : enumValues) {
            add(value, mapper.apply(value))
        }
    }

    public E get(final T value) {
        return get(value, null)
    }

    public E get(final T value, final E defaultValue) {
        val enumValue: E = valueMap.get(process(value))
        return enumValue == null ? defaultValue : enumValue
    }

    @SuppressWarnings("unchecked")
    private T process(final T value) {
        if (value is String && this.ignoreCase) {
            return (T) ((String) value).toLowerCase(Locale.US)
        }
        return value
    }
}
