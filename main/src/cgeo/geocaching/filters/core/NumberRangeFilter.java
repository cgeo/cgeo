package cgeo.geocaching.filters.core;

import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


public class NumberRangeFilter<T extends Number & Comparable<T>> {

    private T minRangeValue;
    private T maxRangeValue;

    private T specialNumber;
    private Boolean includeSpecialNumber;

    private final Func1<String, T> numberParser;

    public NumberRangeFilter(final Func1<String, T> numberParser) {
        this.numberParser = numberParser;
    }

    public boolean isInRange(final T value) {
        if (includeSpecialNumber != null && specialNumber != null && isEqualValue(value, specialNumber)) {
            return includeSpecialNumber;
        }

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

    public void setSpecialNumber(final T specialNumber) {
        this.specialNumber = specialNumber;
    }

    public Boolean getIncludeSpecialNumber() {
        return includeSpecialNumber;
    }

    public void setIncludeSpecialNumber(final Boolean includeSpecialNumber) {
        this.includeSpecialNumber = includeSpecialNumber;
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

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity"})
    public void setRangeFromValues(final Collection<T> values, final T minUnlimitedValue, final T maxUnlimitedValue) {
        T min = null;
        T max = null;
        boolean foundMinUnlimited = false;
        boolean foundMaxUnlimited = false;
        for (T v : values) {
            if (v == null) {
                continue;
            }
            if (isEqualValue(v, minUnlimitedValue)) {
                foundMinUnlimited = true;
            }
            if (isEqualValue(v, maxUnlimitedValue)) {
                foundMaxUnlimited = true;
            }
            if (min == null || min.compareTo(v) >= 0) {
                min = v;
            }
            if (max == null || max.compareTo(v) <= 0) {
                max = v;
            }
        }

        if (isEqualValue(min, max)) {
            foundMinUnlimited = false;
            foundMaxUnlimited = false;
        }

        setMinMaxRange(foundMinUnlimited ? null : min, foundMaxUnlimited ? null : max);
    }


    public void setConfig(final List<String> config) {
        if (config == null || config.size() < 2) {
            return;
        }

        minRangeValue = parseString(config.get(0));
        maxRangeValue = parseString(config.get(1));
        specialNumber = config.size() >= 3 ? parseString(config.get(2)) : null;
        includeSpecialNumber = config.size() >= 4 ? Boolean.valueOf(config.get(3)) : null;
    }

    public List<String> getConfig() {
        final List<String> config = new ArrayList<>(Arrays.asList(
                minRangeValue == null ? "-" : String.valueOf(minRangeValue),
                maxRangeValue == null ? "-" : String.valueOf(maxRangeValue)));
        if (specialNumber != null && includeSpecialNumber != null) {
            config.add(String.valueOf(specialNumber));
            config.add(Boolean.toString(includeSpecialNumber));
        }
        return config;
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
        return minRangeValue != null || maxRangeValue != null || (specialNumber != null && includeSpecialNumber != null);
    }

    public void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression) {
        addRangeToSqlBuilder(sqlBuilder, valueExpression, null);
    }

    public void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression, final Func1<T, T> valueConverter) {
        final boolean hasSpecial = specialNumber != null && includeSpecialNumber != null;
        final boolean hasMinMax = minRangeValue != null || maxRangeValue != null;

        if (valueExpression == null || (!hasSpecial && !hasMinMax)) {
            sqlBuilder.addWhereAlwaysInclude();
        } else {
            if (hasSpecial) {
                sqlBuilder.openWhere(includeSpecialNumber ? SqlBuilder.WhereType.OR : SqlBuilder.WhereType.AND);
                final T sn = valueConverter == null ? specialNumber : valueConverter.call(specialNumber);
                if (includeSpecialNumber) {
                    sqlBuilder.addWhere(valueExpression + " = " + sn);
                } else {
                    sqlBuilder.addWhere(valueExpression + " <> " + sn);
                }
            }
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            if (minRangeValue != null) {
                sqlBuilder.addWhere(valueExpression + " >= " + (valueConverter == null ? minRangeValue : valueConverter.call(minRangeValue)));
            }
            if (maxRangeValue != null) {
                sqlBuilder.addWhere(valueExpression + " <= " + (valueConverter == null ? maxRangeValue : valueConverter.call(maxRangeValue)));
            }
            if (minRangeValue == null && maxRangeValue == null) {
                sqlBuilder.addWhereTrue();
            }
            sqlBuilder.closeWhere();

            if (hasSpecial) {
                sqlBuilder.closeWhere();
            }

        }
    }

    protected String getUserDisplayableConfig() {
        final T minValue = getMinRangeValue();
        final T maxValue = getMaxRangeValue();
        final String minValueString = minValue != null ? minValue.toString() : null;
        final String maxValueString = maxValue != null ? maxValue.toString() : null;
        return UserDisplayableStringUtils.getUserDisplayableConfig(minValueString, maxValueString);
    }

    private boolean isEqualValue(final T v1, final T v2) {
        if (Objects.equals(v1, v2)) {
            return true;
        }
        return v1 != null && v2 != null && Math.abs(v1.doubleValue() - v2.doubleValue()) < 0.00000001d;
    }

}
