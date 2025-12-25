// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector

import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.util.List
import java.util.Map

/**
 * An instance of an implementing class allows online logging activites for a concrete geocache.
 */
interface ILoggingManager {

    /** Returns the cache that this instance is associated with */
    Geocache getCache()

    /** Returns the connector that this logging manager is associated with */
    IConnector getConnector()

    /** Create a log for a cache online */
    @WorkerThread
    LogResult createLog(OfflineLogEntry logEntry, Map<String, Trackable> inventory)

    /** Edits an existing log for a cache online */
    @WorkerThread
    LogResult editLog(LogEntry newEntry)

    /** Deletes an existing log for a cache online */
    @WorkerThread
    LogResult deleteLog(LogEntry newEntry, String reason)

    /** converts a log text from a */
    String convertLogTextToEditableText(String logText)

    /** Returns whether this log entry can be edited */
    Boolean canEditLog(LogEntry entry)

    /** Returns whether this log entry can be deleted */
    Boolean canDeleteLog(LogEntry entry)

    /** Returns true if this manager can handle the given reporttype on log create */
    Boolean canLogReportType(ReportProblemType reportType)


    /** Attach an image to an existing log. The supplied image's MUST point to a local file */
    @WorkerThread
    ImageResult createLogImage(String logId, Image image)

    /** Edit an images properties already attached to an existing log */
    @WorkerThread
    ImageResult editLogImage(String logId, String serviceImageId, String title, String description)

    /** Deletes an image from an existing log */
    @WorkerThread
    ImageResult deleteLogImage(String logId, String serviceImageId)

    /** Returns whether this manager supports editing a log image's properties */
    Boolean supportsEditLogImages()

    /** Returns whether this manager supports deleting a log image from a log */
    Boolean supportsDeleteLogImages()

    /** Returns whether this manager supports the assignment of a favorite point together with log creation */
    Boolean supportsLogWithFavorite()

    /** Returns whether this manager supports tracking actions together with log creation */
    Boolean supportsLogWithTrackables()

    /** Returns whether this manager supports passing a vote/rating together with log creation */
    Boolean supportsLogWithVote()

    /** Retrieves additional contexxt information which can be used for online logging */
    @WorkerThread
    LogContextInfo getLogContextInfo(String serviceLogId)

    Long getMaxImageUploadSize()

    Boolean isImageCaptionMandatory()

    List<ReportProblemType> getReportProblemTypes(Geocache geocache)

    Int getFavoriteCheckboxText()

}
