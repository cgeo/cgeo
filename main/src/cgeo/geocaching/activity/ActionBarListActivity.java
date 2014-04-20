package cgeo.geocaching.activity;

import android.support.v7.app.ActionBarActivity;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Compatbility Class until cgeo switches from ListActivities to ListFragments
 */
public class ActionBarListActivity extends ActionBarActivity {

    private ListView mListView;
    protected ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(android.R.id.list);
        }
        return mListView;
    }

    protected void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    protected ListAdapter getListAdapter() {
        ListAdapter adapter = getListView().getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
        }
        return adapter;
    }
}


