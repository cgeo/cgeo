package cgeo.geocaching.search;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.utils.functions.Func0;
import cgeo.geocaching.utils.functions.Func1;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class GeocacheAutoCompleteAdapter extends SearchAutoCompleteAdapter {
    private final Context context;
    boolean isGeoCodeSearch = false;
    boolean isKeywordSearch = false;

    public GeocacheAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction) {
        super(context, R.layout.cacheslist_item_select, geocodeSuggestionFunction, 0, null);
        this.context = context;
    }

    public GeocacheAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction, final Func0<String[]> historyFunction) {
        super(context, R.layout.cacheslist_item_select, geocodeSuggestionFunction, 0, historyFunction);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
        final String geocode = getItem(position);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);

        // in case of keyword suggestions geocode might be an arbitrary string, in that case show history line
        final View cv = (null == convertView || (null == convertView.findViewById(R.id.info) ^ null == cache)) ? null : convertView;
        if (null == cache) {
            return super.getView(position, cv, parent);
        }

        final View geoView = GeoItemSelectorUtils.createGeocacheItemView(context, cache, GeoItemSelectorUtils.getOrCreateView(context, cv, parent));
        if (isKeywordSearch) {
            setHighLightedText(geoView.findViewById(R.id.text), searchTerm);
        }
        return geoView;
    }

    private static View nullConvertViewIfNeeded(@Nullable final View convertView, final boolean requireInfoView) {
        if (null == convertView || (null == convertView.findViewById(R.id.info) ^ !requireInfoView)) {
            return null;
        }
        return convertView;
    }

    public static String[] getLastOpenedCachesArray() {
        final List<String> results = new ArrayList<>();
        for (final Geocache geocache : DataStore.getLastOpenedCaches()) {
            results.add(geocache.getGeocode());
        }
        return results.toArray(new String[0]);
    }

    public static class GeocodeAutoCompleteAdapter extends GeocacheAutoCompleteAdapter {
        public GeocodeAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction, final Func0<String[]> historyFunction) {
            super(context, geocodeSuggestionFunction, historyFunction);
            isGeoCodeSearch = true;
        }

        /**
         * Usually search starts for 2 letters but for geocodes delay that until at least prefix + 1 letter have been entered
         */
        public boolean queryTriggersSearch(final String query) {
            return StringUtils.length(query) >= 3;
        }
    }

    public static class KeywordAutoCompleteAdapter extends GeocacheAutoCompleteAdapter {
        public KeywordAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction, final Func0<String[]> historyFunction) {
            super(context, geocodeSuggestionFunction, historyFunction);
            isKeywordSearch = true;
        }
    }

}
