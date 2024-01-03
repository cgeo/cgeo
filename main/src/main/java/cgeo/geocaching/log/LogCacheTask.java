package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableLoggingManager;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.LastTrackableAction;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class LogCacheTask extends AsyncTaskWithProgressText<String, StatusResult> {
    private final LogCacheActivity.LogCacheTaskInterface taskInterface;

    private final Action1<StatusResult> onPostExecuteInternal;

    LogCacheTask(final Activity activity, final String progressMessage, final String title,
                 final LogCacheActivity.LogCacheTaskInterface taskInterface,
                 final Action1<StatusResult> onPostExecuteInternal) {
        super(activity, title, progressMessage);
        this.taskInterface = taskInterface;
        this.onPostExecuteInternal = onPostExecuteInternal;
    }

    @Override
    protected StatusResult doInBackgroundInternal(final String[] logTexts) {

        final ContextLogger cLog = new ContextLogger("LogCacheTask.doInBackgroundInternal(%s)", taskInterface.logEntry);
        try {
            final ILoggingManager loggingManager = taskInterface.loggingManager;
            final Geocache cache = loggingManager.getCache();
            final IConnector cacheConnector = loggingManager.getConnector();
            final OfflineLogEntry logEntry = (OfflineLogEntry) taskInterface.logEntry;

            final LogResult logResult = taskInterface.loggingManager.createLog(logEntry, taskInterface.inventory);
                //logEntry, taskInterface.password, new ArrayList<>(taskInterface.trackables), taskInterface.addToFavorite, logRating);

            if (logEntry.reportProblem != ReportProblemType.NO_PROBLEM && !taskInterface.loggingManager.canLogReportType(logEntry.reportProblem)) {
                final OfflineLogEntry reportProblemEntry = new OfflineLogEntry.Builder<>()
                    .setLogType(logEntry.reportProblem.logType)
                    .setDate(logEntry.date)
                    .setLog(LocalizationUtils.getString(logEntry.reportProblem.textId))
                    .setPassword(logEntry.password)
                    .build();
                taskInterface.loggingManager.createLog(reportProblemEntry, null);
            }

            ImageResult imageResult = null;
            if (logResult.isOk()) {

                //update trackable actions
                if (loggingManager.supportsLogWithTrackables()) {
                    for (Map.Entry<String, LogTypeTrackable> entry : logEntry.inventoryActions.entrySet()) {
                        LastTrackableAction.setAction(entry.getKey(), entry.getValue());
                    }
                }

                //update "found" counter
                if (logEntry.logType.isFoundLog() && cacheConnector instanceof ILogin) {
                    ((ILogin) cacheConnector).increaseCachesFound(1);
                }

                //update cache state
                if (logEntry.logType == LogType.TEMP_DISABLE_LISTING) {
                    cache.setDisabled(true);
                } else if (logEntry.logType == LogType.ENABLE_LISTING) {
                    cache.setDisabled(false);
                }

                //update favorites
                if (loggingManager.supportsLogWithFavorite() && logEntry.favorite) {
                    cache.setFavorite(true);
                    cache.setFavoritePoints(cache.getFavoritePoints() + 1);
                }

                // update geocache in DB
                if (logEntry.logType.isFoundLog()) {
                    cache.setFound(true);
                    cache.setVisitedDate(logEntry.date);
                } else if (logEntry.logType == LogType.DIDNT_FIND_IT) {
                    cache.setDNF(true);
                    cache.setVisitedDate(logEntry.date);
                }
                DataStore.saveChangedCache(cache);

                final LogEntry.Builder<?> logBuilder = new LogEntry.Builder<>()
                        .setServiceLogId(logResult.getServiceLogId())
                        .setDate(logEntry.date)
                        .setLogType(logEntry.logType)
                        .setLog(logEntry.log)
                        .setFriend(true);

                // login credentials may vary from actual username
                // Get correct author name from connector (if applicable)
                if (cacheConnector instanceof ILogin) {
                    final String username = ((ILogin) cacheConnector).getUserName();
                    if (StringUtils.isNotBlank(username)) {
                        logBuilder.setAuthor(username);
                    }
                }

                imageResult = postImages(logBuilder, logResult, logEntry.logImages);

                storeLogInDatabase(logBuilder, cache, logEntry);
                postCacheRating(cacheConnector, cache, logEntry.rating);
                postGenericIntentoryLogs(cache, logEntry, taskInterface.inventory.values());
            }

            // if an image could not be uploaded, use its error as final state
            if (!isOkResult(imageResult)) {
                return imageResult;
            }
            return logResult;
        } catch (final RuntimeException e) {
            cLog.setException(e);
            Log.e("LogCacheActivity.Poster.doInBackgroundInternal", e);
            return LogResult.error(StatusCode.LOG_POST_ERROR, "LogCacheActivity.Poster.doInBackgroundInternal", e);
        } finally {
            cLog.endLog();
        }
    }

    private ImageResult postImages(final LogEntry.Builder logBuilder, final LogResult logResult, final List<Image> images) {
        ImageResult imageResult = null;
        // Posting image
        if (!images.isEmpty()) {
            publishProgress(LocalizationUtils.getString(R.string.log_posting_image));
            int pos = 0;
            for (Image img : images) {

                //uploader can only deal with files, not with content Uris. Thus scale/compress into a temporary file
                final File imageFileForUpload = ImageUtils.scaleAndCompressImageToTemporaryFile(img.getUri(), img.targetScale, 75);
                final Image imgToSend;
                if (imageFileForUpload == null) {
                    imgToSend = null;
                    imageResult = ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, "Failed to process: " + img.getUrl(), null);
                } else {
                    imgToSend = img.buildUpon()
                        .setUrl(Uri.fromFile(imageFileForUpload))
                        .setTitle(taskInterface.imageTitleCreator.apply(img, pos++))
                        .build();
                    imageResult = taskInterface.loggingManager.createLogImage(logResult.getLogId(), imgToSend);
                }
                if (!isOkResult(imageResult)) {
                    break;
                }

                final String uploadedImageUrl = imageResult.getImageUri();
                if (StringUtils.isNotEmpty(uploadedImageUrl)) {
                    logBuilder.addLogImage(imgToSend.buildUpon()
                            .setUrl(uploadedImageUrl)
                            .build());
                }
                //delete temp file for upload
                if (!imageFileForUpload.delete()) {
                    Log.i("Temporary image not deleted: " + imageFileForUpload);
                }
            }

            if (isOkResult(imageResult)) {
                //delete all images in list (this will work for legacy images)
                for (Image img : images) {
                    ImageUtils.deleteImage(img.getUri());
                }
            }
        }

        return imageResult;
    }

    private static void storeLogInDatabase(final LogEntry.Builder logBuilder, final Geocache cache, final LogEntry logEntry) {
        // update logs in DB
        final List<LogEntry> newLogs = new ArrayList<>(cache.getLogs());
        final LogEntry logNow = logBuilder.build();
        newLogs.add(0, logNow);
        if (logEntry.reportProblem != ReportProblemType.NO_PROBLEM) {
            final LogEntry logProblem = logBuilder.setLog(LocalizationUtils.getString(logEntry.reportProblem.textId)).setLogImages(Collections.emptyList()).setLogType(logEntry.reportProblem.logType).build();
            newLogs.add(0, logProblem);
        }
        DataStore.saveLogs(cache.getGeocode(), newLogs, true);

        // update offline log in DB
        cache.clearOfflineLog(null);

    }

    private void postCacheRating(final IConnector cacheConnector, final Geocache cache, final float rating) {
        // Post cache rating
        if (cacheConnector instanceof IVotingCapability) {
            final IVotingCapability votingConnector = (IVotingCapability) cacheConnector;
            if (votingConnector.supportsVoting(cache) && votingConnector.isValidRating(rating)) {
                publishProgress(LocalizationUtils.getString(R.string.log_posting_vote));
                if (votingConnector.postVote(cache, rating)) {
                    cache.setMyVote(rating);
                    DataStore.saveChangedCache(cache);
                } else {
                    ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.err_vote_send_rating));
                }
            }
        }
    }

    private void postGenericIntentoryLogs(final Geocache cache, final OfflineLogEntry logEntry, final Collection<Trackable> inventory) {
        // Posting Generic Trackables
        for (final TrackableConnector connector : ConnectorFactory.getLoggableGenericTrackablesConnectors()) {
            // Filter trackables logs by action and brand
            final Set<Trackable> trackablesMoved = new HashSet<>();
            for (final Trackable trackable : inventory) {
                final LogTypeTrackable action = logEntry.inventoryActions.get(trackable.getGeocode());
                if (action != null && action != LogTypeTrackable.DO_NOTHING && trackable.getBrand() == connector.getBrand()) {
                    trackablesMoved.add(trackable);
                }
            }

            // Posting trackables logs
            int trackableLogcounter = 1;
            for (final Trackable trackable : trackablesMoved) {
                final TrackableLoggingManager manager = connector.getTrackableLoggingManager(trackable.getGeocode());
                if (manager != null) {
                    publishProgress(LocalizationUtils.getString(R.string.log_posting_generic_trackable, trackable.getBrand().getLabel(), trackableLogcounter, trackablesMoved.size()));
                    final TrackableLogEntry tle = TrackableLogEntry.of(trackable);
                    tle.setAction(logEntry.inventoryActions.get(trackable.getGeocode()))
                        .setLog(logEntry.log)
                        .setDate(logEntry.getDate());
                    manager.postLog(cache, tle);
                    trackableLogcounter++;
                }
            }

        }
    }

    private boolean isOkResult(final ImageResult imageResult) {
        return imageResult == null || imageResult.isOk();
    }

    @Override
    protected void onPostExecuteInternal(final StatusResult statusResult) {
        onPostExecuteInternal.call(statusResult);
    }
}
