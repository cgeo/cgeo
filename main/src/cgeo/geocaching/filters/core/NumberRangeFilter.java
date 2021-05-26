package cgeo.geocaching.filters.core;

import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


public class NumberRangeFilter<T extends Number & Comparable<T>> {

    private T minRangeValue;
    private T maxRangeValue;

    private final Func1<String, T> numberParser;

    public NumberRangeFilter(final Func1<String, T> numberParser) {
        this.numberParser = numberParser;
    }

    public boolean isInRange(final T value) {
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


    public void setConfig(final List<String> config) {
        if (config == null || config.size() < 2) {
            return;
        }

        minRangeValue = parseString(config.get(0));
        maxRangeValue = parseString(config.get(1));
    }

    public List<String> getConfig() {
        return new ArrayList<>(Arrays.asList(minRangeValue == null ? "-" : String.valueOf(minRangeValue), maxRangeValue == null ? "-" : String.valueOf(maxRangeValue)));
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

    public boolean isFilled() {
        return minRangeValue != null || maxRangeValue != null;
    }

    public void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression) {
        addRangeToSqlBuilder(sqlBuilder, valueExpression, null);
    }

    public void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression, final Func1<T, T> valueConverter) {
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
            sqlBuilder.addWhereTrue();
        }
    }

    protected String getUserDisplayableConfig() {
        return (minRangeValue == null ? "*" : minRangeValue) + "-" + (maxRangeValue == null ? "*" : maxRangeValue);
    }

}
