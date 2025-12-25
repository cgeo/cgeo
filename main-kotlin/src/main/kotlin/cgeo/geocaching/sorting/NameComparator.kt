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

package cgeo.geocaching.sorting

import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.TextUtils

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils

/**
 * sorts caches by name
 */
class NameComparator : AbstractCacheComparator() {

    public static val INSTANCE: NameComparator = NameComparator()

    override     protected Boolean canCompare(final Geocache cache) {
        return StringUtils.isNotBlank(cache.getName())
    }

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        return TextUtils.COLLATOR.compare(cache1.getNameForSorting(), cache2.getNameForSorting())
    }

    override     public String getSortableSection(final Geocache cache) {
        return StringUtils.upperCase(StringUtils.substring(cache.getNameForSorting(), 0, 2))
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder("LOWER(" + sql.getMainTableId() + ".name)", sortDesc)
    }
}
