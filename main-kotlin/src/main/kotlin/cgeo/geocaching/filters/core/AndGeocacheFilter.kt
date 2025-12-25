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

class AndGeocacheFilter : LogicalGeocacheFilter() {

    override     public String getId() {
        return "AND"
    }

    override     public Boolean filter(final Geocache cache) {
        Boolean isInconclusive = false
        for (IGeocacheFilter child : getChildren()) {
            val childResult: Boolean = child.filter(cache)
            if (childResult == null) {
                isInconclusive = true
            } else if (!childResult) {
                return false
            }
        }
        return isInconclusive ? null : true
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (!getChildren().isEmpty()) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND)
            for (IGeocacheFilter child : getChildren()) {
                child.addToSql(sqlBuilder)
            }
            sqlBuilder.closeWhere()
        }

    }

    override     public String getUserDisplayableType() {
        return ", "
    }


}
