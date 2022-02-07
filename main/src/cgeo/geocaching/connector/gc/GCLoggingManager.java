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
import cgeo.geocaching.loaders.UrlLoader;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import android.os.Bundle;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

class GCLoggingManager extends AbstractLoggingManager implements LoaderManager.LoaderCallbacks<String>, ILoggingWithFavorites {

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

    @Nullable
    @Override
    public Loader<String> onCreateLoader(final int arg0, final Bundle arg1) {
        if (!Settings.hasGCCredentials()) { // allow offline logging
            ActivityMixin.showToast(activity, activity.getString(R.string.err_login));
            //start the UrlLoader anyway, DON'T return null! (returning null will crash the activity, see #8761)
            //The activity logic will handle this as if it were a "normal" network connectivity failure
        }
        activity.onLoadStarted();
        return new UrlLoader(activity.getBaseContext(), "https://www.geocaching.com/play/geocache/" + cache.getGeocode() + "/log", null);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<String> arg0, final String page) {
        if (page == null) {
            hasLoaderError = true;
        } else {
            trackables = new ArrayList<>();
            try {
                final List<GCWebAPI.TrackableInventoryEntry> trackableInventoryItems = GCWebAPI.getTrackableInventory();
                if (trackableInventoryItems != null) {
                    for (final GCWebAPI.TrackableInventoryEntry entry : trackableInventoryItems) {
                        trackables.add(new TrackableLog(entry.referenceCode, entry.trackingNumber, entry.name, 0, 0, TrackableBrand.TRAVELBUG));
                    }
                }
                hasTrackableLoadError = false;
            } catch (final Exception e) {
                hasTrackableLoadError = true;
                Log.w("GCLoggingManager.onLoadFinished: getTrackableInventory", e);
            }

            possibleLogTypes = GCParser.parseTypes(page);

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
                premFavcount = GCWebAPI.getAvailableFavoritePoints(params.userInfo.referenceCode).blockingGet();
                hasFavPointLoadError = false;
            } catch (final Exception e) {
                hasFavPointLoadError = true;
                Log.w("GCLoggingManager.onLoadFinished: getAvailableFavoritePoints", e);
            }
            hasLoaderError = possibleLogTypes.isEmpty();
        }

        activity.onLoadFinished();
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<String> arg0) {
        // nothing to do
    }

    @Override
    public void init() {
        LoaderManager.getInstance(activity).initLoader(Loaders.LOGGING_GEOCHACHING.getLoaderId(), null, this);
    }

    @Override
    @NonNull
    public LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem) {

        try {
            final CheckBox favCheck = (CheckBox) activity.findViewById(R.id.favorite_check);
            final ImmutablePair<StatusCode, String> postResult = GCWebAPI.postLog(cache, logType,
                    date.getTime(), log, trackableLogs, favCheck.isChecked());

            if (postResult.left == StatusCode.NO_ERROR) {
                DataStore.saveVisitDate(cache.getGeocode(), date.getTime().getTime());

                if (logType.isFoundLog()) {
                    GCLogin.getInstance().increaseActualCachesFound();
                } else if (logType == LogType.TEMP_DISABLE_LISTING) {
                    cache.setDisabled(true);
                } else if (logType == LogType.ENABLE_LISTING) {
                    cache.setDisabled(false);
                }

                if (favCheck.isChecked()) {
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

}
