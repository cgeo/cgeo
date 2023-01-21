package cgeo.geocaching.search;

import cgeo.geocaching.ui.GeoItemSelectorUtils;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.CursorAdapter;

public abstract class BaseSuggestionsAdapter extends CursorAdapter {
    protected String searchTerm;
    protected Context context;

    public BaseSuggestionsAdapter(final Context context, final Cursor c, final int flags) {
        super(context, c, flags);
        this.context = context;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        return GeoItemSelectorUtils.getOrCreateView(context, null, parent);
    }

    public void changeQuery(@NonNull final String searchTerm) {
        this.searchTerm = searchTerm;
        changeCursor(query(searchTerm));
        //the following line would get new DB cursor asynchronous to GUI thread. Not used currently (see discussion in #11227)
        //AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> query(searchTerm), this::changeCursor);
    }

    protected abstract Cursor query(String searchTerm);
}
