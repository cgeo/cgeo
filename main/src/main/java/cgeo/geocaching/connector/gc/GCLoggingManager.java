package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.LastTrackableAction;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

class GCLoggingManager extends AbstractLoggingManager {

    private static final List<ReportProblemType> REPORT_PROBLEM_TYPES = Arrays.asList(ReportProblemType.LOG_FULL, ReportProblemType.DAMAGED, ReportProblemType.MISSING, ReportProblemType.ARCHIVE, ReportProblemType.OTHER);

    GCLoggingManager(final Geocache cache) {
        super(GCConnector.getInstance(), cache);
    }

    @WorkerThread
    @NonNull
    @Override
    public LogContextInfo getLogContextInfo(@Nullable final String serviceLogId) {
        final LogContextInfo result = new LogContextInfo(this, serviceLogId);
        if (!Settings.hasGCCredentials()) { // allow offline logging
            result.addError(R.string.err_login);
            return result;
        }

        final String url = "https://www.geocaching.com/play/geocache/" + getCache().getGeocode() + "/log";
        final String page;
        try {
            page = Network.getResponseData(Network.getRequest(url, null));
        } catch (final Exception e) {
            Log.w("HCLoggingManager: failed to retrieve log page data for '" + url + "'", e);
            result.setError();
            return result;
        }

        final List<LogType> possibleLogTypes = GCParser.parseTypes(page);
        result.setAvailableLogTypes(possibleLogTypes);

        try {
            final List<GCWebAPI.TrackableInventoryEntry> trackableInventoryItems = GCWebAPI.getTrackableInventory();
            final List<TrackableLog> trackables = CollectionStream.of(trackableInventoryItems).map(entry -> new TrackableLog(entry.referenceCode, entry.trackingNumber, entry.name, 0, 0, TrackableBrand.TRAVELBUG)).toList();
            result.setAvailableTrackables(trackables);
        } catch (final Exception e) {
            result.setError();
            Log.w("GCLoggingManager.onLoadFinished: getTrackableInventory", e);
        }



        // TODO: also parse ProblemLogTypes: logSettings.problemLogTypes.push(45);

        /* TODO: the GUID is not available in the new log page
        if (StringUtils.isBlank(cache.getGuid())) {
            // Acquire the cache GUID from the log page. This will not only complete the information in the database,
            // but also allow the user to post a rating using GCVote since it requires the GUID to do so.

            final String guid = TextUtils.getMatch(page, GCConstants.PATTERN_LOG_GUID, null);
            if (StringUtils.isNotBlank(guid)) {
                cache.setGuid(guid);
                DataStore.saveChangedCache(cache);
            } else {
                Log.w("Could not acquire GUID from log page for " + cache.getGeocode());
            }
        }*/

        try {
            final GCLogin.ServerParameters params = GCLogin.getInstance().getServerParameters();
            final Integer premFavcount = GCWebAPI.getAvailableFavoritePoints(params.userInfo.referenceCode).blockingGet();
            if (premFavcount != null && premFavcount >= 0) {
                result.setAvailableFavoritePoints(premFavcount);
            } else {
                result.setError();
            }
        } catch (final Exception e) {
            result.setError();
            Log.w("GCLoggingManager.onLoadFinished: getAvailableFavoritePoints", e);
        }

        return result;
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final LogEntry logEntry, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, final  boolean addToFavorites, final float rating) {

        final LogType logType = logEntry.logType;
        final String log = logEntry.log;
        final Date date = new Date(logEntry.date);
        final ReportProblemType reportProblem = logEntry.reportProblem;

        try {
            final ImmutablePair<StatusCode, String> postResult = GCWebAPI.postLog(getCache(), logType,
                date, log, trackableLogs, addToFavorites);
            for (TrackableLog trackableLog : trackableLogs) {
                LastTrackableAction.setAction(trackableLog);
            }

            final Geocache cache = getCache();

            if (postResult.left == StatusCode.NO_ERROR) {
                DataStore.saveVisitDate(cache.getGeocode(), date.getTime());

                if (logType.isFoundLog()) {
                    GCLogin.getInstance().increaseActualCachesFound();
                } else if (logType == LogType.TEMP_DISABLE_LISTING) {
                    cache.setDisabled(true);
                } else if (logType == LogType.ENABLE_LISTING) {
                    cache.setDisabled(false);
                }

                if (addToFavorites) {
                    cache.setFavorite(true);
                    cache.setFavoritePoints(cache.getFavoritePoints() + 1);
                }
            }

            if (reportProblem != ReportProblemType.NO_PROBLEM) {
                GCWebAPI.postLog(cache, reportProblem.logType, date, CgeoApplication.getInstance().getString(reportProblem.textId), Collections.emptyList(), false);
            }

            return new LogResult(postResult.left, postResult.right);
        } catch (final Exception e) {
            Log.e("GCLoggingManager.postLog", e);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");

    }

    @NonNull
    @Override
    public ImageResult createLogImage(@NonNull final String logId, @NonNull final Image image) {
        if (!image.isEmpty() && image.getFile() != null) {

            final ImmutablePair<StatusCode, String> imageResult = GCWebAPI.postLogImage(getCache().getGeocode(), logId, image);

            return new ImageResult(imageResult.left, imageResult.right, "");
        }

        return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR);
    }

    @Override
    public boolean supportsLogWithFavorite() {
        return true;
    }

    @NonNull
    @Override
    public List<ReportProblemType> getReportProblemTypes(@NonNull final Geocache geocache) {
        if (geocache.isArchived() || geocache.isOwner()) {
            return Collections.emptyList();
        }
        final List<ReportProblemType> possibleReportProblemTypes = new ArrayList<>();
        for (final ReportProblemType reportProblem : REPORT_PROBLEM_TYPES) {
            if ((!geocache.isEventCache() && !geocache.isDisabled()) || reportProblem == ReportProblemType.ARCHIVE) {
                possibleReportProblemTypes.add(reportProblem);
            }
        }
        return possibleReportProblemTypes;
    }

}
