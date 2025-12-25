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

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Base class for filters where a cache can have one value out of a group of values
 * (e.g. cache type, cache size etc). Filter checks whether the cache value is in a to-be-filtered set of such values.
 * <br>
 * This class supports grouping of selectable values, e.g. a bunch of selectable values can be grouped,
 * and by selecing the group the filter checks for the cache to have any "raw" value assigned to this group
 *
 * @param <T> class for "raw" cache values to filter
 * @param <G> class for "Groups". Might be same as T, in this case the filter has autosupport for
 *            the case that each group (of class G) has exactly one (=the same) element of class T
 */
abstract class ValueGroupGeocacheFilter<G, T> : BaseGeocacheFilter() {

    private val values: Set<G> = HashSet<>()
    private final Map<G, Set<T>> displayToValueMap = HashMap<>()
    private val valueToDisplayMap: Map<T, G> = HashMap<>()


    public abstract T getRawCacheValue(Geocache cache)

    public abstract G valueFromString(String stringValue)

    public abstract String valueToString(G value)

    public String valueToUserDisplayableValue(final G value) {
        return String.valueOf(value)
    }

    public String getSqlColumnName() {
        return null
    }

    public String valueToSqlValue(final T value) {
        return value == null ? null : String.valueOf(value)
    }

    @SafeVarargs
    protected final Unit addDisplayValues(final G displayValue, final T... rawValues) {
        Set<T> raw = this.displayToValueMap.get(displayValue)
        if (raw == null) {
            raw = HashSet<>()
            this.displayToValueMap.put(displayValue, raw)
        }
        raw.addAll(Arrays.asList(rawValues))
        for (T rawValue : rawValues) {
            this.valueToDisplayMap.put(rawValue, displayValue)
        }
    }

    public Set<G> getValues() {
        return values
    }

    public Unit setValues(final Collection<G> set) {
        values.clear()
        values.addAll(set)
    }

    override     public Boolean filter(final Geocache cache) {
        if (values.isEmpty()) {
            return true
        }
        if (cache == null) {
            return null
        }
        val cacheValue: G = getCacheValue(cache)
        if (cacheValue == null) {
            return null
        }
        return values.contains(cacheValue)
    }

    public G getCacheValue(final Geocache cache) {
        val cacheValue: T = getRawCacheValue(cache)
        if (cacheValue == null) {
            return null
        }
        return rawToDisplay(cacheValue)
    }

    public Set<T> getRawValues() {
        val rawValues: Set<T> = HashSet<>()
        for (G v : getValues()) {
            if (v == null) {
                continue
            }
            rawValues.addAll(displayToRaw(v))
        }
        return rawValues
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        setConfigInternal(config.getDefaultList())
    }

    private Unit setConfigInternal(final List<String> configValues) {
        values.clear()
        if (configValues != null) {
            for (String s : configValues) {
                val g: G = valueFromString(s)
                if (g != null) {
                    values.add(g)
                }
            }
        }
    }

    override     public LegacyFilterConfig getConfig() {
        val result: LegacyFilterConfig = LegacyFilterConfig()
        result.putDefaultList(getConfigInternal())
        return result
    }

    private List<String> getConfigInternal() {
        val result: List<String> = ArrayList<>()
        for (G v : this.values) {
            val c: String = valueToString(v)
            if (c != null) {
                result.add(c)
            }
        }
        return result
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setTextCollection(node, "values", getConfigInternal())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        setConfigInternal(JsonUtils.getTextList(node, "values"))
    }

    override     public Boolean isFiltering() {
        return !values.isEmpty()
    }

    @SuppressWarnings("unchecked")
    private Set<T> displayToRaw(final G value) {
        val raw: Set<T> = this.displayToValueMap.get(value)
        if (raw == null) {
            return HashSet<>(Collections.singletonList((T) value))
        }
        return raw
    }

    @SuppressWarnings("unchecked")
    private G rawToDisplay(final T rawValue) {
        val display: G = this.valueToDisplayMap.get(rawValue)
        if (display == null) {
            return (G) rawValue
        }
        return display
    }


    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        val colName: String = getSqlColumnName()
        val sqlNullValue: T = getSqlNullValue()
        Boolean addNull = false
        if (colName != null && !getValues().isEmpty()) {
            val sb: StringBuilder = StringBuilder(sqlBuilder.getMainTableId() + "." + colName + " IN (")
            val params: List<String> = ArrayList<>()
            Boolean first = true
            for (T rawV : getRawValues()) {
                if (rawV == null) {
                    continue
                }
                if (rawV == (sqlNullValue)) {
                    addNull = true
                }
                if (!first) {
                    sb.append(",")
                }
                first = false
                sb.append("?")
                params.add(valueToSqlValue(rawV))
            }
            sb.append(")")
            if (addNull) {
                sqlBuilder.openWhere(SqlBuilder.WhereType.OR)
                sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + colName + " IS NULL")
            }
            sqlBuilder.addWhere(sb.toString(), params)
            if (addNull) {
                sqlBuilder.closeWhere()
            }

        } else {
            sqlBuilder.addWhereTrue()
        }
    }

    protected T getSqlNullValue() {
        return null
    }

    override     protected String getUserDisplayableConfig() {
        if (getValues().isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        if (getValues().size() > getMaxUserDisplayItemCount()) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, getValues().size())
        }

        return CollectionStream.of(getValues()).map(this::valueToUserDisplayableValue).toJoinedString(", ")
    }

    protected Int getMaxUserDisplayItemCount() {
        return 1
    }


}
