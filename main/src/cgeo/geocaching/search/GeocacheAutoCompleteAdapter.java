package cgeo.geocaching.search;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GeocacheAutoCompleteAdapter extends AutoCompleteAdapter {
    private final Context context;

    public GeocacheAutoCompleteAdapter(final Context context, final Func1<String, String[]> geocodeSuggestionFunction) {
        super(context, R.layout.cacheslist_item_select, geocodeSuggestionFunction);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
        final String geocode = getItem(position);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        return GeoItemSelectorUtils.createGeocacheItemView(context, cache,
                GeoItemSelectorUtils.getOrCreateView(context, convertView, parent));
    }


}
