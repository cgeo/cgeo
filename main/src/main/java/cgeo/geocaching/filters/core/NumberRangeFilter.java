package cgeo.geocaching.filters.core;

import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.functions.Func1;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class NumberRangeFilter<T extends Number & Comparable<T>> {

    private T minRangeValue;
    private T maxRangeValue;

    private final Set<T> specialNumberInclude = new HashSet<>();
    private final Set<T> specialNumberExclude = new HashSet<>();

    private boolean includeNull = false;

    private final Func1<String, T> numberParser;
    private final Func1<Float, T> numberConverter;

    public NumberRangeFilter(final Func1<String, T> numberParser, final Func1<Float, T> numberConverter) {
        this.numberParser = numberParser;
        this.numberConverter = numberConverter;
    }

    public boolean isInRange(final T value) {
        if (value == null) {
            return includeNull;
        }
        for (T special : specialNumberInclude) {
            if (isEqualValue(value, special)) {
                return true;
            }
        }
        for (T special : specialNumberExclude) {
            if (isEqualValue(value, special)) {
                return false;
            }
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

    public void setSpecialNumbers(final Collection<T> include, final Collection<T> exclude) {
        this.specialNumberInclude.clear();
        if (include != null) {
            this.specialNumberInclude.addAll(include);
        }
        this.specialNumberExclude.clear();
        if (exclude != null) {
            this.specialNumberExclude.addAll(exclude);
        }
    }

    public Set<T> getSpecialNumberInclude() {
        return specialNumberInclude;
    }

    public Set<T> getSpecialNumberExclude() {
        return specialNumberExclude;
    }

    public void setIncludeNull(final boolean includeNull) {
        this.includeNull = includeNull;
    }

    public boolean getIncludeNull() {
        return includeNull;
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
        setMinMaxRange(foundMinUnlimited ? null : min, foundMaxUnlimited ? null : max);
    }

    public boolean isFilled() {
        return minRangeValue != null || maxRangeValue != null || includeNull || !specialNumberInclude.isEmpty() || !specialNumberExclude.isEmpty();
    }

    public void addRangeToSqlBuilder(final SqlBuilder sqlBuilder, final String valueExpression, final Func1<T, T> valueConverter) {
        final boolean hasSpecial = !specialNumberInclude.isEmpty() || !specialNumberExclude.isEmpty();
        final boolean hasMinMax = minRangeValue != null || maxRangeValue != null;

        if (valueExpression == null || (!hasSpecial && !hasMinMax && !includeNull)) {
            sqlBuilder.addWhereAlwaysInclude();
        } else {
            if (includeNull || !specialNumberInclude.isEmpty()) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
                if (includeNull) {
                    sqlBuilder.addWhere(valueExpression + " IS NULL");
                }
                for (T specialNumber : specialNumberInclude) {
                    final T sn = valueConverter == null ? specialNumber : valueConverter.call(specialNumber);
                    sqlBuilder.addWhere(valueExpression + " = " + sn);
                }
            }
            if (!specialNumberExclude.isEmpty()) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
                for (T specialNumber : specialNumberExclude) {
                    final T sn = valueConverter == null ? specialNumber : valueConverter.call(specialNumber);
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

            if (!specialNumberExclude.isEmpty()) {
                sqlBuilder.closeWhere();
            }
            if (includeNull || !specialNumberInclude.isEmpty()) {
                sqlBuilder.closeWhere();
            }

        }
    }

    protected String getUserDisplayableConfig() {
        return getUserDisplayableConfig(null);
    }

    protected String getUserDisplayableConfig(@Nullable final Function<T, String> converter) {
        final T minValue = getMinRangeValue();
        final T maxValue = getMaxRangeValue();
        return UserDisplayableStringUtils.getUserDisplayableConfig(minValue, maxValue, converter);
    }

    private boolean isEqualValue(final T v1, final T v2) {
        if (Objects.equals(v1, v2)) {
            return true;
        }
        return v1 != null && v2 != null && Math.abs(v1.doubleValue() - v2.doubleValue()) < 0.00000001d;
    }

    public void setJsonConfig(final JsonNode node) {

        if (node != null) {
            minRangeValue = floatToValue(JsonUtils.getFloat(node, "min", null));
            maxRangeValue = floatToValue(JsonUtils.getFloat(node, "max", null));
            includeNull = JsonUtils.getBoolean(node, "includeNull", false);
            final Set<T> specialNumberInclude = getArrayNode(node, "specialInclude");
            final Set<T> specialNumberExclude = getArrayNode(node, "specialExclude");

            //legacy
            final T legacySpecialNumber = floatToValue(JsonUtils.getFloat(node, "special", null));
            final Boolean includeSpecialNumber = JsonUtils.getBoolean(node, "includeSpecial", false);
            if (legacySpecialNumber != null && includeSpecialNumber != null) {
                (includeSpecialNumber ? specialNumberInclude : specialNumberExclude).add(legacySpecialNumber);
            }
            setSpecialNumbers(specialNumberInclude, specialNumberExclude);
        }
    }

    private Set<T> getArrayNode(final JsonNode node, final String key) {
        final Set<T> result = new HashSet<>();
        final JsonNode arrayNode = JsonUtils.get(node, key);
        if (arrayNode instanceof ArrayNode) {
            for (JsonNode specialNumberNode : arrayNode) {
                final Float specialNumberFloat = JsonUtils.toFloat(specialNumberNode, null);
                if (specialNumberFloat != null) {
                    result.add(floatToValue(specialNumberFloat));
                }
            }
        }
        return result;
    }

    private T floatToValue(final Float value) {
        return value == null ? null : this.numberConverter.call(value);
    }


    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setFloat(node, "min", minRangeValue);
        JsonUtils.setFloat(node, "max", maxRangeValue);
        JsonUtils.setBoolean(node, "includeNull", includeNull);
        final ArrayNode specialNumberExcludeNode = JsonUtils.createArrayNode();
        for (T specialNumber : specialNumberExclude) {
            specialNumberExcludeNode.add(JsonUtils.fromFloat(specialNumber));
        }
        JsonUtils.set(node, "specialExclude", specialNumberExcludeNode);
        final ArrayNode specialNumberIncludeNode = JsonUtils.createArrayNode();
        for (T specialNumber : specialNumberInclude) {
            specialNumberIncludeNode.add(JsonUtils.fromFloat(specialNumber));
        }
        JsonUtils.set(node, "specialInclude", specialNumberIncludeNode);
        return node;
    }


}
