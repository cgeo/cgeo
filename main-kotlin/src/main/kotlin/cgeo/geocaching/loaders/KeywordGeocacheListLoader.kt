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

package cgeo.geocaching.loaders

import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.NameGeocacheFilter
import cgeo.geocaching.sorting.GeocacheSort

import android.app.Activity

import androidx.annotation.NonNull

class KeywordGeocacheListLoader : LiveFilterGeocacheListLoader() {

    public final String keyword

    public KeywordGeocacheListLoader(final Activity activity, final GeocacheSort sort, final String keyword) {
        super(activity, sort)
        this.keyword = keyword
    }

    override     public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.NAME
    }

    override     public IGeocacheFilter getAdditionalFilterParameter() {
        val nameFilter: NameGeocacheFilter = GeocacheFilterType.NAME.create()
        nameFilter.getStringFilter().setTextValue(keyword)
        return nameFilter
    }

}
