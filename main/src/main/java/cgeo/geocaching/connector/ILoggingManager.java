package cgeo.geocaching.connector;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.List;
import java.util.Map;

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

    /** Create a new log for a cache online */
    @NonNull
    @WorkerThread
    LogResult createLog(@NonNull OfflineLogEntry logEntry, @Nullable Map<String, Trackable> inventory);

    /** Edits an existing log for a cache online */
    @NonNull
    @WorkerThread
    LogResult editLog(@NonNull LogEntry newEntry);

    /** Deletes an existing log for a cache online */
    @NonNull
    @WorkerThread
    LogResult deleteLog(@NonNull LogEntry newEntry);

    /** converts a log text from a */
    String convertLogTextToEditableText(String logText);

    /** Returns whether this log entry can be edited */
    boolean canEditLog(@NonNull LogEntry entry);

    /** Returns whether this log entry can be deleted */
    boolean canDeleteLog(@NonNull LogEntry entry);

    /** Returns true if this manager can handle the given reporttype on log create */
    boolean canLogReportType(@NonNull ReportProblemType reportType);


    /** Attach an image to an existing log. The supplied image's MUST point to a local file */
    @NonNull
    @WorkerThread
    ImageResult createLogImage(@NonNull String logId, @NonNull Image image);

    /** Edit an images properties already attached to an existing log */
    @NonNull
    @WorkerThread
    ImageResult editLogImage(@NonNull String logId, @NonNull String serviceImageId, @Nullable String title, @Nullable String description);

    /** Deletes an image from an existing log */
    @NonNull
    @WorkerThread
    ImageResult deleteLogImage(@NonNull String logId, @NonNull String serviceImageId);

    /** Returns whether this manager supports editing a log image's properties */
    boolean supportsEditLogImages();

    /** Returns whether this manager supports deleting a log image from a log */
    boolean supportsDeleteLogImages();

    /** Returns whether this manager supports the assignment of a favorite point together with log creation */
    boolean supportsLogWithFavorite();

    /** Returns whether this manager supports tracking actions together with log creation */
    boolean supportsLogWithTrackables();

    /** Returns whether this manager supports passing a vote/rating together with log creation */
    boolean supportsLogWithVote();

    /** Retrieves additional contexxt information which can be used for online logging */
    @NonNull
    @WorkerThread
    LogContextInfo getLogContextInfo(@Nullable String serviceLogId);

    Long getMaxImageUploadSize();

    boolean isImageCaptionMandatory();

    @NonNull
    List<ReportProblemType> getReportProblemTypes(@NonNull Geocache geocache);

    int getFavoriteCheckboxText();

}
