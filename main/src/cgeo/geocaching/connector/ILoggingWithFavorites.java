package cgeo.geocaching.connector;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Calendar;
import java.util.List;

/**
 * This is the counterpart of {@link cgeo.geocaching.connector.capability.IFavoriteCapability} for logging.
 * This Interface  has to be implemented by Logging Managers in order to be able to mark caches
 * as favorites directly on Cache Logging screen.
 * Typically the implementation of the fetching is done inside @code {onLoadFinished} method.
 */
public interface ILoggingWithFavorites extends ILoggingManager {

    /**
     * Post a log for a cache online,
     * including if it should be favorited
     *
     * @param logPassword optional, maybe null
     */
    @NonNull
    @WorkerThread
    LogResult postLog(@NonNull LogType logType,
                      @NonNull Calendar date,
                      @NonNull String log,
                      @Nullable String logPassword,
                      @NonNull List<TrackableLog> trackableLogs,
                      @NonNull ReportProblemType reportProblem,
                      boolean addToFavorites);

    /**
     * @return number of available favorite points. This number will be displayed near "add to favorites" checkbox
     */
    int getFavoritePoints();

    /**
     * @return true if there was loading error, false otherwise.
     */
    boolean hasFavPointLoadError();
}
