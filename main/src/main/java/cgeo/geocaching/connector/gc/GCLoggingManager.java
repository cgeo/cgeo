package cgeo.geocaching.connector.gc;

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
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class GCLoggingManager extends AbstractLoggingManager {

    private static final List<ReportProblemType> REPORT_PROBLEM_TYPES = Arrays.asList(ReportProblemType.LOG_FULL, ReportProblemType.DAMAGED, ReportProblemType.MISSING, ReportProblemType.ARCHIVE, ReportProblemType.OTHER);

    GCLoggingManager(final Geocache cache) {
        super(GCConnector.getInstance(), cache);
    }

    @NonNull
    @Override
    public GCConnector getConnector() {
        return (GCConnector) super.getConnector();
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

        final Geocache cache = getCache();

        try {
            return GCLogAPI.createLog(cache.getGeocode(), logEntry, trackableLogs, addToFavorites);
        } catch (final Exception e) {
            Log.e("GCLoggingManager.createLog", e);
            return LogResult.error(StatusCode.LOG_POST_ERROR, "GCLoggingManager.createLog", e);
        }
    }

    @NonNull
    @Override
    public LogResult editLog(@NonNull final LogEntry newEntry) {
        return GCLogAPI.editLog(getCache().getGeocode(), newEntry, Collections.emptyList(), false);
    }

    @NonNull
    @Override
    public LogResult deleteLog(@NonNull final LogEntry newEntry) {
        return GCLogAPI.deleteLog(newEntry.serviceLogId);
    }

    @NonNull
    @Override
    public ImageResult createLogImage(@NonNull final String logId, @NonNull final Image image) {
        if (!image.isEmpty() && image.getFile() != null) {
            return GCLogAPI.addLogImage(logId, image);
        }
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, "No valid image:" + image, null);
    }

    @NonNull
    @Override
    public ImageResult editLogImage(@NonNull final String logId, @NonNull final String serviceImageId, @Nullable final String title, @Nullable final String description) {
        return GCLogAPI.editLogImageData(logId, serviceImageId, title, description);
    }

    @NonNull
    @Override
    public ImageResult deleteLogImage(@NonNull final String logId, @NonNull final String serviceImageId) {
        return GCLogAPI.deleteLogImage(logId, serviceImageId);
    }

    @Override
    public boolean supportsEditLogImages() {
        return true;
    }

    @Override
    public boolean supportsDeleteLogImages() {
        return true;
    }



    @Override
    public boolean supportsLogWithFavorite() {
        return true;
    }

    @Override
    public boolean supportsLogWithTrackables() {
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
