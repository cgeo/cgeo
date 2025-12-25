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
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.functions.Func0
import cgeo.geocaching.utils.functions.Func1

import android.content.Context
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

class GeocacheAutoCompleteAdapter : SearchAutoCompleteAdapter() {
    private final Context context

    public GeocacheAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction, final Action1<String> deleteFunction) {
        super(context, R.layout.cacheslist_item_select, geocodeSuggestionFunction, 0, null, deleteFunction)
        this.context = context
    }

    public GeocacheAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction, final Func0<String[]> historyFunction, final Action1<String> deleteFunction) {
        super(context, R.layout.cacheslist_item_select, geocodeSuggestionFunction, 0, historyFunction, deleteFunction)
        this.context = context
    }

    override     public View getView(final Int position, final View convertView, final ViewGroup parent) {
        val geocode: String = getItem(position)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        if (null != cache) {
            return GeoItemSelectorUtils.createGeocacheItemView(context, cache, GeoItemSelectorUtils.getOrCreateView(context, convertView, parent))
        }
        return superGetView(position, convertView, parent)
    }

    private View superGetView(final Int position, final View convertView, final ViewGroup parent) {
        return super.getView(position, convertView, parent)
    }

    public static String[] getLastOpenedCachesArray() {
        val results: List<String> = ArrayList<>()
        for (final Geocache geocache : DataStore.getLastOpenedCaches()) {
            results.add(geocache.getGeocode())
        }
        return results.toArray(String[0])
    }

    public static class GeocodeAutoCompleteAdapter : GeocacheAutoCompleteAdapter() {
        public GeocodeAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction, final Func0<String[]> historyFunction, final Action1<String> deleteFunction) {
            super(context, geocodeSuggestionFunction, historyFunction, deleteFunction)
        }

        /**
         * Usually search starts for 2 letters but for geocodes delay that until at least prefix + 1 letter have been entered
         */
        public Boolean queryTriggersSearch(final String query) {
            return StringUtils.length(query) >= 3
        }
    }

    public static class KeywordAutoCompleteAdapter : GeocacheAutoCompleteAdapter() {
        public KeywordAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction, final Func0<String[]> historyFunction, final Action1<String> deleteFunction) {
            super(context, geocodeSuggestionFunction, historyFunction, deleteFunction)
        }

        override         public View getView(final Int position, final View convertView, final ViewGroup parent) {
            // Keyword search shows geocache layout for results and single-line layout for history, thus need to invalidate views on updates
            val cv: View = (null == convertView || (null == convertView.findViewById(R.id.info) ^ isShowingResultsFromHistory)) ? null : convertView
            if (isShowingResultsFromHistory) {
                return super.superGetView(position, cv, parent)
            }
            val geoView: View = super.getView(position, cv, parent)
            // Highlight search term in geocache title
            setHighLightedText(geoView.findViewById(R.id.text), searchTerm)
            return geoView
        }
    }

}
