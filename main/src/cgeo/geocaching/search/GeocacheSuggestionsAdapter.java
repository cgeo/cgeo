package cgeo.geocaching.search;


import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

public class GeocacheSuggestionsAdapter extends BaseSuggestionsAdapter {

    public GeocacheSuggestionsAdapter(final Context context) {
        //initialize with empty cursor to reduce long startup time problem (see #11227)
        super(context, new GeocacheSearchSuggestionCursor(), 0);
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final Geocache cache = DataStore.loadCache(cursor.getString(2), LoadFlags.LOAD_CACHE_OR_DB);

        if (cache != null) {
            GeoItemSelectorUtils.createGeocacheItemView(context, cache, view);
        } else {
            final TextView tv = view.findViewById(R.id.text);
            tv.setText(cursor.getString(1));
            tv.setCompoundDrawablesWithIntrinsicBounds(cursor.getInt(5), 0, 0, 0);
            ((TextView) view.findViewById(R.id.info)).setText(cursor.getString(2));
        }
    }

    @Override
    protected Cursor query(@NonNull final String searchTerm) {
        if (StringUtils.isBlank(searchTerm)) {
            return getLastOpenedCaches();
        }
        return DataStore.findSuggestions(searchTerm.trim());
    }

    private static Cursor getLastOpenedCaches() {
        final GeocacheSearchSuggestionCursor resultCursor = new GeocacheSearchSuggestionCursor();
        for (final Geocache geocache : DataStore.getLastOpenedCaches()) {
            resultCursor.addCache(geocache.getGeocode(), geocache.getName(), geocache.getType().id);
        }
        return resultCursor;
    }
}
