package cgeo.geocaching.log;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.UserClickListener;
import cgeo.geocaching.ui.dialog.ContextMenuDialog;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class CacheLogsViewCreator extends LogsViewCreator {
    private final boolean allLogs;
    private final Resources res = CgeoApplication.getInstance().getResources();
    private final CacheDetailActivity cacheDetailActivity;
    private TextView countview1 = null;
    private TextView countview2 = null;

    public CacheLogsViewCreator(final CacheDetailActivity cacheDetailActivity, final boolean allLogs) {
        super(cacheDetailActivity);
        this.cacheDetailActivity = cacheDetailActivity;
        this.allLogs = allLogs;
    }

    private Geocache getCache() {
        return cacheDetailActivity.getCache();
    }

    @Override
    protected List<LogEntry> getLogs() {
        final Geocache cache = getCache();
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

                countview1 = new TextView(activity);
                countview1.setText(res.getString(R.string.cache_log_types) + ": " + StringUtils.join(labels, ", "));
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
            countview2 = new TextView(activity);
            countview2.setText(res.getString(R.string.log_empty_logbook));
            binding.getRoot().addHeaderView(countview2, null, false);
        }
    }

    @Override
    protected ContextMenuDialog extendContextMenu(final ContextMenuDialog ctxMenu, final LogEntry log) {
        if (getCache().canShareLog(log)) {
            ctxMenu.addItem(R.string.context_share_as_link, R.drawable.ic_menu_share, it -> getCache().shareLog(cacheDetailActivity, log));
            ctxMenu.addItem(cacheDetailActivity.getString(R.string.cache_menu_browser),
                    R.drawable.ic_menu_info_details, it -> getCache().openLogInBrowser(cacheDetailActivity, log));
        }
        if (isOfflineLog(log)) {
            ctxMenu.addItem(R.string.cache_personal_note_edit, R.drawable.ic_menu_edit, it -> new EditOfflineLogListener(getCache(), cacheDetailActivity).onClick(null));
        }
        return ctxMenu;
    }

    @Override
    protected View.OnClickListener createOnLogClickListener(final LogViewHolder holder, final LogEntry log) {
        if (isOfflineLog(log)) {
            return new EditOfflineLogListener(getCache(), cacheDetailActivity);
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

    @Override
    protected void fillViewHolder(final View convertView, final LogViewHolder holder, final LogEntry log) {
        super.fillViewHolder(convertView, holder, log);
        if (isOfflineLog(log)) {
            holder.binding.author.setOnClickListener(new EditOfflineLogListener(getCache(), cacheDetailActivity));
            holder.binding.logMark.setVisibility(View.VISIBLE);
            holder.binding.logMark.setImageResource(R.drawable.mark_orange);
        }
    }

    private boolean isOfflineLog(final LogEntry log) {
        return log.author.equals(activity.getString(R.string.log_your_saved_log));
    }

    @Override
    protected boolean isValid() {
        return getCache() != null;
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
