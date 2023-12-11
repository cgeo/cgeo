package cgeo.geocaching.connector.su;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

public class SuLoggingManager extends AbstractLoggingManager implements ILoggingWithFavorites {

    SuLoggingManager(@NonNull final SuConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @NonNull
    @Override
    public LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem, final float rating) {
        return postLog(logType, date, log, logPassword, trackableLogs, reportProblem, false, rating);
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem, final boolean addToFavorites, final float rating) {
        final LogResult result;
        final Geocache cache = getCache();
        try {
            result = SuApi.postLog(cache, logType, date, log, addToFavorites);
        } catch (final SuApi.SuApiException e) {
            Log.e("Logging manager SuApi.postLog exception: ", e);
            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        }

        if (addToFavorites) {
            cache.setFavorite(true);
            cache.setFavoritePoints(cache.getFavoritePoints() + 1);
        }
        return result;
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final Image image) {
        return SuApi.postImage(getCache(), image);
    }

    @NonNull
    @Override
    public LogContextInfo getLogContextInfo(@Nullable final String serviceLogId) {
        final LogContextInfo info = new LogContextInfo(this, serviceLogId);
        final Integer recCount = SuApi.getAvailableRecommendations();
        if (recCount != null) {
            info.setAvailableFavoritePoints(recCount);
        }
        return info;

    }

    @Override
    public int getFavoriteCheckboxText() {
        return R.plurals.fav_points_remaining;
    }

}
