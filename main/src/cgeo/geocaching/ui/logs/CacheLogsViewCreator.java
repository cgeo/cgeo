package cgeo.geocaching.ui.logs;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.ui.UserActionsClickListener;

import org.apache.commons.lang3.StringUtils;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CacheLogsViewCreator extends LogsViewCreator {
    private final boolean allLogs;
    private final Resources res = CgeoApplication.getInstance().getResources();
    private final CacheDetailActivity cacheDetailActivity;

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
            log.author = res.getString(R.string.log_your_saved_log);
            logs.add(0, log);
        }
        return logs;
    }

    @Override
    protected void addHeaderView() {
        // adds the log counts
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
                Collections.sort(sortedLogCounts, new Comparator<Entry<LogType, Integer>>() {

                    @Override
                    public int compare(final Entry<LogType, Integer> logCountItem1, final Entry<LogType, Integer> logCountItem2) {
                        return logCountItem1.getKey().compareTo(logCountItem2.getKey());
                    }
                });

                final List<String> labels = new ArrayList<>(sortedLogCounts.size());
                for (final Entry<LogType, Integer> pair : sortedLogCounts) {
                    labels.add(pair.getValue() + "× " + pair.getKey().getL10n());
                }

                final TextView countView = new TextView(activity);
                countView.setText(res.getString(R.string.cache_log_types) + ": " + StringUtils.join(labels, ", "));
                view.addHeaderView(countView, null, false);
            }
        }
    }

    @Override
    protected void fillCountOrLocation(final LogViewHolder holder, final LogEntry log) {
        // finds count
        if (log.found == -1) {
            holder.countOrLocation.setVisibility(View.GONE);
        } else {
            holder.countOrLocation.setVisibility(View.VISIBLE);
            holder.countOrLocation.setText(res.getQuantityString(R.plurals.cache_counts, log.found, log.found));
        }
    }

    @Override
    protected void fillViewHolder(final View convertView, final LogViewHolder holder, final LogEntry log) {
        super.fillViewHolder(convertView, holder, log);
        if (isOfflineLog(log)) {
            holder.author.setOnClickListener(new EditOfflineLogListener(getCache(), cacheDetailActivity));
            holder.text.setOnClickListener(new EditOfflineLogListener(getCache(), cacheDetailActivity));
            holder.marker.setVisibility(View.VISIBLE);
            holder.marker.setImageResource(R.drawable.mark_orange);
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
    protected UserActionsClickListener createUserActionsListener() {
        return new UserActionsClickListener(getCache());
    }

}
