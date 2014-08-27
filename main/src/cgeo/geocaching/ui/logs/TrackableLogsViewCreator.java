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

    private Trackable trackable;
    private final TrackableActivity trackableActivity;

    /**
     * @param trackableActivity
     */
    public TrackableLogsViewCreator(final TrackableActivity trackableActivity) {
        super(trackableActivity);
        this.trackableActivity = trackableActivity;
        trackable = trackableActivity.getTrackable();
    }

    @Override
    protected boolean isValid() {
        return trackable != null;
    }

    @Override
    protected List<LogEntry> getLogs() {
        trackable = trackableActivity.getTrackable();
        return trackable.getLogs();
    }

    @Override
    protected void addHeaderView() {
        // empty
    }

    @Override
    protected void fillCountOrLocation(final LogViewHolder holder, final LogEntry log) {
        if (StringUtils.isBlank(log.cacheName)) {
            holder.countOrLocation.setVisibility(View.GONE);
        } else {
            holder.countOrLocation.setText(Html.fromHtml(log.cacheName));
            final String cacheGuid = log.cacheGuid;
            final String cacheName = log.cacheName;
            holder.countOrLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View arg0) {
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
        return new UserActionsClickListener(trackable);
    }

}