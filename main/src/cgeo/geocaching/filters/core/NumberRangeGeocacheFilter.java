package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


public abstract class NumberRangeGeocacheFilter<T extends Number & Comparable> extends BaseGeocacheFilter {

    private T minRangeValue;
    private T maxRangeValue;

    private final Func1<String, T> numberParser;
    private final Func1<Float, T> floatConverter;

    public NumberRangeGeocacheFilter(final Func1<String, T> numberParser, final Func1<Float, T> floatConverter) {
        this.numberParser = numberParser;
        this.floatConverter = floatConverter;
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

    public void setValuesAsFloat(final Float minValue, final Float maxValue) {
        this.minRangeValue = parseFloat(minValue);
        this.maxRangeValue = parseFloat(maxValue);
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
            if (values.contains(v)) {
                if (min == null) {
                    min = v;
                }
                max = v;
            }
        }
        setMinMaxRange(min, max);
    }

    @Override
    public void setConfig(final String[] values) {
        if (values == null || values.length < 2) {
            return;
        }

        minRangeValue = parseString(values[0]);
        maxRangeValue = parseString(values[1]);
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

    private T parseFloat(final Float f) {
        if (f == null) {
            return null;
        }
        try {
            return this.floatConverter.call(f);
        } catch (Exception e) {
            Log.w("Problem convering float '" + f + "' to a number", e);
            return null;
        }
    }

    @Override
    public String[] getConfig() {
        return new String[]{minRangeValue == null ? "-" : String.valueOf(minRangeValue), maxRangeValue == null ? "-" : String.valueOf(maxRangeValue)};
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String colName = getSqlColumnName();
        if (colName != null && (minRangeValue != null || maxRangeValue != null)) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (minRangeValue != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + colName + " >= " + minRangeValue);
            }
            if (maxRangeValue != null) {
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + colName + " <= " + maxRangeValue);
            }
            sqlBuilder.closeWhere();
        } else {
            sqlBuilder.addWhereAlwaysInclude();
        }
    }
}
