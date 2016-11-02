package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;
import org.apache.commons.collections4.CollectionUtils;

public class PocketQueryListActivity extends AbstractActionBarActivity {

    @NonNull private final List<PocketQuery> pocketQueries = new ArrayList<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.pocketquery_activity);

        final PocketQueryListAdapter adapter = new PocketQueryListAdapter(pocketQueries);
        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.pocketquery_list, true, true);
        view.setAdapter(adapter);

        final ProgressDialog waitDialog = ProgressDialog.show(this, getString(R.string.search_pocket_title), getString(R.string.search_pocket_loading), true, true);
        waitDialog.setCancelable(true);
        loadInBackground(adapter, waitDialog);
    }

    private void loadInBackground(final PocketQueryListAdapter adapter, final ProgressDialog waitDialog) {
        AndroidRxUtils.bindActivity(this, GCParser.searchPocketQueryListObservable).subscribe(new Consumer<List<PocketQuery>>() {
            @Override
            public void accept(final List<PocketQuery> pocketQueryList) {
                waitDialog.dismiss();
                if (CollectionUtils.isEmpty(pocketQueryList)) {
                    ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.warn_no_pocket_query_found));
                    finish();
                }
                pocketQueries.addAll(pocketQueryList);
                adapter.notifyItemRangeInserted(0, pocketQueryList.size());
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(final Throwable e) {
                ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.err_read_pocket_query_list));
                finish();
            }
        });
    }

}
