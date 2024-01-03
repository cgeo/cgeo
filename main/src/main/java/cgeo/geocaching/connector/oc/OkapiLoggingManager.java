package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OkapiLoggingManager extends AbstractLoggingManager {

    public OkapiLoggingManager(@NonNull final OCApiLiveConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @NonNull
    @Override
    public OCApiLiveConnector getConnector() {
        return (OCApiLiveConnector) super.getConnector();
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final OfflineLogEntry logEntry, @Nullable final Map<String, Trackable> inventory) {
        final LogResult result = OkapiClient.postLog(getCache(), logEntry.logType, new Date(logEntry.date), logEntry.log, logEntry.password, getConnector(), logEntry.reportProblem, logEntry.favorite, logEntry.rating);
        getConnector().login();
        return result;
    }

    @NonNull
    @Override
    public ImageResult createLogImage(@NonNull final String logId, @NonNull final Image image) {
        return OkapiClient.postLogImage(logId, image, getConnector());
    }

    @NonNull
    @Override
    public LogContextInfo getLogContextInfo(@Nullable final String serviceLogId) {
        final LogContextInfo info = new LogContextInfo(this, serviceLogId);
        if (!getConnector().isLoggedIn()) {
            info.setError();
        }
        return info;
    }
    @Override
    public boolean canLogReportType(@NonNull final ReportProblemType reportType) {
        //Okapi can handle one report type
        return ReportProblemType.NEEDS_MAINTENANCE == reportType;
    }

    @Override
    public boolean supportsLogWithFavorite() {
        return true;
    }

    @Override
    public boolean supportsLogWithVote() {
        return true;
    }

    @Override
    public boolean isImageCaptionMandatory() {
        return true;
    }

    @NonNull
    @Override
    public List<ReportProblemType> getReportProblemTypes(@NonNull final Geocache geocache) {
        if (geocache.isEventCache()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(ReportProblemType.NEEDS_MAINTENANCE);
    }

    @Override
    public int getFavoriteCheckboxText() {
        return R.plurals.fav_points_remaining_oc;
    }
}
