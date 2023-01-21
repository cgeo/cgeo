package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for filters where a cache can have one value out of a group of values
 * (e.g. cache type, cache size etc). Filter checks whether the cache value is in a to-be-filtered set of such values.
 *
 * This class supports grouping of selectable values, e.g. a bunch of selectable values can be grouped,
 * and by selecing the group the filter checks for the cache to have any "raw" value assigned to this group
 *
 * @param <T> class for "raw" cache values to filter
 * @param <G> class for "Groups". Might be same as T, in this case the filter has autosupport for
 *            the case that each group (of class G) has exactly one (=the same) element of class T
 */
public abstract class ValueGroupGeocacheFilter<G, T> extends BaseGeocacheFilter {

    private final Set<G> values = new HashSet<>();
    private final Map<G, Set<T>> displayToValueMap = new HashMap<>();
    private final Map<T, G> valueToDisplayMap = new HashMap<>();


    public abstract T getRawCacheValue(Geocache cache);

    public abstract G valueFromString(String stringValue);

    public abstract String valueToString(G value);

    public String valueToUserDisplayableValue(final G value) {
        return String.valueOf(value);
    }

    public String getSqlColumnName() {
        return null;
    }

    public String valueToSqlValue(final T value) {
        return value == null ? null : String.valueOf(value);
    }

    @SafeVarargs
    protected final void addDisplayValues(final G displayValue, final T... rawValues) {
        Set<T> raw = this.displayToValueMap.get(displayValue);
        if (raw == null) {
            raw = new HashSet<>();
            this.displayToValueMap.put(displayValue, raw);
        }
        raw.addAll(Arrays.asList(rawValues));
        for (T rawValue : rawValues) {
            this.valueToDisplayMap.put(rawValue, displayValue);
        }
    }

    public Set<G> getValues() {
        return values;
    }

    public void setValues(final Collection<G> set) {
        values.clear();
        values.addAll(set);
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (values.isEmpty()) {
            return true;
        }
        if (cache == null) {
            return null;
        }
        final G cacheValue = getCacheValue(cache);
        if (cacheValue == null) {
            return null;
        }
        return values.contains(cacheValue);
    }

    public G getCacheValue(final Geocache cache) {
        final T cacheValue = getRawCacheValue(cache);
        if (cacheValue == null) {
            return null;
        }
        return rawToDisplay(cacheValue);
    }

    public Set<T> getRawValues() {
        final Set<T> rawValues = new HashSet<>();
        for (G v : getValues()) {
            if (v == null) {
                continue;
            }
            rawValues.addAll(displayToRaw(v));
        }
        return rawValues;
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        values.clear();
        for (String s : config.getDefaultList()) {
            final G g = valueFromString(s);
            if (g != null) {
                values.add(g);
            }
        }
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig result = new ExpressionConfig();
        for (G v : this.values) {
            final String c = valueToString(v);
            if (c != null) {
                result.addToDefaultList(c);
            }
        }
        return result;

    }

    @Override
    public boolean isFiltering() {
        return !values.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Set<T> displayToRaw(final G value) {
        final Set<T> raw = this.displayToValueMap.get(value);
        if (raw == null) {
            return new HashSet<>(Collections.singletonList((T) value));
        }
        return raw;
    }

    @SuppressWarnings("unchecked")
    private G rawToDisplay(final T rawValue) {
        final G display = this.valueToDisplayMap.get(rawValue);
        if (display == null) {
            return (G) rawValue;
        }
        return display;
    }


    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String colName = getSqlColumnName();
        if (colName != null && !getValues().isEmpty()) {
            final StringBuilder sb = new StringBuilder(sqlBuilder.getMainTableId() + "." + colName + " IN (");
            final List<String> params = new ArrayList<>();
            boolean first = true;
            for (T rawV : getRawValues()) {
                if (rawV == null) {
                    continue;
                }
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("?");
                params.add(valueToSqlValue(rawV));
            }
            sb.append(")");
            sqlBuilder.addWhere(sb.toString(), params);
        } else {
            sqlBuilder.addWhereTrue();
        }
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (getValues().isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (getValues().size() > getMaxUserDisplayItemCount()) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, getValues().size());
        }

        return CollectionStream.of(getValues()).map(this::valueToUserDisplayableValue).toJoinedString(", ");
    }

    protected int getMaxUserDisplayItemCount() {
        return 1;
    }


}
