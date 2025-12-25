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

import java.util.Date

import com.fasterxml.jackson.databind.node.ObjectNode


abstract class DateRangeGeocacheFilter : BaseGeocacheFilter() {

    private val dateFilter: DateFilter = DateFilter()

    protected abstract Date getDate(Geocache cache)

    protected String getSqlColumnName() {
        return null
    }

    override     public Boolean filter(final Geocache cache) {
        return dateFilter.matches(getDate(cache))
    }

    public Date getMinDate() {
        return dateFilter.getMinDate()
    }

    public DateFilter getDateFilter() {
        return dateFilter
    }

    public Date getMaxDate() {
        return dateFilter.getMaxDate()
    }

    public Unit setMinMaxDate(final Date min, final Date max) {
        this.dateFilter.setMinMaxDate(min, max)
    }

    public Unit setRelativeMinMaxDays(final Int minOffset, final Int maxOffset) {
        this.dateFilter.setRelativeDays(minOffset, maxOffset)
    }

    public Int getDaysSinceMinDate() {
        return dateFilter.getDaysSinceMinDate()
    }

    override     public Unit setConfig(final LegacyFilterConfig config) {
        dateFilter.setConfig(config.get(null))
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.put(null, dateFilter.getConfig())
        return config
    }

    override     public ObjectNode getJsonConfig() {
        return dateFilter.getJsonConfig()
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        dateFilter.setJsonConfig(node)
    }

    override     public Boolean isFiltering() {
        return dateFilter.isFilled()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        addToSql(sqlBuilder, getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName())
    }

    protected Unit addToSql(final SqlBuilder sqlBuilder, final String valueExpression) {
        dateFilter.addToSql(sqlBuilder, valueExpression)
    }

    override     protected String getUserDisplayableConfig() {
        return dateFilter.getUserDisplayableConfig()
    }

}
