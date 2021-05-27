package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.expressions.ExpressionConfig;
import cgeo.geocaching.utils.functions.Func1;

import java.util.Collection;


public abstract class NumberRangeGeocacheFilter<T extends Number & Comparable<T>> extends BaseGeocacheFilter {

    private final NumberRangeFilter<T> numberRangeFilter;

    public NumberRangeGeocacheFilter(final Func1<String, T> numberParser) {
        numberRangeFilter = new NumberRangeFilter<>(numberParser);
    }

    protected abstract T getValue(Geocache cache);

    protected String getSqlColumnName() {
        return null;
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

    public T getMaxRangeValue() {
        return numberRangeFilter.getMaxRangeValue();
    }


    public Collection<T> getValuesInRange(final T[] values) {
        return numberRangeFilter.getValuesInRange(values);
    }

    public void setRangeFromValues(final Collection<T> values) {
        numberRangeFilter.setRangeFromValues(values);
    }

   @Override
    public void setConfig(final ExpressionConfig config) {
        numberRangeFilter.setConfig(config.getDefaultList());
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        config.putDefaultList(numberRangeFilter.getConfig());
        return config;
    }


    @Override
    public boolean isFiltering() {
        return numberRangeFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        addRangeToSqlBuilder(sqlBuilder, getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName());
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

}
