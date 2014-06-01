package cgeo.geocaching.search;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringUtils;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class SuggestionProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(final Uri arg0) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
        final String searchTerm = uri.getLastPathSegment();
        // can be empty when deleting the query
        if (StringUtils.equals(searchTerm, SearchManager.SUGGEST_URI_PATH_QUERY)) {
            return getLastOpenedCaches();
        }
        return getSuggestions(searchTerm);
    }

    private static Cursor getLastOpenedCaches() {
        final SearchSuggestionCursor resultCursor = new SearchSuggestionCursor();
        for (final Geocache geocache : DataStore.getLastOpenedCaches()) {
            resultCursor.addCache(geocache.getGeocode(), geocache.getName());
        }
        return resultCursor;
    }

    private static Cursor getSuggestions(final String searchTerm) {
        return DataStore.findSuggestions(searchTerm);
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
