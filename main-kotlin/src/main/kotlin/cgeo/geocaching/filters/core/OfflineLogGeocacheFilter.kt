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
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder


class OfflineLogGeocacheFilter : StringGeocacheFilter() {


    override     protected String getValue(final Geocache cache) {
        if (!getStringFilter().isFilled()) {
            return ""
        }
        return cache.hasLogOffline() ? cache.getOfflineLog().log : ""
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        val logTableId: String = sqlBuilder.getNewTableId()
        getStringFilter().addToSqlForSubquery(sqlBuilder,
                "SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableLogsOffline + " " + logTableId + " WHERE " + logTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode,
                true, logTableId + "." + DataStore.dbFieldLogsOffline_log)
    }
}
