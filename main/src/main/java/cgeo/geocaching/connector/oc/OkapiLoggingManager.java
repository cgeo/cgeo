package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class OkapiLoggingManager extends AbstractLoggingManager implements ILoggingWithFavorites {

    public OkapiLoggingManager(@NonNull final OCApiLiveConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @NonNull
    @Override
    public OCApiLiveConnector getConnector() {
        return (OCApiLiveConnector) super.getConnector();
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem, final float rating) {
        final LogResult result = OkapiClient.postLog(getCache(), logType, date, log, logPassword, getConnector(), reportProblem, false, rating);
        getConnector().login();
        return result;
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem, final boolean addToFavorites, final float rating) {
        final LogResult result = OkapiClient.postLog(getCache(), logType, date, log, logPassword, getConnector(), reportProblem, addToFavorites, rating);
        getConnector().login();
        return result;
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final Image image) {
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
    public Long getMaxImageUploadSize() {
        return null;
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
