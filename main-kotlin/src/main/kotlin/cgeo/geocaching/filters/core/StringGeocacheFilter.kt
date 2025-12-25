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

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.fasterxml.jackson.databind.node.ObjectNode


abstract class StringGeocacheFilter : BaseGeocacheFilter() {

    private val stringFilter: StringFilter = StringFilter()

    protected abstract String getValue(Geocache cache)

    protected String getSqlColumnName() {
        return null
    }

    public StringFilter getStringFilter() {
        return stringFilter
    }

    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        val gcValue: String = getValue(cache)
        if (gcValue == null) {
            return null
        }
        return stringFilter.matches(gcValue)
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        stringFilter.setConfig(config.get(null))
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.put(null, stringFilter.getConfig())
        return config
    }

    override     public ObjectNode getJsonConfig() {
       return stringFilter.getJsonConfig()
    }

    override     public Unit setJsonConfig(final ObjectNode config) {
        stringFilter.setJsonConfig(config)
    }

    override     public Boolean isFiltering() {
        return stringFilter.isFilled()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        val colName: String = getSqlColumnName()
        if (colName != null) {
            stringFilter.addToSql(sqlBuilder, sqlBuilder.getMainTableId() + "." + colName)
        } else {
            sqlBuilder.addWhereTrue()
        }
    }

    override     protected String getUserDisplayableConfig() {
        return stringFilter.getUserDisplayableConfig()
    }

}
