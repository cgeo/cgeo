package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

public class PocketQueryListActivity extends AbstractActionBarActivity {

    @NonNull private final List<PocketQuery> pocketQueries = new ArrayList<>();

    private boolean onlyDownloadable = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.pocketquery_activity);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            onlyDownloadable = extras.getBoolean(Intents.EXTRA_PQ_LIST_IMPORT);
        }

        final PocketQueryListAdapter adapter = new PocketQueryListAdapter(this);
        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.pocketquery_list, true, true);
        view.setAdapter(adapter);

        final ProgressDialog waitDialog = ProgressDialog.show(this, getString(R.string.search_pocket_title), getString(R.string.search_pocket_loading), true, true);
        waitDialog.setCancelable(true);
        loadInBackground(adapter, waitDialog);
    }

    private void loadInBackground(final PocketQueryListAdapter adapter, final ProgressDialog waitDialog) {
        AndroidRxUtils.bindActivity(this, GCParser.searchPocketQueryListObservable).subscribe(pocketQueryList -> {
            waitDialog.dismiss();
            if (onlyDownloadable) {
                for (final PocketQuery pq : pocketQueryList) {
                    if (pq.isDownloadable()) {
                        pocketQueries.add(pq);
                    }
                }
            } else {
                pocketQueries.addAll(pocketQueryList);
            }

            if (CollectionUtils.isEmpty(pocketQueryList)) {
                ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.warn_no_pocket_query_found));
                finish();
            }

            adapter.notifyItemRangeInserted(0, pocketQueryList.size());
        }, e -> {
            ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.err_read_pocket_query_list));
            finish();
        });
    }

    public static void startSubActivity(final Activity fromActivity, final int requestCode) {
        final Intent intent = new Intent(fromActivity, PocketQueryListActivity.class);
        intent.putExtra(Intents.EXTRA_PQ_LIST_IMPORT, true);
        fromActivity.startActivityForResult(intent, requestCode);
    }

    public List<PocketQuery> getQueries() {
        return pocketQueries;
    }

    public boolean onlyDownloadable() {
        return onlyDownloadable;
    }

    public void returnResult(final PocketQuery pocketQuery) {
        setResult(RESULT_OK, new Intent()
                .setDataAndType(pocketQuery.getUri(), "application/zip"));
        finish();
    }

}
