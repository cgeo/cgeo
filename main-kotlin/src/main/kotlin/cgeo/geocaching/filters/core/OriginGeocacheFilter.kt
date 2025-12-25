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

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.SqlBuilder

class OriginGeocacheFilter : ValueGroupGeocacheFilter()<IConnector, IConnector> {


    override     public IConnector getRawCacheValue(final Geocache cache) {
        return ConnectorFactory.getConnector(cache)
    }

    override     public IConnector valueFromString(final String stringValue) {
        return ConnectorFactory.getConnectorByName(stringValue)
    }

    override     public String valueToString(final IConnector value) {
        return value.getName()
    }

    public String valueToUserDisplayableValue(final IConnector value) {
        return value.getDisplayName()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (!getValues().isEmpty()) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.OR)
            for (IConnector con : getRawValues()) {
                for (String sqlLikeExp : con.getGeocodeSqlLikeExpressions()) {
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".geocode LIKE ?", sqlLikeExp)
                }
            }
            sqlBuilder.closeWhere()
        } else {
            sqlBuilder.addWhereTrue()
        }
    }

    public Boolean allowsCachesOf(final IConnector connector) {
        return !isFiltering() || getValues().contains(connector)
    }

}
