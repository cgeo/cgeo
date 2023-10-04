package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.config.LegacyFilterConfig;
import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class NumberRangeGeocacheFilter<T extends Number & Comparable<T>> extends BaseGeocacheFilter {

    private final NumberRangeFilter<T> numberRangeFilter;

    public NumberRangeGeocacheFilter(final Func1<String, T> numberParser, final Func1<Float, T> numberConverter) {
        numberRangeFilter = new NumberRangeFilter<>(numberParser, numberConverter);
    }

    public void setSpecialNumber(final T specialNumber, final Boolean include) {
        numberRangeFilter.setSpecialNumber(specialNumber);
        numberRangeFilter.setIncludeSpecialNumber(include);
    }

    public Boolean getIncludeSpecialNumber() {
        return numberRangeFilter.getIncludeSpecialNumber();
    }

    protected abstract T getValue(Geocache cache);

    protected String getSqlColumnName() {
        return null;
    }

    protected String getSqlColumnExpression(final SqlBuilder sqlBuilder) {
        return getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName();
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        final T gcValue = getValue(cache);
        if (gcValue == null) {
            return null;
        }
        return isInRange(gcValue);
    }

    private boolean isInRange(final T value) {
        return numberRangeFilter.isInRange(value);
    }

    public T getMinRangeValue() {
        return numberRangeFilter.getMinRangeValue();
    }

    public void setMinMaxRange(final T min, final T max) {
        numberRangeFilter.setMinMaxRange(min, max);
    }

    public <V extends Number & Comparable<V>> void setMinMaxRange(@NonNull final V leftValue, @NonNull final V rightValue,
                                                                  @NonNull final V minValue, @NonNull final V maxValue,
                                                                  @NonNull final Func1<V, T> valueConverter) {
        final boolean foundMinUnlimited = leftValue.compareTo(minValue) < 0; // leftValue < minValue
        final boolean foundMaxUnlimited = rightValue.compareTo(maxValue) > 0; // rightValue > maxValue

        setMinMaxRange(foundMinUnlimited ? null : valueConverter.call(leftValue),
                foundMaxUnlimited ? null : valueConverter.call(rightValue));
    }


    public T getMaxRangeValue() {
        return numberRangeFilter.getMaxRangeValue();
    }


    public Collection<T> getValuesInRange(final T[] values) {
        return numberRangeFilter.getValuesInRange(values);
    }

    public void setRangeFromValues(final Collection<T> values) {
        setRangeFromValues(values, null, null);
    }

    public void setRangeFromValues(final Collection<T> values, final T minUnlimitedValue, final T maxUnlimitedValue) {

        numberRangeFilter.setRangeFromValues(values, minUnlimitedValue, maxUnlimitedValue);
    }

    @Override
    public void setConfig(final LegacyFilterConfig config) {
        numberRangeFilter.setConfig(config.getDefaultList());
    }

    @Override
    public LegacyFilterConfig getConfig() {
        final LegacyFilterConfig config = new LegacyFilterConfig();
        config.putDefaultList(numberRangeFilter.getConfig());
        return config;
    }


    @Override
    public boolean isFiltering() {
        return numberRangeFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        addRangeToSqlBuilder(sqlBuilder, getSqlColumnExpression(sqlBuilder));
    }

    protected void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression) {
        addRangeToSqlBuilder(sqlBuilder, valueExpression, null);
    }

    protected void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression, final Func1<T, T> valueConverter) {
        numberRangeFilter.addRangeToSqlBuilder(sqlBuilder, valueExpression, valueConverter);
    }

    @Override
    protected String getUserDisplayableConfig() {
        return numberRangeFilter.getUserDisplayableConfig();
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        return numberRangeFilter.getJsonConfig();
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode config) {
        numberRangeFilter.setJsonConfig(config);
    }
}
