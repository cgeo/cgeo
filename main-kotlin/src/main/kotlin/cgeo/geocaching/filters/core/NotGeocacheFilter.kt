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
import cgeo.geocaching.utils.LocalizationUtils

class NotGeocacheFilter : AndGeocacheFilter() {

    override     public String getId() {
        return "NOT"
    }

    override     public Boolean filter(final Geocache cache) {
        val superResult: Boolean = super.filter(cache)
        return superResult == null ? null : !superResult
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        sqlBuilder.openWhere(SqlBuilder.WhereType.NOT)
        super.addToSql(sqlBuilder)
        sqlBuilder.closeWhere()
    }

    override     public String toUserDisplayableString(final Int level) {
        return LocalizationUtils.getString(R.string.cache_filter_userdisplay_not) + "[" + super.toUserDisplayableString(level) + "]"
    }
}
