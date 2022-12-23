package cgeo.geocaching.search;

import android.app.SearchManager;
import android.database.MatrixCursor;
import android.provider.BaseColumns;

/**
 * Fixed fields cursor holding the necessary data for the search provider of the global search bar.
 */
public class BaseSearchSuggestionCursor extends MatrixCursor {

    /**
     * id of the row for callbacks after selection
     */
    protected int rowId = 0;

    public BaseSearchSuggestionCursor() {
        super(new String[]{
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                SearchManager.SUGGEST_COLUMN_QUERY,
                SearchManager.SUGGEST_COLUMN_ICON_1});
    }
}
