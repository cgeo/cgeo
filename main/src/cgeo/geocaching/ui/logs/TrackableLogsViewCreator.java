package cgeo.geocaching.ui.logs;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.TrackableActivity;
import cgeo.geocaching.ui.UserActionsClickListener;

import org.apache.commons.lang3.StringUtils;

import android.text.Html;
import android.view.View;

import java.util.List;

public class TrackableLogsViewCreator extends LogsViewCreator {

    private final Trackable trackable;

    /**
     * @param trackableActivity
     */
    public TrackableLogsViewCreator(TrackableActivity trackableActivity, final Trackable trackable) {
        super(trackableActivity);
        this.trackable = trackable;
    }

    @Override
    protected boolean isValid() {
        return trackable != null;
    }

    @Override
    protected List<LogEntry> getLogs() {
        return trackable.getLogs();
    }

    @Override
    protected void addHeaderView() {
        // empty
    }

    @Override
    protected void fillCountOrLocation(LogViewHolder holder, final LogEntry log) {
        if (StringUtils.isBlank(log.cacheName)) {
            holder.countOrLocation.setVisibility(View.GONE);
        } else {
            holder.countOrLocation.setText(Html.fromHtml(log.cacheName));
            final String cacheGuid = log.cacheGuid;
            final String cacheName = log.cacheName;
            holder.countOrLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    CacheDetailActivity.startActivityGuid(activity, cacheGuid, Html.fromHtml(cacheName).toString());
                }
            });
        }
    }

    @Override
    protected String getGeocode() {
        return trackable.getGeocode();
    }

    @Override
    protected UserActionsClickListener createUserActionsListener() {
        return new UserActionsClickListener();
    }

}