package cgeo.geocaching.search;


import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.CursorAdapter;

import org.apache.commons.lang3.StringUtils;

public class SuggestionsAdapter extends CursorAdapter {

    public SuggestionsAdapter(final Context context) {
        super(context, query(""), 0);
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final Geocache cache = DataStore.loadCache(cursor.getString(2), LoadFlags.LOAD_CACHE_OR_DB);

        if (cache != null) {
            GeoItemSelectorUtils.createGeocacheItemView(context, cache, view);
        } else {
            final TextView tv = (TextView) view.findViewById(R.id.text);
            tv.setText(cursor.getString(1));

            final TextView infoView = (TextView) view.findViewById(R.id.info);
            infoView.setText(cursor.getString(2));

            tv.setCompoundDrawablesWithIntrinsicBounds(cursor.getInt(5), 0, 0, 0);
        }
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        return GeoItemSelectorUtils.getOrCreateView(context, null, parent);
    }

    public void changeQuery(@NonNull final String searchTerm) {
        changeCursor(query(searchTerm));
    }

    private static Cursor query(@NonNull final String searchTerm) {
        if (StringUtils.isBlank(searchTerm)) {
            return getLastOpenedCaches();
        }
        return DataStore.findSuggestions(searchTerm.trim());
    }

    private static Cursor getLastOpenedCaches() {
        final SearchSuggestionCursor resultCursor = new SearchSuggestionCursor();
        for (final Geocache geocache : DataStore.getLastOpenedCaches()) {
            resultCursor.addCache(geocache.getGeocode(), geocache.getName(), geocache.getType().id);
        }
        return resultCursor;
    }
}
