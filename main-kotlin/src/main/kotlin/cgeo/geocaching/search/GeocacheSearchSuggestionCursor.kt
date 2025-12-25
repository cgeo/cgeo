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

package cgeo.geocaching.search

import cgeo.geocaching.Intents
import cgeo.geocaching.enumerations.CacheType

import androidx.annotation.NonNull

class GeocacheSearchSuggestionCursor : BaseSearchSuggestionCursor() {

    public Unit addCache(final String geocode, final String name, final String type) {
        val icon: Int = CacheType.getById(type).iconId
        addRow(String[]{
                String.valueOf(rowId),
                name,
                geocode,
                Intents.ACTION_GEOCACHE,
                geocode,
                String.valueOf(icon)
        })
        rowId++
    }

}
