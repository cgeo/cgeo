package cgeo.geocaching.ui.logs;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
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
    private final Resources res = cgeoapplication.getInstance().getResources();

    public CacheLogsViewCreator(CacheDetailActivity cacheDetailActivity, boolean allLogs) {
        super(cacheDetailActivity);
        this.allLogs = allLogs;
    }

    /**
     * May return null!
     *
     * @return
     */
    private Geocache getCache() {
        if (this.activity instanceof CacheDetailActivity) {
            CacheDetailActivity details = (CacheDetailActivity) this.activity;
            return details.getCache();
        }
        return null;
    }

    @Override
    protected List<LogEntry> getLogs() {
        return allLogs ? getCache().getLogs() : getCache().getFriendsLogs();
    }

    @Override
    protected void addHeaderView() {
        // adds the log counts
        final Map<LogType, Integer> logCounts = getCache().getLogCounts();
        if (logCounts != null) {
            final List<Entry<LogType, Integer>> sortedLogCounts = new ArrayList<Entry<LogType, Integer>>(logCounts.size());
            for (final Entry<LogType, Integer> entry : logCounts.entrySet()) {
                // it may happen that the label is unknown -> then avoid any output for this type
                if (entry.getKey() != LogType.PUBLISH_LISTING && entry.getKey().getL10n() != null) {
                    sortedLogCounts.add(entry);
                }
            }

            if (!sortedLogCounts.isEmpty()) {
                // sort the log counts by type id ascending. that way the FOUND, DNF log types are the first and most visible ones
                Collections.sort(sortedLogCounts, new Comparator<Entry<LogType, Integer>>() {

                    @Override
                    public int compare(Entry<LogType, Integer> logCountItem1, Entry<LogType, Integer> logCountItem2) {
                        return logCountItem1.getKey().compareTo(logCountItem2.getKey());
                    }
                });

                final ArrayList<String> labels = new ArrayList<String>(sortedLogCounts.size());
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
    protected void fillCountOrLocation(LogViewHolder holder, final LogEntry log) {
        // finds count
        if (log.found == -1) {
            holder.countOrLocation.setVisibility(View.GONE);
        } else {
            holder.countOrLocation.setVisibility(View.VISIBLE);
            holder.countOrLocation.setText(res.getQuantityString(R.plurals.cache_counts, log.found, log.found));
        }
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