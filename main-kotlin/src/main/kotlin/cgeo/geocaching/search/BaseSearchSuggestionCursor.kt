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

import android.app.SearchManager
import android.database.MatrixCursor
import android.provider.BaseColumns

/**
 * Fixed fields cursor holding the necessary data for the search provider of the global search bar.
 */
class BaseSearchSuggestionCursor : MatrixCursor() {

    /**
     * id of the row for callbacks after selection
     */
    protected var rowId: Int = 0

    public BaseSearchSuggestionCursor() {
        super(String[]{
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                SearchManager.SUGGEST_COLUMN_QUERY,
                SearchManager.SUGGEST_COLUMN_ICON_1})
    }
}
