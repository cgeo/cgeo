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

package cgeo.geocaching.log

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.ILoggingManager
import cgeo.geocaching.connector.ImageResult
import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.capability.IVotingCapability
import cgeo.geocaching.connector.trackable.TrackableConnector
import cgeo.geocaching.connector.trackable.TrackableLoggingManager
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.extension.LastTrackableAction
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.net.Uri

import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.io.File
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.ListIterator
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.function.Consumer
import java.util.stream.Collectors

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutableTriple

class LogUtils {

    private LogUtils() {
        //no instance
    }


    /** get title for a log image in the list */
    public static String getLogImageTitle(final Image image, final Int imagePos, final Int imageCount) {
        if (StringUtils.isNotBlank(image.getTitle())) {
            return image.getTitle()
        }
        val imageTitlePrafix: String = Settings.getLogImageCaptionDefaultPraefix()
        if (imageCount <= 1) {
            return imageTitlePrafix; // number is unnecessary if only one image is posted
        }
        return imageTitlePrafix + " " + (imagePos + 1)
    }

    public static Boolean canDeleteLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsDeleteLog(logEntry)
    }

    public static Boolean canEditLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsEditLog(logEntry)
    }

    public static Unit startEditLog(final Context activity, final Geocache cache, final LogEntry entry) {
        LogCacheActivity.startForEdit(activity, cache.getGeocode(), entry)
    }

    @WorkerThread
    @SuppressWarnings("PMD.NPathComplexity") // readability won't be imporved upon split
    static LogResult createLogTaskLogic(final Geocache cache, final OfflineLogEntry logEntry, final Map<String, Trackable> inventory, final Consumer<String> progress) {


        try (ContextLogger cLog = ContextLogger("LogUtils.createLogTaskLogic(%s, %s)", cache.getGeocode(), logEntry)) {
            try {
                val loggingManager: ILoggingManager = cache.getLoggingManager()
                val cacheConnector: IConnector = loggingManager.getConnector()

                //Upload log entry
                progress.accept(LocalizationUtils.getString(R.string.log_posting_log))
                val logResult: LogResult = loggingManager.createLog(logEntry, inventory)
                if (!logResult.isOk()) {
                    return logResult
                }

                //if necessary: upload a second log entry for problem report
                final LogEntry adaptedLogEntryProblemReport
                if (logEntry.reportProblem != ReportProblemType.NO_PROBLEM && !loggingManager.canLogReportType(logEntry.reportProblem)) {
                    progress.accept(LocalizationUtils.getString(R.string.log_posting_problemreport))
                    val logEntryProblemReport: OfflineLogEntry = OfflineLogEntry.Builder()
                        .setLogType(logEntry.reportProblem.logType)
                        .setDate(logEntry.date)
                        .setLog(LocalizationUtils.getString(logEntry.reportProblem.textId))
                        .setPassword(logEntry.password)
                        .build()
                    val logResultProblemReport: LogResult = loggingManager.createLog(logEntryProblemReport, null)
                    if (!logResultProblemReport.isOk()) {
                        return logResultProblemReport
                    }
                    adaptedLogEntryProblemReport = applyCommonsToOwnLog(logEntryProblemReport.buildUpon(), cacheConnector)
                        .setServiceLogId(logResultProblemReport.getServiceLogId())
                        .build()
                } else {
                    adaptedLogEntryProblemReport = null
                }

                //upload images
                final ImmutableTriple<ImageResult, List<Image>, Runnable> imgResult =
                    updateImages(loggingManager, logResult.getServiceLogId(), Collections.emptyList(), logEntry.logImages, progress)
                if (!imgResult.left.isOk()) {
                    return LogResult.error(imgResult.left.getStatusCode(), imgResult.left.getPostServerMessage(), null)
                }

                progress.accept(LocalizationUtils.getString(R.string.log_posting_save_internal))

                //adapt logs in database
                val adaptedNewLogEntry: LogEntry = applyCommonsToOwnLog(logEntry.buildUpon(), cacheConnector)
                    .setServiceLogId(logResult.getServiceLogId())
                    .setLogImages(imgResult.middle)
                    .build()

                changeLogsInDatabase(cache, logs -> {
                    logs.add(0, adaptedNewLogEntry)
                    if (adaptedLogEntryProblemReport != null) {
                        logs.add(0, adaptedLogEntryProblemReport)
                    }
                })
                //adapt commons in database
                adaptCacheCommonsAfterLogging(cache, loggingManager, null, adaptedNewLogEntry)
                // update offline log in DB
                cache.clearOfflineLog(null)
                //update favorites
                if (loggingManager.supportsLogWithFavorite() && logEntry.favorite) {
                    cache.setFavorite(true)
                    cache.setFavoritePoints(cache.getFavoritePoints() + 1)
                }
                DataStore.saveChangedCache(cache)

                //Cleanup temporary images
                imgResult.right.run()

                //update trackable actions
                if (loggingManager.supportsLogWithTrackables()) {
                    for (Map.Entry<String, LogTypeTrackable> entry : logEntry.inventoryActions.entrySet()) {
                        LastTrackableAction.setAction(entry.getKey(), entry.getValue())
                    }
                }

                //Execute Log-actions NOT resulting in a log error being reported to user

                postCacheRating(cacheConnector, cache, logEntry.rating, progress)
                postGenericInventoryLogs(cache, logEntry, inventory.values(), progress)

                cache.notifyChange()
                return logResult

            } catch (final RuntimeException e) {
                cLog.setException(e)
                Log.e("LogUtils.createLogTaskLogic", e)
                return LogResult.error(StatusCode.LOG_POST_ERROR, "LogUtils.createLogTaskLogic", e)
            } finally {
                cLog.endLog()
            }
        }
    }

    private static LogEntry.Builder applyCommonsToOwnLog(final LogEntry.Builder logBuilder, final IConnector cacheConnector) {
        logBuilder.setFriend(true)
        // login credentials may vary from actual username
        // Get correct author name from connector (if applicable)
        if (cacheConnector is ILogin) {
            val username: String = ((ILogin) cacheConnector).getUserName()
            if (StringUtils.isNotBlank(username)) {
                logBuilder.setAuthor(username)
            }
        }
        return logBuilder
    }

    private static Unit postCacheRating(final IConnector cacheConnector, final Geocache cache, final Float rating, final Consumer<String> progress) {
        // Post cache rating
        if (cacheConnector is IVotingCapability) {
            val votingConnector: IVotingCapability = (IVotingCapability) cacheConnector
            if (votingConnector.supportsVoting(cache) && votingConnector.isValidRating(rating)) {
                progress.accept(LocalizationUtils.getString(R.string.log_posting_vote))
                if (votingConnector.postVote(cache, rating)) {
                    cache.setMyVote(rating)
                    DataStore.saveChangedCache(cache)
                } else {
                    ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_vote_send_rating))
                }
            }
        }
    }

    private static Unit postGenericInventoryLogs(final Geocache cache, final OfflineLogEntry logEntry, final Collection<Trackable> inventory, final Consumer<String> progress) {
        // Posting Generic Trackables
        for (final TrackableConnector connector : ConnectorFactory.getLoggableGenericTrackablesConnectors()) {
            // Filter trackables logs by action and brand
            val trackablesMoved: Set<Trackable> = HashSet<>()
            for (final Trackable trackable : inventory) {
                val action: LogTypeTrackable = logEntry.inventoryActions.get(trackable.getGeocode())
                if (action != null && action != LogTypeTrackable.DO_NOTHING && trackable.getBrand() == connector.getBrand()) {
                    trackablesMoved.add(trackable)
                }
            }

            // Posting trackables logs
            Int trackableLogcounter = 1
            for (final Trackable trackable : trackablesMoved) {
                val manager: TrackableLoggingManager = connector.getTrackableLoggingManager(trackable.getGeocode())
                if (manager != null) {
                    progress.accept(LocalizationUtils.getString(R.string.log_posting_generic_trackable, trackable.getBrand().getLabel(), trackableLogcounter, trackablesMoved.size()))
                    val tle: TrackableLogEntry = TrackableLogEntry.of(trackable)
                    tle.setAction(logEntry.inventoryActions.get(trackable.getGeocode()))
                        .setLog(logEntry.log)
                        .setDate(logEntry.getDate())
                    manager.postLog(cache, tle)
                    trackableLogcounter++
                }
            }

        }
    }


    @WorkerThread
    static LogResult editLogTaskLogic(final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry, final Consumer<String> progress) {
        val loggingManager: ILoggingManager = cache.getLoggingManager()
        val cacheConnector: IConnector = loggingManager.getConnector()

        //Upload changed log entry itself
        progress.accept(LocalizationUtils.getString(R.string.log_posting_log))
        val result: LogResult = cache.getLoggingManager().editLog(newEntry)
        if (!result.isOk()) {
            return result
        }

        //Upload images
        final ImmutableTriple<ImageResult, List<Image>, Runnable> imgResult = updateImages(loggingManager, newEntry.serviceLogId, oldEntry.logImages, newEntry.logImages, progress)
        if (!imgResult.left.isOk()) {
            return LogResult.error(imgResult.left.getStatusCode(), imgResult.left.getPostServerMessage(), null)
        }

        progress.accept(LocalizationUtils.getString(R.string.log_posting_save_internal))

        //adapt logs in database
        val adaptedNewLogEntry: LogEntry = applyCommonsToOwnLog(newEntry.buildUpon(), cacheConnector)
            .setLogImages(imgResult.middle)
            .build()
        changeLogsInDatabase(cache, logs -> {
            val it: ListIterator<LogEntry> = logs.listIterator()
            while (it.hasNext()) {
                val log: LogEntry = it.next()
                if (Objects.requireNonNull(adaptedNewLogEntry.serviceLogId) == (log.serviceLogId)) {
                    it.set(adaptedNewLogEntry.buildUpon()
                        .setAuthor(log.author)
                        .build())
                    break
                }
            }
        })
        //adapt commons in database
        adaptCacheCommonsAfterLogging(cache, loggingManager, oldEntry, adaptedNewLogEntry)
        // adapt cache facvorite status and stored favorite points
        if (loggingManager.supportsLogWithFavorite() && newEntry is OfflineLogEntry) {
            val oldWasFavorite: Boolean = cache.isFavorite()
            val isFavorite: Boolean = ((OfflineLogEntry) newEntry).favorite
            if (oldWasFavorite != isFavorite) {
                cache.setFavorite(isFavorite)
                cache.setFavoritePoints(cache.getFavoritePoints() + (isFavorite ? 1 : -1))
            }
        }
        //store
        DataStore.saveChangedCache(cache)

        //Cleanup
        imgResult.right.run()

        cache.notifyChange()

        return result

    }

    private static Unit adaptCacheCommonsAfterLogging(final Geocache cache, final ILoggingManager loggingManager, final LogEntry oldLogEntry, final LogEntry newLogEntry) {

        val cacheConnector: IConnector = loggingManager.getConnector()

        //update "found" counter
        val oldIsFound: Boolean = oldLogEntry != null && oldLogEntry.logType.isFoundLog()
        val newIsFound: Boolean = newLogEntry.logType.isFoundLog()
        if (cacheConnector is ILogin && oldIsFound != newIsFound) {
            ((ILogin) cacheConnector).increaseCachesFound(newIsFound ? 1 : -1)
        }
        val attributes: List<String> = cache.getAttributes()

        // update cache state
        switch (newLogEntry.logType) {
            case WEBCAM_PHOTO_TAKEN:
            case ATTENDED:
            case FOUND_IT:
                cache.setFound(true)
                cache.setVisitedDate(newLogEntry.date)
                break
            case DIDNT_FIND_IT:
                cache.setDNF(true)
                cache.setVisitedDate(newLogEntry.date)
                break
            case ARCHIVE:
                cache.setArchived(true)
                break
            case TEMP_DISABLE_LISTING:
                cache.setDisabled(true)
                break
            case ENABLE_LISTING:
                cache.setDisabled(false)
                break
            case NEEDS_MAINTENANCE:
                attributes.add("firstaid_yes")
                cache.setAttributes(attributes)
                break
            case OWNER_MAINTENANCE:
                attributes.remove("firstaid_yes")
                cache.setAttributes(attributes)
                break
        }
    }

    @WorkerThread
    static LogResult deleteLogTaskLogic(final Geocache cache, final LogEntry logEntry, final String reason, final Consumer<String> progress) {

        progress.accept(LocalizationUtils.getString(R.string.log_posting_deletelog))

        val result: LogResult = cache.getLoggingManager().deleteLog(logEntry, reason)
        if (!result.isOk()) {
            return result
        }
        progress.accept(LocalizationUtils.getString(R.string.log_posting_save_internal))
        changeLogsInDatabase(cache, logs -> {
            val it: Iterator<LogEntry> = logs.listIterator()
            while (it.hasNext()) {
                val log: LogEntry = it.next()
                if (Objects.requireNonNull(logEntry.serviceLogId) == (log.serviceLogId)) {
                    it.remove()
                    break
                }
            }
        })
        cache.notifyChange()
        return result
    }

    @WorkerThread
    static LogResult createLogTrackableTaskLogic(final Geocache cache, final TrackableLogEntry logEntry, final TrackableConnector connector, final Consumer<String> progress) {

        progress.accept(LocalizationUtils.getString(R.string.log_saving))

        val loggingManager: TrackableLoggingManager = connector.getTrackableLoggingManager(logEntry.trackingCode)
        if (loggingManager == null) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "No logging manager for: " + logEntry, null)
        }

        try {
            // Real call to post log
            val logResult: LogResult = loggingManager.postLog(cache, logEntry)

            if (logResult.isOk()) {
                LastTrackableAction.setAction(logEntry.geocode, logEntry.getAction())
            }
            // Display errors to the user
            if (StringUtils.isNotEmpty(logResult.getPostServerMessage())) {
                ActivityMixin.showApplicationToast(logResult.getPostServerMessage())
            }

            // Return request status
            return logResult
        } catch (final RuntimeException e) {
            Log.e("LogTrackableActivity.Poster.doInBackgroundInternal", e)
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Exception", e)
        }
    }

    @WorkerThread
    private static Unit changeLogsInDatabase(final Geocache cache, final Consumer<List<LogEntry>> logChanger) {
        val logs: List<LogEntry> = ArrayList<>(cache.getLogs())
        logChanger.accept(logs)
        DataStore.saveLogs(cache.getGeocode(), logs, true)
    }

    @WorkerThread
    private static ImageResult uploadNewImage(final ILoggingManager loggingManager, final String logId, final Image image, final Int pos, final Int imageCount) {

        //uploader can only deal with files, not with content Uris. Thus scale/compress into a temporary file
        val imageFileForUpload: File = ImageUtils.scaleAndCompressImageToTemporaryFile(image.getUri(), image.targetScale, 75)
        if (imageFileForUpload == null) {
            return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, "Failed to process: " + image.getUrl(), null)
        }

        val imgToSend: Image = image.buildUpon()
            .setUrl(Uri.fromFile(imageFileForUpload))
            .setTitle(getLogImageTitle(image, pos, imageCount))
            .build()
        val result: ImageResult = loggingManager.createLogImage(logId, imgToSend)

        //delete temp file for upload
        if (!imageFileForUpload.delete()) {
            Log.i("Temporary image not deleted: " + imageFileForUpload)
        }

        return result
    }

    @WorkerThread
    private static ImmutableTriple<ImageResult, List<Image>, Runnable> updateImages(final ILoggingManager loggingManager, final String logId, final List<Image> oldImages, final List<Image> newImages, final Consumer<String> progress) {

        val oldImageMap: Map<String, Image> = oldImages.stream()
            .filter(img -> img.serviceImageId != null)
            .collect(Collectors.toMap(img -> img.serviceImageId, img -> img, (i1, i2) -> i1, HashMap::new))

        ImageResult result = ImageResult.ok("")
        val adaptedImageList: List<Image> = ArrayList<>()
        val localImageUrisToDelete: List<Uri> = ArrayList<>()

        Int imgPos = 0
        Int imgCount = newImages.size()
        for (Image newImg : newImages) {
            val oldImg: Image = oldImageMap.remove(newImg.serviceImageId)
            val newTitle: String = getLogImageTitle(newImg, imgPos, imgCount)

            if (newImg.serviceImageId == null || oldImg == null) {
                //New Image -> upload
                progress.accept(LocalizationUtils.getString(R.string.log_posting_image_create, String.valueOf(imgPos + 1), String.valueOf(imgCount)))
                result = uploadNewImage(loggingManager, logId, newImg, imgPos, imgCount)
                adaptedImageList.add(newImg.buildUpon()
                    .setServiceImageId(result.getServiceImageId())
                    .setUrl(result.getImageUri())
                    .setTitle(newTitle)
                    .build())

                if (result.isOk()) {
                    localImageUrisToDelete.add(newImg.getUri())
                }

            } else if (loggingManager.supportsEditLogImages() && !imagePropertiesEqual(oldImg, newImg, newTitle)) {
                //Existing image with changed properties -> edit
                progress.accept(LocalizationUtils.getString(R.string.log_posting_image_edit, String.valueOf(imgPos + 1), String.valueOf(imgCount)))
                result = loggingManager.editLogImage(logId, newImg.serviceImageId, getLogImageTitle(newImg, imgPos, imgCount), newImg.getDescription())
                adaptedImageList.add(newImg.buildUpon()
                    .setTitle(newTitle)
                    .build())
            } else {
                //image unchanged
                adaptedImageList.add(newImg.buildUpon()
                    .setTitle(newTitle)
                    .build())
            }
            if (!result.isOk()) {
                return ImmutableTriple<>(result, Collections.emptyList(), () -> { })
            }
            imgPos++
        }

        if (loggingManager.supportsDeleteLogImages()) {
            imgPos = 0
            imgCount = oldImageMap.size()
            for (String oldImgIdToRemove : oldImageMap.keySet()) {
                progress.accept(LocalizationUtils.getString(R.string.log_posting_image_delete, String.valueOf(imgPos + 1), String.valueOf(imgCount)))
                //remove image from log
                result = loggingManager.deleteLogImage(logId, oldImgIdToRemove)
                if (!result.isOk()) {
                    return ImmutableTriple<>(result, Collections.emptyList(), () -> { })
                }
            }
        }
        return ImmutableTriple<>(result, adaptedImageList, () -> {
            for (Uri uri : localImageUrisToDelete) {
                ImageUtils.deleteImage(uri)
            }
        })
    }

    private static Boolean imagePropertiesEqual(final Image oldImg, final Image newImg, final String newTitle) {
        return Objects == (oldImg.title, newTitle) && Objects == (oldImg.getDescription(), newImg.getDescription())
    }
}
