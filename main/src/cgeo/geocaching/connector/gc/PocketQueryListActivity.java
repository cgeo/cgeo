package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.TextUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

public class PocketQueryListActivity extends AbstractActionBarActivity {

    @NonNull private final List<PocketQuery> allPocketQueries = new ArrayList<>();
    @NonNull private final List<PocketQuery> pocketQueries = new ArrayList<>();

    private boolean onlyDownloadable = false;
    private boolean fixed = false;
    private boolean startDownload = false;
    private SwitchCompat switchCompat = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.pocketquery_activity);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            startDownload = false;
            onlyDownloadable = extras.getBoolean(Intents.EXTRA_PQ_LIST_IMPORT);
            if (onlyDownloadable) {
                fixed = true;
            }
        } else {
            startDownload = true;
            onlyDownloadable = Settings.getPqShowDownloadableOnly();
        }

        final PocketQueryListAdapter adapter = new PocketQueryListAdapter(this);
        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.pocketquery_list, true, true);
        view.setAdapter(adapter);

        if (!fixed) {
            final ActionBar bar = getSupportActionBar();
            if (bar != null) {
                @SuppressLint("InflateParams") final View customView = getLayoutInflater().inflate(R.layout.pq_actionbar, null);
                bar.setCustomView(customView);
                bar.setDisplayShowCustomEnabled(true);

                switchCompat = customView.findViewById(R.id.switchAB);
                switchCompat.setVisibility(View.INVISIBLE);
                switchCompat.setChecked(!Settings.getPqShowDownloadableOnly());
                switchCompat.setOnCheckedChangeListener((a, b) -> checkSwitchState(adapter));
            }
        }

        final ProgressDialog waitDialog = ProgressDialog.show(this, getString(R.string.menu_lists_pocket_queries), getString(R.string.search_pocket_and_bookmark_loading), true, true);
        waitDialog.setCancelable(true);
        loadInBackground(adapter, waitDialog);
    }

    private void loadInBackground(final PocketQueryListAdapter adapter, final ProgressDialog waitDialog) {
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
            final List<PocketQuery> bookmarkLists = GCParser.searchBookmarkLists();
            final List<PocketQuery> pocketQueries = GCParser.searchPocketQueries();

            if (CollectionUtils.isEmpty(bookmarkLists) && CollectionUtils.isEmpty(pocketQueries)) {
                return; // do not flood the user with multiple toasts
            }

            if (bookmarkLists != null) {
                allPocketQueries.addAll(bookmarkLists);
            } else {
                ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.err_read_bookmark_list));
            }
            if (pocketQueries != null) {
                allPocketQueries.addAll(pocketQueries);
            } else {
                ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.err_read_pocket_query_list));
            }
        }, () -> {
            Collections.sort(allPocketQueries, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()));
            if (CollectionUtils.isEmpty(allPocketQueries)) {
                ActivityMixin.showToast(PocketQueryListActivity.this, getString(R.string.warn_no_pocket_query_found));
                finish();
            }
            fillAdapter(adapter);
            if (!fixed) {
                switchCompat.setVisibility(View.VISIBLE);
            }
            waitDialog.dismiss();
        });
    }

    private void fillAdapter(final PocketQueryListAdapter adapter) {
        pocketQueries.clear();
        if (onlyDownloadable) {
            for (final PocketQuery pq : allPocketQueries) {
                if (pq.isDownloadable()) {
                    pocketQueries.add(pq);
                }
            }
        } else {
            pocketQueries.addAll(allPocketQueries);
        }
        adapter.notifyDataSetChanged();
    }

    private void checkSwitchState(final PocketQueryListAdapter adapter) {
        onlyDownloadable = !switchCompat.isChecked();
        Settings.setPqShowDownloadableOnly(onlyDownloadable);
        fillAdapter(adapter);
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

    public boolean getStartDownload() {
        return startDownload;
    }

    public void returnResult(final PocketQuery pocketQuery) {
        setResult(RESULT_OK, new Intent()
                .setDataAndType(pocketQuery.getUri(), "application/zip"));
        finish();
    }

}
