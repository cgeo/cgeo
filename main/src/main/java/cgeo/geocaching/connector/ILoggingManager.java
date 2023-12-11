package cgeo.geocaching.connector;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Calendar;
import java.util.List;

/**
 * An instance of an implementing class allows online logging activites for a concrete geocache.
 */
public interface ILoggingManager {

    /** Returns the cache that this instance is associated with */
    @NonNull
    Geocache getCache();

    /** Returns the connector that this logging manager is associated with */
    @NonNull
    IConnector getConnector();

    /** Post a new log for a cache online */
    @NonNull
    @WorkerThread
    LogResult postLog(@NonNull LogType logType,
                      @NonNull Calendar date,
                      @NonNull String log,
                      @Nullable String logPassword,
                      @NonNull List<TrackableLog> trackableLogs,
                      @NonNull ReportProblemType reportProblem,
                      float rating);

    @NonNull
    @WorkerThread
    ImageResult postLogImage(String logId,
                             Image image);

    /** Retrieves additional contexxt information which can be used for online logging */
    @NonNull
    @WorkerThread
    LogContextInfo getLogContextInfo(@Nullable String serviceLogId);

    Long getMaxImageUploadSize();

    boolean isImageCaptionMandatory();

    @NonNull
    List<ReportProblemType> getReportProblemTypes(@NonNull Geocache geocache);

}
