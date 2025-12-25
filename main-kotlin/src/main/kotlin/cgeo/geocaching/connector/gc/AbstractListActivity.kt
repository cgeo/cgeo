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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.CustomMenuEntryActivity
import cgeo.geocaching.models.GCList
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.Intents.EXTRA_PQ_LIST_IMPORT

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.Collections
import java.util.List

import org.apache.commons.collections4.CollectionUtils

abstract class AbstractListActivity : CustomMenuEntryActivity() {

    private val allGCLists: List<GCList> = ArrayList<>()
    private val visibleGCLists: List<GCList> = ArrayList<>()

    private var filteredList: Boolean = false
    private val fixed: Boolean = false
    private var startDownload: Boolean = false
    private var switchCompat: SwitchCompat = null

    // values need to be set in derived classes constructor
    @StringRes protected Int title
    @StringRes protected Int progressInfo
    @StringRes protected Int errorReadingList
    @StringRes protected Int warnNoSelectedList
    @StringRes protected Int switchLabel

    abstract Boolean getFiltersetting()

    abstract Unit setFiltersetting(Boolean value)

    @WorkerThread
    abstract List<GCList> getList()

    abstract Boolean alwaysShow(GCList list)

    abstract Boolean supportMultiPreview()

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.gclist_activity)

        val extras: Bundle = getIntent().getExtras()
        if (extras != null) {
            startDownload = !extras.getBoolean(EXTRA_PQ_LIST_IMPORT, false)
        } else {
            startDownload = true
        }
        filteredList = getFiltersetting()

        val adapter: AbstractListAdapter = AbstractListAdapter(this)
        val view: RecyclerView = RecyclerViewProvider.provideRecyclerView(this, R.id.gclist, true, true)
        view.setAdapter(adapter)

        if (!fixed) {
            val bar: ActionBar = getSupportActionBar()
            if (bar != null) {
                @SuppressLint("InflateParams") val customView: View = getLayoutInflater().inflate(R.layout.gclist_actionbar, null)
                ((TextView) customView.findViewById(R.id.title)).setText(title)
                bar.setCustomView(customView)
                bar.setDisplayShowCustomEnabled(true)

                switchCompat = customView.findViewById(R.id.switchAB)
                switchCompat.setText(switchLabel)
                switchCompat.setVisibility(View.INVISIBLE)
                switchCompat.setChecked(!getFiltersetting())
                switchCompat.setOnCheckedChangeListener((a, b) -> checkSwitchState(adapter))

                customView.findViewById(R.id.download_selected).setOnClickListener(v -> {
                    val selectedLists: List<GCList> = adapter.getSelectedLists()
                    if (!selectedLists.isEmpty()) {
                        CacheListActivity.startActivityPocketDownload(customView.getContext(), selectedLists)
                    }
                })
                customView.findViewById(R.id.cachelist_selected).setOnClickListener(v -> {
                    val selectedLists: List<GCList> = adapter.getSelectedLists()
                    if (!selectedLists.isEmpty()) {
                        CacheListActivity.startActivityPocket(customView.getContext(), selectedLists)
                    }
                })

            }
        }

        val waitDialog: ProgressDialog = ProgressDialog.show(this, getString(title), getString(progressInfo), true, true)
        waitDialog.setCancelable(true)
        loadInBackground(adapter, waitDialog)
    }

    private Unit loadInBackground(final AbstractListAdapter adapter, final ProgressDialog waitDialog) {
        AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
            val list: List<GCList> = getList()
            if (CollectionUtils.isEmpty(list)) {
                ActivityMixin.showToast(AbstractListActivity.this, errorReadingList)
                return; // do not flood the user with multiple toasts
            }
            allGCLists.addAll(list)
        }, () -> {
            Collections.sort(allGCLists, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()))
            if (CollectionUtils.isEmpty(allGCLists)) {
                ActivityMixin.showToast(AbstractListActivity.this, getString(R.string.warn_no_pocket_query_found))
                finish()
            }
            fillAdapter(adapter)
            if (!fixed) {
                switchCompat.setVisibility(View.VISIBLE)
            }
            waitDialog.dismiss()
        })
    }

    private Unit fillAdapter(final AbstractListAdapter adapter) {
        visibleGCLists.clear()
        if (!filteredList) {
            for (final GCList pq : allGCLists) {
                if (alwaysShow(pq)) {
                    visibleGCLists.add(pq)
                }
            }
        } else {
            visibleGCLists.addAll(allGCLists)
        }
        adapter.notifyDataSetChanged()
    }

    private Unit checkSwitchState(final AbstractListAdapter adapter) {
        filteredList = !switchCompat.isChecked()
        setFiltersetting(filteredList)
        fillAdapter(adapter)
    }

    public List<GCList> getQueries() {
        return visibleGCLists
    }

    public Boolean getStartDownload() {
        return startDownload
    }

    public Unit returnResult(final GCList gcList) {
        setResult(RESULT_OK, Intent()
                .setDataAndType(gcList.getUri(), gcList.getMimeType()))
        finish()
    }

}
