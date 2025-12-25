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

import cgeo.geocaching.ui.GeoItemSelectorUtils

import android.content.Context
import android.database.Cursor
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.cursoradapter.widget.CursorAdapter

abstract class BaseSuggestionsAdapter : CursorAdapter() {
    protected String searchTerm
    protected Context context

    public BaseSuggestionsAdapter(final Context context, final Cursor c, final Int flags) {
        super(context, c, flags)
        this.context = context
    }

    override     public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        return GeoItemSelectorUtils.getOrCreateView(context, null, parent)
    }

    public Unit changeQuery(final String searchTerm) {
        this.searchTerm = searchTerm
        changeCursor(query(searchTerm))
        //the following line would get DB cursor asynchronous to GUI thread. Not used currently (see discussion in #11227)
        //AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> query(searchTerm), this::changeCursor)
    }

    protected abstract Cursor query(String searchTerm)
}
