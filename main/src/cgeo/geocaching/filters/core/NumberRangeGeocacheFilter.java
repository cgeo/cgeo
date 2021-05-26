package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.expressions.ExpressionConfig;
import cgeo.geocaching.utils.functions.Func1;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


public abstract class NumberRangeGeocacheFilter<T extends Number & Comparable<T>> extends BaseGeocacheFilter {

    private T minRangeValue;
    private T maxRangeValue;

    private final Func1<String, T> numberParser;

    public NumberRangeGeocacheFilter(final Func1<String, T> numberParser) {
        this.numberParser = numberParser;
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
        if (minRangeValue != null && minRangeValue.compareTo(value) > 0) {
            return false;
        }
        return maxRangeValue == null || maxRangeValue.compareTo(value) >= 0;
    }

    public T getMinRangeValue() {
        return minRangeValue;
    }

    public void setMinMaxRange(final T min, final T max) {
        this.minRangeValue = min;
        this.maxRangeValue = max;
    }

    public T getMaxRangeValue() {
        return maxRangeValue;
    }


    public Collection<T> getValuesInRange(final T[] values) {
        final Set<T> set = new HashSet<>();
        for (T v : values) {
            if (isInRange(v)) {
                set.add(v);
            }
        }
        return set;
    }

    public void setRangeFromValues(final Collection<T> values) {
        T min = null;
        T max = null;
        for (T v : values) {
            if (v == null) {
                continue;
            }
            if (min == null || min.compareTo(v) >= 0) {
                min = v;
            }
            if (max == null || max.compareTo(v) <= 0) {
                max = v;
            }
        }
        setMinMaxRange(min, max);
    }


    private T parseString(final String text) {
        if (StringUtils.isBlank(text) || "-".equals(text)) {
            return null;
        }
        try {
            return this.numberParser.call(text);
        } catch (Exception e) {
            Log.w("Problem parsing '" + text + "' as a number", e);
            return null;
        }
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        minRangeValue = config.getDefaultList().size() > 0 ? parseString(config.getDefaultList().get(0)) : null;
        maxRangeValue = config.getDefaultList().size() > 1 ? parseString(config.getDefaultList().get(1)) : null;
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig result = new ExpressionConfig();
        result.addToDefaultList(
            minRangeValue == null ? "-" : String.valueOf(minRangeValue),
            maxRangeValue == null ? "-" : String.valueOf(maxRangeValue)
        );
        return result;
    }


    @Override
    public boolean isFiltering() {
        return minRangeValue != null || maxRangeValue != null;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        addRangeToSqlBuilder(sqlBuilder, getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName());
    }

    protected void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression) {
        addRangeToSqlBuilder(sqlBuilder, valueExpression, null);
    }

    protected void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression, final Func1<T, T> valueConverter) {
        if (valueExpression != null && (minRangeValue != null || maxRangeValue != null)) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (minRangeValue != null) {
                sqlBuilder.addWhere(valueExpression + " >= " + (valueConverter == null ? minRangeValue : valueConverter.call(minRangeValue)));
            }
            if (maxRangeValue != null) {
                sqlBuilder.addWhere(valueExpression + " <= " + (valueConverter == null ? maxRangeValue : valueConverter.call(maxRangeValue)));
            }
            sqlBuilder.closeWhere();
        } else {
            sqlBuilder.addWhereAlwaysInclude();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        return (minRangeValue == null ? "*" : minRangeValue) + "-" + (maxRangeValue == null ? "*" : maxRangeValue);
    }

}
