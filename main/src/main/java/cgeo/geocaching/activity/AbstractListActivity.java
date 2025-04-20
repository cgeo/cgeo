package cgeo.geocaching.activity;

import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract class AbstractListActivity extends AbstractNavigationBarActivity {
    private ListView mListView;

    protected ListView getListView() {
        if (mListView == null) {
            mListView = findViewById(android.R.id.list);
        }
        return mListView;
    }

    protected void setListAdapter(final ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    protected ListAdapter getListAdapter() {
        final ListAdapter adapter = getListView().getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            return ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        }
        return adapter;
    }
}
