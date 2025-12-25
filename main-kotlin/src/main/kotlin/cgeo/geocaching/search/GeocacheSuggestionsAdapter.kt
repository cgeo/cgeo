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


import cgeo.geocaching.R
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.GeoItemSelectorUtils

import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.TextView

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils

class GeocacheSuggestionsAdapter : BaseSuggestionsAdapter() {

    public GeocacheSuggestionsAdapter(final Context context) {
        //initialize with empty cursor to reduce Long startup time problem (see #11227)
        super(context, GeocacheSearchSuggestionCursor(), 0)
    }

    override     public Unit bindView(final View view, final Context context, final Cursor cursor) {
        val cache: Geocache = DataStore.loadCache(cursor.getString(2), LoadFlags.LOAD_CACHE_OR_DB)

        if (cache != null) {
            GeoItemSelectorUtils.createGeocacheItemView(context, cache, view)
        } else {
            val tv: TextView = view.findViewById(R.id.text)
            tv.setText(cursor.getString(1))
            tv.setCompoundDrawablesWithIntrinsicBounds(cursor.getInt(5), 0, 0, 0)
            ((TextView) view.findViewById(R.id.info)).setText(cursor.getString(2))
        }
        view.setBackgroundColor(context.getResources().getColor(R.color.colorBackgroundDialog))
    }

    override     protected Cursor query(final String searchTerm) {
        if (StringUtils.isBlank(searchTerm)) {
            return getLastOpenedCaches()
        }
        return DataStore.findSuggestions(searchTerm.trim())
    }

    private static Cursor getLastOpenedCaches() {
        val resultCursor: GeocacheSearchSuggestionCursor = GeocacheSearchSuggestionCursor()
        for (final Geocache geocache : DataStore.getLastOpenedCaches()) {
            resultCursor.addCache(geocache.getGeocode(), StringUtils.defaultIfBlank(geocache.getName(), ""), geocache.getType().id)
        }
        return resultCursor
    }
}
