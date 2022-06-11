package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.Intents.EXTRA_PQ_LIST_IMPORT;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

public abstract class AbstractListActivity extends AbstractActionBarActivity {

    @NonNull private final List<GCList> allPocketQueries = new ArrayList<>();
    @NonNull private final List<GCList> pocketQueries = new ArrayList<>();

    private boolean filteredList = false;
    private boolean fixed = false;
    private boolean startDownload = false;
    private SwitchCompat switchCompat = null;

    // values need to be set in derived classes constructor
    @StringRes protected int title;
    @StringRes protected int progressInfo;
    @StringRes protected int errorReadingList;

    abstract boolean getFiltersetting();

    abstract void setFiltersetting(boolean value);

    @WorkerThread
    abstract List<GCList> getList();

    abstract boolean alwaysShow(GCList list);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.gclist_activity);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            startDownload = !extras.getBoolean(EXTRA_PQ_LIST_IMPORT, false);
        } else {
            startDownload = true;
        }
        filteredList = getFiltersetting();

        final AbstractListAdapter adapter = new AbstractListAdapter(this);
        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.gclist, true, true);
        view.setAdapter(adapter);

        if (!fixed) {
            final ActionBar bar = getSupportActionBar();
            if (bar != null) {
                @SuppressLint("InflateParams") final View customView = getLayoutInflater().inflate(R.layout.gclist_actionbar, null);
                ((TextView) customView.findViewById(R.id.title)).setText(title);
                bar.setCustomView(customView);
                bar.setDisplayShowCustomEnabled(true);

                switchCompat = customView.findViewById(R.id.switchAB);
                switchCompat.setVisibility(View.INVISIBLE);
                switchCompat.setChecked(!getFiltersetting());
                switchCompat.setOnCheckedChangeListener((a, b) -> checkSwitchState(adapter));
            }
        }

        final ProgressDialog waitDialog = ProgressDialog.show(this, getString(title), getString(progressInfo), true, true);
        waitDialog.setCancelable(true);
        loadInBackground(adapter, waitDialog);
    }

    private void loadInBackground(final AbstractListAdapter adapter, final ProgressDialog waitDialog) {
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
            final List<GCList> list = getList();
            if (CollectionUtils.isEmpty(list)) {
                ActivityMixin.showToast(AbstractListActivity.this, errorReadingList);
                return; // do not flood the user with multiple toasts
            }
            allPocketQueries.addAll(list);
        }, () -> {
            Collections.sort(allPocketQueries, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()));
            if (CollectionUtils.isEmpty(allPocketQueries)) {
                ActivityMixin.showToast(AbstractListActivity.this, getString(R.string.warn_no_pocket_query_found));
                finish();
            }
            fillAdapter(adapter);
            if (!fixed) {
                switchCompat.setVisibility(View.VISIBLE);
            }
            waitDialog.dismiss();
        });
    }

    private void fillAdapter(final AbstractListAdapter adapter) {
        pocketQueries.clear();
        if (filteredList) {
            for (final GCList pq : allPocketQueries) {
                if (alwaysShow(pq)) {
                    pocketQueries.add(pq);
                }
            }
        } else {
            pocketQueries.addAll(allPocketQueries);
        }
        adapter.notifyDataSetChanged();
    }

    private void checkSwitchState(final AbstractListAdapter adapter) {
        filteredList = !switchCompat.isChecked();
        setFiltersetting(filteredList);
        fillAdapter(adapter);
    }

    public List<GCList> getQueries() {
        return pocketQueries;
    }

    public boolean onlyDownloadable() {
        return filteredList;
    }

    public boolean getStartDownload() {
        return startDownload;
    }

    public void returnResult(final GCList pocketQuery) {
        setResult(RESULT_OK, new Intent()
                .setDataAndType(pocketQuery.getUri(), "application/zip"));
        finish();
    }

}
