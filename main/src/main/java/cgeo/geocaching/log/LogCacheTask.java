package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableLoggingManager;
import cgeo.geocaching.databinding.LogcacheActivityBinding;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.CacheVotingBar;
import cgeo.geocaching.ui.DateTimeEditor;
import cgeo.geocaching.ui.ImageListFragment;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.res.Resources;
import android.net.Uri;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class LogCacheTask extends AsyncTaskWithProgressText<String, StatusCode> {
    private final Resources res;
    private final ILoggingManager loggingManager;

    private final Geocache cache;
    private final Set<TrackableLog> trackables;

    private final LogcacheActivityBinding binding;
    private final LogType logType;
    private final ReportProblemType reportProblemType;
    private final ImageListFragment imageListFragment;
    private final CacheVotingBar cacheVotingBar;

    private final DateTimeEditor date;

    private final Action1<StatusCode> onPostExecuteInternal;

    LogCacheTask(final Activity activity, final Resources res, final String progressMessage, final String title,
                 final ILoggingManager loggingManager,
                 final Geocache cache, final Set<TrackableLog> trackables,
                 final LogcacheActivityBinding binding, final LogType logType, final ReportProblemType reportProblemType, final ImageListFragment imageListFragment, final CacheVotingBar cacheVotingBar,
                 final DateTimeEditor date,
                 final Action1<StatusCode> onPostExecuteInternal) {
        super(activity, title, progressMessage);
        this.res = res;
        this.loggingManager = loggingManager;
        this.cache = cache;
        this.trackables = trackables;
        this.binding = binding;
        this.logType = logType;
        this.reportProblemType = reportProblemType;
        this.imageListFragment = imageListFragment;
        this.cacheVotingBar = cacheVotingBar;
        this.date = date;
        this.onPostExecuteInternal = onPostExecuteInternal;
    }

    @Override
    protected StatusCode doInBackgroundInternal(final String[] logTexts) {

        final String log = logTexts[0];
        final String logPwd = logTexts.length > 1 ? logTexts[1] : null;

        final ContextLogger cLog = new ContextLogger("LCA.Poster.doInBackgroundInternal(%s)", log);
        try {
            final LogResult logResult;
            if (loggingManager instanceof ILoggingWithFavorites) {
                logResult = ((ILoggingWithFavorites) loggingManager).postLog(logType, date.getCalendar(),
                        log, logPwd, new ArrayList<>(trackables), reportProblemType, binding.favoriteCheck.isChecked());
            } else {
                logResult = loggingManager.postLog(logType, date.getCalendar(),
                        log, logPwd, new ArrayList<>(trackables), reportProblemType);
            }
            ImageResult imageResult = null;

            if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                // update geocache in DB
                if (logType.isFoundLog()) {
                    cache.setFound(true);
                    cache.setVisitedDate(date.getDate().getTime());
                } else if (logType == LogType.DIDNT_FIND_IT) {
                    cache.setDNF(true);
                    cache.setVisitedDate(date.getDate().getTime());
                }
                DataStore.saveChangedCache(cache);

                final LogEntry.Builder logBuilder = new LogEntry.Builder()
                        .setServiceLogId(logResult.getServiceLogId())
                        .setDate(date.getDate().getTime())
                        .setLogType(logType)
                        .setLog(log)
                        .setFriend(true);

                // login credentials may vary from actual username
                // Get correct author name from connector (if applicable)
                final IConnector cacheConnector = ConnectorFactory.getConnector(cache);
                if (cacheConnector instanceof ILogin) {
                    final String username = ((ILogin) cacheConnector).getUserName();
                    if (StringUtils.isNotBlank(username)) {
                        logBuilder.setAuthor(username);
                    }
                }

                // Posting image
                if (!imageListFragment.getImages().isEmpty()) {
                    publishProgress(res.getString(R.string.log_posting_image));
                    int pos = 0;
                    for (Image img : imageListFragment.getImages()) {

                        //uploader can only deal with files, not with content Uris. Thus scale/compress into a temporary file
                        final File imageFileForUpload = ImageUtils.scaleAndCompressImageToTemporaryFile(img.getUri(), img.targetScale, 75);
                        final Image imgToSend;
                        if (imageFileForUpload == null) {
                            imgToSend = null;
                            imageResult = new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, img.getUrl());
                        } else {
                            imgToSend = img.buildUpon().setUrl(Uri.fromFile(imageFileForUpload)).setTitle(imageListFragment.getImageTitle(img, pos++)).build();
                            imageResult = loggingManager.postLogImage(logResult.getLogId(), imgToSend);
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
                        for (Image img : imageListFragment.getImages()) {
                            ImageUtils.deleteImage(img.getUri());
                        }
                    }
                }

                // update logs in DB
                final List<LogEntry> newLogs = new ArrayList<>(cache.getLogs());
                final LogEntry logNow = logBuilder.build();
                newLogs.add(0, logNow);
                if (reportProblemType != ReportProblemType.NO_PROBLEM) {
                    final LogEntry logProblem = logBuilder.setLog(res.getString(reportProblemType.textId)).setLogImages(Collections.emptyList()).setLogType(reportProblemType.logType).build();
                    newLogs.add(0, logProblem);
                }
                DataStore.saveLogs(cache.getGeocode(), newLogs, true);

                // update offline log in DB
                cache.clearOfflineLog();

                if (logType == LogType.FOUND_IT && binding.tweet.isChecked() && binding.tweet.getVisibility() == View.VISIBLE) {
                    publishProgress(res.getString(R.string.log_posting_twitter));
                    Twitter.postTweetCache(cache.getGeocode(), logNow);
                }

                // Post cache rating
                if (cacheConnector instanceof IVotingCapability) {
                    final IVotingCapability votingConnector = (IVotingCapability) cacheConnector;
                    if (votingConnector.supportsVoting(cache) && votingConnector.isValidRating(cacheVotingBar.getRating())) {
                        publishProgress(res.getString(R.string.log_posting_vote));
                        if (votingConnector.postVote(cache, cacheVotingBar.getRating())) {
                            cache.setMyVote(cacheVotingBar.getRating());
                            DataStore.saveChangedCache(cache);
                        } else {
                            ActivityMixin.showToast(activity, res.getString(R.string.err_vote_send_rating));
                        }
                    }
                }


                // Posting Generic Trackables
                for (final TrackableConnector connector : ConnectorFactory.getLoggableGenericTrackablesConnectors()) {
                    final TrackableLoggingManager manager = connector.getTrackableLoggingManager((AbstractLoggingActivity) activity);
                    if (manager != null) {
                        // Filter trackables logs by action and brand
                        final Set<TrackableLog> trackablesMoved = new HashSet<>();
                        for (final TrackableLog trackableLog : trackables) {
                            if (trackableLog.action != LogTypeTrackable.DO_NOTHING && trackableLog.brand == connector.getBrand()) {
                                trackablesMoved.add(trackableLog);
                            }
                        }

                        // Posting trackables logs
                        int trackableLogcounter = 1;
                        for (final TrackableLog trackableLog : trackablesMoved) {
                            publishProgress(res.getString(R.string.log_posting_generic_trackable, trackableLog.brand.getLabel(), trackableLogcounter, trackablesMoved.size()));
                            manager.postLog(cache, trackableLog, date.getCalendar(), log);
                            trackableLogcounter++;
                        }
                    }
                }
            }

            // if an image could not be uploaded, use its error as final state
            if (!isOkResult(imageResult)) {
                return imageResult.getPostResult();
            }
            return logResult.getPostLogResult();
        } catch (final RuntimeException e) {
            cLog.setException(e);
            Log.e("LogCacheActivity.Poster.doInBackgroundInternal", e);
        } finally {
            cLog.endLog();
        }

        return StatusCode.LOG_POST_ERROR;
    }

    private boolean isOkResult(final ImageResult imageResult) {
        return imageResult == null || imageResult.getPostResult() == StatusCode.NO_ERROR || imageResult.getPostResult() == StatusCode.LOG_SAVED;
    }

    @Override
    protected void onPostExecuteInternal(final StatusCode status) {
        onPostExecuteInternal.call(status);
    }
}
