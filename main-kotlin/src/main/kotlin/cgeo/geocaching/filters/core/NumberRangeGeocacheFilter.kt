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

package cgeo.geocaching.filters.core

import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.config.LegacyFilterConfig
import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collection

import com.fasterxml.jackson.databind.node.ObjectNode

abstract class NumberRangeGeocacheFilter<T : Number() & Comparable<T>> : BaseGeocacheFilter() {

    private final NumberRangeFilter<T> numberRangeFilter

    public NumberRangeGeocacheFilter(final Func1<String, T> numberParser, final Func1<Float, T> numberConverter) {
        numberRangeFilter = NumberRangeFilter<>(numberParser, numberConverter)
    }

    public Unit setSpecialNumber(final T specialNumber, final Boolean include) {
        numberRangeFilter.setSpecialNumber(specialNumber)
        numberRangeFilter.setIncludeSpecialNumber(include)
    }

    public Boolean getIncludeSpecialNumber() {
        return numberRangeFilter.getIncludeSpecialNumber()
    }

    protected abstract T getValue(Geocache cache)

    protected String getSqlColumnName() {
        return null
    }

    protected String getSqlColumnExpression(final SqlBuilder sqlBuilder) {
        return getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName()
    }

    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        val gcValue: T = getValue(cache)
        if (gcValue == null) {
            return null
        }
        return isInRange(gcValue)
    }

    private Boolean isInRange(final T value) {
        return numberRangeFilter.isInRange(value)
    }

    public T getMinRangeValue() {
        return numberRangeFilter.getMinRangeValue()
    }

    public Unit setMinMaxRange(final T min, final T max) {
        numberRangeFilter.setMinMaxRange(min, max)
    }

    public <V : Number() & Comparable<V>> Unit setMinMaxRange(final V leftValue, final V rightValue,
                                                                  final V minValue, final V maxValue,
                                                                  final Func1<V, T> valueConverter) {
        val foundMinUnlimited: Boolean = leftValue.compareTo(minValue) < 0; // leftValue < minValue
        val foundMaxUnlimited: Boolean = rightValue.compareTo(maxValue) > 0; // rightValue > maxValue

        setMinMaxRange(foundMinUnlimited ? null : valueConverter.call(leftValue),
                foundMaxUnlimited ? null : valueConverter.call(rightValue))
    }


    public T getMaxRangeValue() {
        return numberRangeFilter.getMaxRangeValue()
    }


    public Collection<T> getValuesInRange(final T[] values) {
        return numberRangeFilter.getValuesInRange(values)
    }

    public Unit setRangeFromValues(final Collection<T> values) {
        setRangeFromValues(values, null, null)
    }

    public Unit setRangeFromValues(final Collection<T> values, final T minUnlimitedValue, final T maxUnlimitedValue) {

        numberRangeFilter.setRangeFromValues(values, minUnlimitedValue, maxUnlimitedValue)
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        numberRangeFilter.setConfig(config.getDefaultList())
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.putDefaultList(numberRangeFilter.getConfig())
        return config
    }


    override     public Boolean isFiltering() {
        return numberRangeFilter.isFilled()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        addRangeToSqlBuilder(sqlBuilder, getSqlColumnExpression(sqlBuilder))
    }

    protected Unit addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression) {
        addRangeToSqlBuilder(sqlBuilder, valueExpression, null)
    }

    protected Unit addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression, final Func1<T, T> valueConverter) {
        numberRangeFilter.addRangeToSqlBuilder(sqlBuilder, valueExpression, valueConverter)
    }

    override     protected String getUserDisplayableConfig() {
        return numberRangeFilter.getUserDisplayableConfig()
    }

    override     public ObjectNode getJsonConfig() {
        return numberRangeFilter.getJsonConfig()
    }

    override     public Unit setJsonConfig(final ObjectNode config) {
        numberRangeFilter.setJsonConfig(config)
    }
}
