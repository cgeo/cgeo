package cgeo.geocaching.connector.su;

import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;

public class SuLoggingManager extends AbstractLoggingManager {

    SuLoggingManager(@NonNull final SuConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
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

    @NonNull
    @Override
    public LogResult createLog(@NonNull final LogEntry logEntry, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, final boolean addToFavorites, final float rating) {
        final LogResult result;
        final Geocache cache = getCache();
        try {
            result = SuApi.postLog(cache, logEntry.logType, new Date(logEntry.date), logEntry.log, addToFavorites);
        } catch (final SuApi.SuApiException e) {
            Log.e("Logging manager SuApi.postLog exception: ", e);
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Logging manager SuApi.postLog exception", e);
        }

        if (addToFavorites) {
            cache.setFavorite(true);
            cache.setFavoritePoints(cache.getFavoritePoints() + 1);
        }
        return result;
    }

    @NonNull
    @Override
    public ImageResult createLogImage(@NonNull final String logId, @NonNull final Image image) {
        return SuApi.postImage(getCache(), image);
    }

    @Override
    public boolean supportsLogWithFavorite() {
        return true;
    }
}
