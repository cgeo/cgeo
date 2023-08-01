package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableLoggingManager;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.res.Resources;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class LogCacheTask extends AsyncTaskWithProgressText<String, StatusResult> {
    private final Resources res;
    private final LogCacheActivity.LogCacheTaskInterface taskInterface;

    private final Action1<StatusResult> onPostExecuteInternal;

    LogCacheTask(final Activity activity, final Resources res, final String progressMessage, final String title,
                 final LogCacheActivity.LogCacheTaskInterface taskInterface,
                 final Action1<StatusResult> onPostExecuteInternal) {
        super(activity, title, progressMessage);
        this.res = res;
        this.taskInterface = taskInterface;
        this.onPostExecuteInternal = onPostExecuteInternal;
    }

    @Override
    protected StatusResult doInBackgroundInternal(final String[] logTexts) {

        final String log = logTexts[0];
        final String logPwd = logTexts.length > 1 ? logTexts[1] : null;

        final ContextLogger cLog = new ContextLogger("LCA.Poster.doInBackgroundInternal(%s)", log);
        try {
            final IConnector cacheConnector = ConnectorFactory.getConnector(taskInterface.geocache);
            float logRating = 0;
            if (cacheConnector instanceof IVotingCapability) {
                logRating = getLogRating((IVotingCapability) cacheConnector);
            }

            final LogResult logResult;
            if (taskInterface.loggingManager instanceof ILoggingWithFavorites) {
                logResult = ((ILoggingWithFavorites) taskInterface.loggingManager).postLog(taskInterface.logType, taskInterface.date.getCalendar(),
                        log, logPwd, new ArrayList<>(taskInterface.trackables), taskInterface.reportProblemType, taskInterface.binding.favoriteCheck.isChecked(), logRating);
            } else {
                logResult = taskInterface.loggingManager.postLog(taskInterface.logType, taskInterface.date.getCalendar(),
                        log, logPwd, new ArrayList<>(taskInterface.trackables), taskInterface.reportProblemType, logRating);
            }

            ImageResult imageResult = null;
            if (logResult.getStatusCode() == StatusCode.NO_ERROR) {
                // update geocache in DB
                if (taskInterface.logType.isFoundLog()) {
                    taskInterface.geocache.setFound(true);
                    taskInterface.geocache.setVisitedDate(taskInterface.date.getDate().getTime());
                } else if (taskInterface.logType == LogType.DIDNT_FIND_IT) {
                    taskInterface.geocache.setDNF(true);
                    taskInterface.geocache.setVisitedDate(taskInterface.date.getDate().getTime());
                }
                DataStore.saveChangedCache(taskInterface.geocache);

                final LogEntry.Builder logBuilder = new LogEntry.Builder()
                        .setServiceLogId(logResult.getServiceLogId())
                        .setDate(taskInterface.date.getDate().getTime())
                        .setLogType(taskInterface.logType)
                        .setLog(log)
                        .setFriend(true);

                // login credentials may vary from actual username
                // Get correct author name from connector (if applicable)
                if (cacheConnector instanceof ILogin) {
                    final String username = ((ILogin) cacheConnector).getUserName();
                    if (StringUtils.isNotBlank(username)) {
                        logBuilder.setAuthor(username);
                    }
                }

                imageResult = postImages(logBuilder, logResult);
                storeLogInDatabase(logBuilder);
                postCacheRating(cacheConnector);
                postTrackables(log);
            }

            // if an image could not be uploaded, use its error as final state
            if (!isOkResult(imageResult)) {
                return imageResult;
            }
            return logResult;
        } catch (final RuntimeException e) {
            cLog.setException(e);
            Log.e("LogCacheActivity.Poster.doInBackgroundInternal", e);
        } finally {
            cLog.endLog();
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    private ImageResult postImages(final LogEntry.Builder logBuilder, final LogResult logResult) {
        ImageResult imageResult = null;
        // Posting image
        if (!taskInterface.imageListFragment.getImages().isEmpty()) {
            publishProgress(res.getString(R.string.log_posting_image));
            int pos = 0;
            for (Image img : taskInterface.imageListFragment.getImages()) {

                //uploader can only deal with files, not with content Uris. Thus scale/compress into a temporary file
                final File imageFileForUpload = ImageUtils.scaleAndCompressImageToTemporaryFile(img.getUri(), img.targetScale, 75);
                final Image imgToSend;
                if (imageFileForUpload == null) {
                    imgToSend = null;
                    imageResult = new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, img.getUrl());
                } else {
                    imgToSend = img.buildUpon().setUrl(Uri.fromFile(imageFileForUpload)).setTitle(taskInterface.imageListFragment.getImageTitle(img, pos++)).build();
                    imageResult = taskInterface.loggingManager.postLogImage(logResult.getLogId(), imgToSend);
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
                for (Image img : taskInterface.imageListFragment.getImages()) {
                    ImageUtils.deleteImage(img.getUri());
                }
            }
        }

        return imageResult;
    }

    private void storeLogInDatabase(final LogEntry.Builder logBuilder) {
        // update logs in DB
        final List<LogEntry> newLogs = new ArrayList<>(taskInterface.geocache.getLogs());
        final LogEntry logNow = logBuilder.build();
        newLogs.add(0, logNow);
        if (taskInterface.reportProblemType != ReportProblemType.NO_PROBLEM) {
            final LogEntry logProblem = logBuilder.setLog(res.getString(taskInterface.reportProblemType.textId)).setLogImages(Collections.emptyList()).setLogType(taskInterface.reportProblemType.logType).build();
            newLogs.add(0, logProblem);
        }
        DataStore.saveLogs(taskInterface.geocache.getGeocode(), newLogs, true);

        // update offline log in DB
        taskInterface.geocache.clearOfflineLog();

    }

    private float getLogRating(final IVotingCapability votingConnector) {
        if (votingConnector != null && votingConnector.supportsVoting(taskInterface.geocache) && votingConnector.isValidRating(taskInterface.cacheVotingBar.getRating())) {
            return taskInterface.cacheVotingBar.getRating();
        }
        return 0;
    }

    private void postCacheRating(final IConnector cacheConnector) {
        // Post cache rating
        if (cacheConnector instanceof IVotingCapability) {
            final IVotingCapability votingConnector = (IVotingCapability) cacheConnector;
            if (votingConnector.supportsVoting(taskInterface.geocache) && votingConnector.isValidRating(taskInterface.cacheVotingBar.getRating())) {
                publishProgress(res.getString(R.string.log_posting_vote));
                if (votingConnector.postVote(taskInterface.geocache, taskInterface.cacheVotingBar.getRating())) {
                    taskInterface.geocache.setMyVote(taskInterface.cacheVotingBar.getRating());
                    DataStore.saveChangedCache(taskInterface.geocache);
                } else {
                    ActivityMixin.showToast(activity, res.getString(R.string.err_vote_send_rating));
                }
            }
        }
    }

    private void postTrackables(final String log) {
        // Posting Generic Trackables
        for (final TrackableConnector connector : ConnectorFactory.getLoggableGenericTrackablesConnectors()) {
            final TrackableLoggingManager manager = connector.getTrackableLoggingManager((AbstractLoggingActivity) activity);
            if (manager != null) {
                // Filter trackables logs by action and brand
                final Set<TrackableLog> trackablesMoved = new HashSet<>();
                for (final TrackableLog trackableLog : taskInterface.trackables) {
                    if (trackableLog.action != LogTypeTrackable.DO_NOTHING && trackableLog.brand == connector.getBrand()) {
                        trackablesMoved.add(trackableLog);
                    }
                }

                // Posting trackables logs
                int trackableLogcounter = 1;
                for (final TrackableLog trackableLog : trackablesMoved) {
                    publishProgress(res.getString(R.string.log_posting_generic_trackable, trackableLog.brand.getLabel(), trackableLogcounter, trackablesMoved.size()));
                    manager.postLog(taskInterface.geocache, trackableLog, taskInterface.date.getCalendar(), log);
                    trackableLogcounter++;
                }
            }
        }
    }

    private boolean isOkResult(final ImageResult imageResult) {
        return imageResult == null || imageResult.getStatusCode() == StatusCode.NO_ERROR || imageResult.getStatusCode() == StatusCode.LOG_SAVED;
    }

    @Override
    protected void onPostExecuteInternal(final StatusResult statusResult) {
        onPostExecuteInternal.call(statusResult);
    }
}
