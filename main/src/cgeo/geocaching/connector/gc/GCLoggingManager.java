package cgeo.geocaching.connector.gc;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.loaders.UrlLoader;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.Nullable;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

class GCLoggingManager extends AbstractLoggingManager implements LoaderManager.LoaderCallbacks<String> {

    private final LogCacheActivity activity;
    private final Geocache cache;

    private String[] viewstates;
    private List<TrackableLog> trackables;
    private List<LogType> possibleLogTypes;
    private boolean hasLoaderError = true;

    GCLoggingManager(final LogCacheActivity activity, final Geocache cache) {
        this.activity = activity;
        this.cache = cache;
    }

    @Nullable
    @Override
    public Loader<String> onCreateLoader(final int arg0, final Bundle arg1) {
        if (!Settings.isLogin()) { // allow offline logging
            ActivityMixin.showToast(activity, activity.getResources().getString(R.string.err_login));
            return null;
        }
        return new UrlLoader(activity.getBaseContext(), "http://www.geocaching.com/seek/log.aspx", new Parameters("ID", cache.getCacheId()));
    }

    @Override
    public void onLoadFinished(final Loader<String> arg0, final String page) {
        if (page == null) {
            hasLoaderError = true;
        } else {
            viewstates = GCLogin.getViewstates(page);
            trackables = GCParser.parseTrackableLog(page);
            possibleLogTypes = GCParser.parseTypes(page);
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
            }

            hasLoaderError = possibleLogTypes.isEmpty();
        }

        activity.onLoadFinished();
    }

    @Override
    public void onLoaderReset(final Loader<String> arg0) {
        // nothing to do
    }

    @Override
    public void init() {
        activity.getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public LogResult postLog(final LogType logType, final Calendar date, final String log, final String logPassword, final List<TrackableLog> trackableLogs) {

        try {
            final ImmutablePair<StatusCode, String> postResult = GCParser.postLog(cache.getGeocode(), cache.getCacheId(), viewstates, logType,
                    date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE),
                    log, trackableLogs);

            if (postResult.left == StatusCode.NO_ERROR) {
                if (logType == LogType.TEMP_DISABLE_LISTING) {
                    cache.setDisabled(true);
                } else if (logType == LogType.ENABLE_LISTING) {
                    cache.setDisabled(false);
                }
            }
            return new LogResult(postResult.left, postResult.right);
        } catch (final Exception e) {
            Log.e("GCLoggingManager.postLog", e);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public ImageResult postLogImage(final String logId, final String imageCaption, final String imageDescription, final Uri imageUri) {

        if (StringUtils.isNotBlank(imageUri.getPath())) {

            final ImmutablePair<StatusCode, String> imageResult = GCParser.uploadLogImage(logId, imageCaption, imageDescription, imageUri);

            return new ImageResult(imageResult.left, imageResult.right);
        }

        return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
    }

    @Override
    public boolean hasLoaderError() {
        return hasLoaderError;
    }

    @Override
    public List<TrackableLog> getTrackables() {
        if (hasLoaderError) {
            return Collections.emptyList();
        }
        return trackables;
    }

    @Override
    public List<LogType> getPossibleLogTypes() {
        if (hasLoaderError) {
            return Collections.emptyList();
        }
        return possibleLogTypes;
    }

}
