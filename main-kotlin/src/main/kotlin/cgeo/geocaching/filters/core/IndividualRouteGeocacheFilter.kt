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
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder

import java.util.ArrayList

class IndividualRouteGeocacheFilter : BooleanGeocacheFilter() {

    override     public Boolean filter(final Geocache cache, final Boolean value) {
        val routeItems: ArrayList<RouteItem> = DataStore.loadIndividualRoute()
        return routeItems.stream().anyMatch(item -> cache.getGeocode() == (item.getGeocode())) == value
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder, final Boolean value) {
        val routeTableId: String = sqlBuilder.getNewTableId()

        sqlBuilder.addWhere((value ? "" : "NOT ") +
                "EXISTS(SELECT " + DataStore.dbFieldRoute_id + " FROM " + DataStore.dbTableRoute + " " + routeTableId + " WHERE " + routeTableId + "." + DataStore.dbFieldRoute_id + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + ")")
    }

}
