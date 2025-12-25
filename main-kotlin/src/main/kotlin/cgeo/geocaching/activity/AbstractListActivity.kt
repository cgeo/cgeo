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

package cgeo.geocaching.activity

import android.widget.HeaderViewListAdapter
import android.widget.ListAdapter
import android.widget.ListView

abstract class AbstractListActivity : AbstractNavigationBarActivity() {
    private ListView mListView

    protected ListView getListView() {
        if (mListView == null) {
            mListView = findViewById(android.R.id.list)
        }
        return mListView
    }

    protected Unit setListAdapter(final ListAdapter adapter) {
        getListView().setAdapter(adapter)
    }

    protected ListAdapter getListAdapter() {
        val adapter: ListAdapter = getListView().getAdapter()
        if (adapter is HeaderViewListAdapter) {
            return ((HeaderViewListAdapter) adapter).getWrappedAdapter()
        }
        return adapter
    }
}
