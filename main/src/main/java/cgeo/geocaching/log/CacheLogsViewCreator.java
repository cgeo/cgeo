package cgeo.geocaching.log;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.databinding.LogsPageBinding;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.UserClickListener;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.TooltipCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.text.StringEscapeUtils;

public class CacheLogsViewCreator extends LogsViewCreator {
    private static final String BUNDLE_ALLLOGS = "alllogs";

    private final Resources res = CgeoApplication.getInstance().getResources();
    private LinearLayout countview1 = null;
    private TextView countview2 = null;

    public static TabbedViewPagerFragment<LogsPageBinding> newInstance(final boolean allLogs) {
        final CacheLogsViewCreator fragment = new CacheLogsViewCreator();
        final Bundle bundle = new Bundle();
        bundle.putBoolean(BUNDLE_ALLLOGS, allLogs);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public long getPageId() {
        final Bundle arguments = getArguments();
        return arguments == null ? 0 : arguments.getBoolean(BUNDLE_ALLLOGS) ? CacheDetailActivity.Page.LOGS.id : CacheDetailActivity.Page.LOGSFRIENDS.id;
    }

    private Geocache getCache() {
        final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
        return activity.getCache();
    }

    @Override
    protected List<LogEntry> getLogs() {
        final Geocache cache = getCache();
        final Bundle arguments = getArguments();
        final boolean allLogs = arguments == null || arguments.getBoolean(BUNDLE_ALLLOGS);
        final List<LogEntry> logs = allLogs ? cache.getLogs() : cache.getFriendsLogs();
        return addOwnOfflineLog(cache, logs);
    }

    private List<LogEntry> addOwnOfflineLog(final Geocache cache, final List<LogEntry> logsIn) {
        final LogEntry log = DataStore.loadLogOffline(cache.getGeocode());
        final List<LogEntry> logs = new ArrayList<>(logsIn);
        if (log != null) {
            logs.add(0, log.buildUpon().setAuthor(res.getString(R.string.log_your_saved_log)).build());
        }
        return logs;
    }

    @Override
    protected void addHeaderView() {
        if (binding != null) {
            addLogCountsHeader();
            addEmptyLogsHeader();
        }
    }

    @SuppressLint("SetTextI18n")
    private void addLogCountsHeader() {
        if (countview1 != null) {
            binding.getRoot().removeHeaderView(countview1);
            countview1 = null;
        }

        final Map<LogType, Integer> logCounts = getCache().getLogCounts();
        if (logCounts != null) {
            final List<Entry<LogType, Integer>> sortedLogCounts = new ArrayList<>(logCounts.size());
            for (final Entry<LogType, Integer> entry : logCounts.entrySet()) {
                // it may happen that the label is unknown -> then avoid any output for this type
                if (entry.getKey() != LogType.PUBLISH_LISTING && entry.getValue() != 0) {
                    sortedLogCounts.add(entry);
                }
            }

            if (!sortedLogCounts.isEmpty()) {
                // sort the log counts by type id ascending. that way the FOUND, DNF log types are the first and most visible ones
                Collections.sort(sortedLogCounts, (logCountItem1, logCountItem2) -> logCountItem1.getKey().compareTo(logCountItem2.getKey()));

                final List<String> labels = new ArrayList<>(sortedLogCounts.size());
                for (final Entry<LogType, Integer> pair : sortedLogCounts) {
                    labels.add(pair.getValue() + "× " + pair.getKey().getL10n());
                }

                countview1 = new LinearLayout(getActivity());
                final TextView logtypes = new TextView(getActivity());
                logtypes.setText(res.getString(R.string.cache_log_types) + ": ");
                countview1.addView(logtypes);
                for (final Entry<LogType, Integer> pair : sortedLogCounts) {
                    final TextView tv = new TextView(getActivity());
                    tv.setText(pair.getValue().toString());
                    tv.setCompoundDrawablesWithIntrinsicBounds(pair.getKey().getLogOverlay(), 0, 0, 0);
                    tv.setCompoundDrawablePadding(4);
                    tv.setPadding(0, 0, 10, 0);
                    TooltipCompat.setTooltipText(tv, pair.getKey().getL10n());
                    countview1.addView(tv);
                }
                binding.getRoot().addHeaderView(countview1, null, false);
            }
        }
    }

    private void addEmptyLogsHeader() {
        if (countview2 != null) {
            binding.getRoot().removeHeaderView(countview2);
            countview2 = null;
        }

        if (getLogs().isEmpty()) {
            countview2 = new TextView(getActivity());
            countview2.setText(res.getString(R.string.log_empty_logbook));
            binding.getRoot().addHeaderView(countview2, null, false);
        }
    }

    @Override
    protected ContextMenuDialog extendContextMenu(final ContextMenuDialog ctxMenu, final LogEntry log) {
        final CacheDetailActivity activity = (CacheDetailActivity) getActivity();
        if (getCache().canShareLog(log)) {
            ctxMenu.addItem(R.string.context_share_as_link, R.drawable.ic_menu_share, it -> getCache().shareLog(activity, log));
            ctxMenu.addItem(activity.getString(R.string.cache_menu_browser),
                    R.drawable.ic_menu_info_details, it -> getCache().openLogInBrowser(activity, log));
        }
        if (isOfflineLog(log)) {
            ctxMenu.addItem(R.string.cache_personal_note_edit, R.drawable.ic_menu_edit, it -> new EditOfflineLogListener(getCache(), activity).onClick(null));
        }
        return ctxMenu;
    }

    @Override
    protected View.OnClickListener createOnLogClickListener(final LogViewHolder holder, final LogEntry log) {
        if (isOfflineLog(log)) {
            return new EditOfflineLogListener(getCache(), (CacheDetailActivity) getActivity());
        }
        return super.createOnLogClickListener(holder, log);
    }


    @Override
    protected void fillCountOrLocation(final LogViewHolder holder, final LogEntry log) {
        // finds count
        if (log.found == -1) {
            holder.binding.countOrLocation.setVisibility(View.GONE);
        } else {
            holder.binding.countOrLocation.setVisibility(View.VISIBLE);
            holder.binding.countOrLocation.setText(res.getQuantityString(R.plurals.cache_counts, log.found, log.found));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void fillViewHolder(final View convertView, final LogViewHolder holder, final LogEntry log) {
        super.fillViewHolder(convertView, holder, log);
        if (isOfflineLog(log)) {
            holder.binding.author.setOnClickListener(new EditOfflineLogListener(getCache(), (CacheDetailActivity) getActivity()));
            holder.binding.logMark.setVisibility(View.VISIBLE);
            holder.binding.logMark.setImageResource(R.drawable.mark_orange);
        }
        // avoid the need for a double tap to edit item (offline log) resp. display context menu (all other log types)
        // see stackoverflow.com/questions/22653641/using-onclick-on-textview-with-selectable-text-how-to-avoid-double-click
        holder.binding.log.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.requestFocus();
            }
            return false;
        });
    }

    private boolean isOfflineLog(final LogEntry log) {
        return log.author.equals(getString(R.string.log_your_saved_log));
    }

    @Override
    protected boolean isValid() {
        return getActivity() != null && getCache() != null;
    }

    @Override
    protected String getGeocode() {
        return getCache().getGeocode();
    }

    @Override
    protected UserClickListener createUserActionsListener(final LogEntry log) {
        final String userName = StringEscapeUtils.unescapeHtml4(log.author);
        return UserClickListener.forUser(getCache(), userName, userName, log.authorGuid);
    }

}
