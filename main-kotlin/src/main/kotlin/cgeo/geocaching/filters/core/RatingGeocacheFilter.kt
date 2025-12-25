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

class RatingGeocacheFilter : NumberRangeGeocacheFilter()<Float> {

    public RatingGeocacheFilter() {
        super(Float::valueOf, f -> f)
    }

    override     public Float getValue(final Geocache cache) {
        return cache.getRating()
    }

    override     protected String getSqlColumnName() {
        return "rating"
    }

}
