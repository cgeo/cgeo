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
import cgeo.geocaching.models.bettercacher.Tier

class TierGeocacheFilter : ValueGroupGeocacheFilter()<Tier, Tier> {


    override     public Tier getRawCacheValue(final Geocache cache) {
        return cache.getTier() == null ? Tier.NONE : cache.getTier()
    }

    override     public Tier valueFromString(final String stringValue) {
        return Tier.getByName(stringValue)
    }

    override     public String valueToUserDisplayableValue(final Tier value) {
        return value.getI18nText()
    }


    override     public String valueToString(final Tier value) {
        return value.getRaw()
    }

    override     public String getSqlColumnName() {
        return "tier"
    }

    override     public Tier getSqlNullValue() {
        return Tier.NONE
    }

    override     public String valueToSqlValue(final Tier value) {
        return value.getRaw()
    }

}
