package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.PocketQuery;

import org.apache.commons.collections4.CollectionUtils;

import android.app.ProgressDialog;
import android.os.Bundle;

import java.util.List;

import rx.android.app.AppObservable;
import rx.functions.Action1;

public class PocketQueryListActivity extends AbstractListActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.pocketquery_activity);

        final PocketQueryListAdapter adapter = new PocketQueryListAdapter(this);
        setListAdapter(adapter);

        final ProgressDialog waitDialog = ProgressDialog.show(this, getString(R.string.search_pocket_title), getString(R.string.search_pocket_loading), true, true);
        waitDialog.setCancelable(true);
        loadInBackground(adapter, waitDialog);
    }

    private void loadInBackground(final PocketQueryListAdapter adapter, final ProgressDialog waitDialog) {
        AppObservable.bindActivity(this, GCParser.searchPocketQueryListObservable).subscribe(new Action1<List<PocketQuery>>() {
            @Override
            public void call(final List<PocketQuery> pocketQueryList) {
                waitDialog.dismiss();
                if (CollectionUtils.isEmpty(pocketQueryList)) {
                    ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.warn_no_pocket_query_found));
                    finish();
                }
                // adapter.addAll does not exist until API 11. We will notify the adapter ourselves as not to queue
                // a call to notifyDataSetChanged() after every add.
                adapter.setNotifyOnChange(false);
                for (final PocketQuery pocketQuery : pocketQueryList) {
                    adapter.add(pocketQuery);
                }
                adapter.notifyDataSetChanged();
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(final Throwable e) {
                ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.err_read_pocket_query_list));
                finish();
            }
        });
    }

}
