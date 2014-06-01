package cgeo.geocaching.search;

import cgeo.geocaching.Intents;
import cgeo.geocaching.enumerations.CacheType;

import org.eclipse.jdt.annotation.NonNull;

import android.app.SearchManager;
import android.database.MatrixCursor;
import android.provider.BaseColumns;

/**
 * Fixed fields cursor holding the necessary data for the search provider of the global search bar.
 *
 */
public class SearchSuggestionCursor extends MatrixCursor {

    /**
     * id of the row for callbacks after selection
     */
    private int rowId = 0;

    public SearchSuggestionCursor() {
        super(new String[] {
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                SearchManager.SUGGEST_COLUMN_QUERY,
                SearchManager.SUGGEST_COLUMN_ICON_1 });
    }

    public void addCache(@NonNull final String geocode, @NonNull final String name, final String type) {
        final int icon = CacheType.getById(type).markerId;
        addRow(new String[] {
                String.valueOf(rowId),
                name,
                geocode,
                Intents.ACTION_GEOCACHE,
                geocode,
                String.valueOf(icon)
        });
        rowId++;
    }

}
