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

import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.models.Geocache

class SizeGeocacheFilter : ValueGroupGeocacheFilter()<CacheSize, CacheSize> {


    override     public CacheSize getRawCacheValue(final Geocache cache) {
        return CacheSize.UNKNOWN == (cache.getSize()) ? null : cache.getSize()
    }

    override     public CacheSize valueFromString(final String stringValue) {
        return CacheSize.valueOf(stringValue)
    }

    override     public String valueToUserDisplayableValue(final CacheSize value) {
        return value.getShortName()
    }


    override     public String valueToString(final CacheSize value) {
        return value.name()
    }

    override     public String getSqlColumnName() {
        return "size"
    }

    override     public String valueToSqlValue(final CacheSize value) {
        return value.id
    }

    override     protected Int getMaxUserDisplayItemCount() {
        return 3
    }
}
