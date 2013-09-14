package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.loaders.UrlLoader;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

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

public class GCLoggingManager implements ILoggingManager, LoaderManager.LoaderCallbacks<String> {

    private final LogCacheActivity activity;
    private final Geocache cache;

    private String[] viewstates;
    private List<TrackableLog> trackables;
    private List<LogType> possibleLogTypes;
    private boolean hasLoaderError = true;

    public GCLoggingManager(final LogCacheActivity activity, final Geocache cache) {
        this.activity = activity;
        this.cache = cache;
    }

    @Nullable
    @Override
    public Loader<String> onCreateLoader(int arg0, Bundle arg1) {
        if (!Settings.isLogin()) { // allow offline logging
            ActivityMixin.showToast(activity, activity.getResources().getString(R.string.err_login));
            return null;
        }
        return new UrlLoader(activity.getBaseContext(), "http://www.geocaching.com/seek/log.aspx", new Parameters("ID", cache.getCacheId()));
    }

    @Override
    public void onLoadFinished(Loader<String> arg0, String page) {

        if (page == null) {
            hasLoaderError = true;
        } else {

            viewstates = Login.getViewstates(page);
            trackables = GCParser.parseTrackableLog(page);
            possibleLogTypes = GCParser.parseTypes(page);

            hasLoaderError = possibleLogTypes.isEmpty();
        }

        activity.onLoadFinished();
    }

    @Override
    public void onLoaderReset(Loader<String> arg0) {
        // nothing to do
    }

    @Override
    public void init() {
        activity.getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public LogResult postLog(Geocache cache, LogType logType, Calendar date, String log, String logPassword, List<TrackableLog> trackableLogs) {

        try {
            final ImmutablePair<StatusCode, String> postResult = GCParser.postLog(cache.getGeocode(), cache.getCacheId(), viewstates, logType,
                    date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE),
                    log, trackableLogs);

            return new LogResult(postResult.left, postResult.right);
        } catch (Exception e) {
            Log.e("GCLoggingManager.postLog", e);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public ImageResult postLogImage(String logId, String imageCaption, String imageDescription, Uri imageUri) {

        if (StringUtils.isNotBlank(imageUri.getPath())) {

            ImmutablePair<StatusCode, String> imageResult = GCParser.uploadLogImage(logId, imageCaption, imageDescription, imageUri);

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
