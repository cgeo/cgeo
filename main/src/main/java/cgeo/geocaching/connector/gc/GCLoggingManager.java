package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogCacheActivity;
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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

class GCLoggingManager extends AbstractLoggingManager implements LoaderManager.LoaderCallbacks<GCLoggingManager.Result>, ILoggingWithFavorites {

    private static final List<ReportProblemType> REPORT_PROBLEM_TYPES = Arrays.asList(ReportProblemType.LOG_FULL, ReportProblemType.DAMAGED, ReportProblemType.MISSING, ReportProblemType.ARCHIVE, ReportProblemType.OTHER);
    private final LogCacheActivity activity;
    private final Geocache cache;
    @NonNull
    private List<TrackableLog> trackables = Collections.emptyList();
    private List<LogType> possibleLogTypes;
    private boolean hasLoaderError = true;
    private boolean hasTrackableLoadError = true;
    private boolean hasFavPointLoadError = true;
    private int premFavcount;

    GCLoggingManager(final LogCacheActivity activity, final Geocache cache) {
        this.activity = activity;
        this.cache = cache;
    }

    @NonNull
    @Override
    public Loader<GCLoggingManager.Result> onCreateLoader(final int arg0, final Bundle arg1) {
        if (!Settings.hasGCCredentials()) { // allow offline logging
            ActivityMixin.showToast(activity, activity.getString(R.string.err_login));
            //start the UrlLoader anyway, DON'T return null! (returning null will crash the activity, see #8761)
            //The activity logic will handle this as if it were a "normal" network connectivity failure
        }
        activity.onLoadStarted();

        final String url = "https://www.geocaching.com/play/geocache/" + cache.getGeocode() + "/log";
        return new AsyncTaskLoader<Result>(activity.getBaseContext()) {
            @Override
            protected void onStartLoading() {
                forceLoad();
            }

            @Nullable
            @Override
            public Result loadInBackground() {
                final String page;
                try {
                    page = Network.getResponseData(Network.getRequest(url, null));
                } catch (final Exception e) {
                    Log.w("UrlLoader.loadInBackground", e);
                    return null;
                }

                @Nullable
                List<TrackableLog> trackables = null;
                try {
                    final List<GCWebAPI.TrackableInventoryEntry> trackableInventoryItems = GCWebAPI.getTrackableInventory();
                    trackables = CollectionStream.of(trackableInventoryItems).map(entry -> new TrackableLog(entry.referenceCode, entry.trackingNumber, entry.name, 0, 0, TrackableBrand.TRAVELBUG)).toList();
                } catch (final Exception e) {
                    Log.w("GCLoggingManager.onLoadFinished: getTrackableInventory", e);
                }

                final List<LogType> possibleLogTypes = GCParser.parseTypes(page);

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

                Integer premFavcount = null;
                try {
                    final GCLogin.ServerParameters params = GCLogin.getInstance().getServerParameters();
                    premFavcount = GCWebAPI.getAvailableFavoritePoints(params.userInfo.referenceCode).blockingGet();
                } catch (final Exception e) {
                    Log.w("GCLoggingManager.onLoadFinished: getAvailableFavoritePoints", e);
                }

                return new Result(trackables, possibleLogTypes, premFavcount);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<GCLoggingManager.Result> loader, final GCLoggingManager.Result result) {
        if (result == null) {
            hasLoaderError = true;
        } else {
            if (result.trackables != null) {
                trackables = result.trackables;
                hasTrackableLoadError = false;
            } else {
                hasTrackableLoadError = true;
            }

            possibleLogTypes = result.possibleLogTypes;
            hasLoaderError = possibleLogTypes.isEmpty();

            if (result.premFavcount != null) {
                premFavcount = result.premFavcount;
                hasFavPointLoadError = false;
            } else {
                hasFavPointLoadError = true;
            }
        }

        activity.onLoadFinished();
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<Result> loader) {
        // nothing to do
    }

    @Override
    public void init() {
        LoaderManager.getInstance(activity).initLoader(Loaders.LOGGING_GEOCHACHING.getLoaderId(), null, this);
    }

    @NonNull
    @Override
    public LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem) {
        return postLog(logType, date, log, logPassword, trackableLogs, reportProblem, false);
    }

    @Override
    @NonNull
    public LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem, final boolean addToFavorites) {

        try {
            final ImmutablePair<StatusCode, String> postResult = GCWebAPI.postLog(cache, logType,
                    date.getTime(), log, trackableLogs, addToFavorites);
            for (TrackableLog trackableLog : trackableLogs) {
                LastTrackableAction.setAction(trackableLog);
            }

            if (postResult.left == StatusCode.NO_ERROR) {
                DataStore.saveVisitDate(cache.getGeocode(), date.getTime().getTime());

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
                GCWebAPI.postLog(cache, reportProblem.logType, date.getTime(), CgeoApplication.getInstance().getString(reportProblem.textId), Collections.emptyList(), false);
            }

            return new LogResult(postResult.left, postResult.right);
        } catch (final Exception e) {
            Log.e("GCLoggingManager.postLog", e);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    @NonNull
    public ImageResult postLogImage(final String logId, final Image image) {

        if (!image.isEmpty()) {

            final ImmutablePair<StatusCode, String> imageResult = GCWebAPI.postLogImage(cache.getGeocode(), logId, image);

            return new ImageResult(imageResult.left, imageResult.right);
        }

        return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
    }

    @Override
    public boolean hasLoaderError() {
        return hasLoaderError;
    }

    @Override
    public boolean hasTrackableLoadError() {
        return hasTrackableLoadError;
    }

    @Override
    public boolean hasFavPointLoadError() {
        return hasFavPointLoadError;
    }

    @Override
    @NonNull
    public List<TrackableLog> getTrackables() {
        if (hasLoaderError || hasTrackableLoadError) {
            return Collections.emptyList();
        }
        return trackables;
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes() {
        if (hasLoaderError) {
            return Collections.emptyList();
        }
        return possibleLogTypes;
    }

    @Override
    public int getFavoritePoints() {
        return (hasLoaderError || hasFavPointLoadError) ? 0 : premFavcount;
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

    public static class Result {
        @Nullable
        public final List<TrackableLog> trackables;
        @NonNull
        public final List<LogType> possibleLogTypes;
        @Nullable
        public final Integer premFavcount;

        Result(@Nullable final List<TrackableLog> trackables, @NonNull final List<LogType> possibleLogTypes, @Nullable final Integer premFavcount) {
            this.trackables = trackables;
            this.possibleLogTypes = possibleLogTypes;
            this.premFavcount = premFavcount;
        }
    }
}
