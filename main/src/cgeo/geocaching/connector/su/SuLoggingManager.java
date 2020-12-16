package cgeo.geocaching.connector.su;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ILoggingWithFavorites;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class SuLoggingManager extends AbstractLoggingManager implements LoaderManager.LoaderCallbacks<String>, ILoggingWithFavorites {

    @NonNull
    private final SuConnector connector;
    @NonNull
    private final Geocache cache;
    @NonNull
    private final LogCacheActivity activity;

    private boolean hasFavPointLoadError = true;
    private int recommendationsCount;

    SuLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final SuConnector connector, @NonNull final Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public void init() {
        LoaderManager.getInstance(activity).initLoader(Loaders.LOGGING_GEOCHACHING.getLoaderId(), null, this);
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem) {
        final LogResult result;
        final boolean addFavorite = ((CheckBox) activity.findViewById(R.id.favorite_check)).isChecked();
        try {
            result = SuApi.postLog(cache, logType, date, log, addFavorite);
        } catch (final SuApi.SuApiException e) {
            Log.e("Logging manager SuApi.postLog exception: ", e);
            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        }

        if (addFavorite) {
            cache.setFavorite(true);
            cache.setFavoritePoints(cache.getFavoritePoints() + 1);
        }
        return result;
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final Image image) {
        return SuApi.postImage(cache, image);
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes() {
        return connector.getPossibleLogTypes(cache);
    }

    @NonNull
    @Override
    public List<ReportProblemType> getReportProblemTypes(@NonNull final Geocache geocache) {
        return Collections.emptyList();
    }

    @Override
    @NonNull
    public Loader<String> onCreateLoader(final int id, final Bundle args) {
        activity.onLoadStarted();
        return new SuLoggingLoader(activity.getBaseContext());
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<String> loader, final String data) {
        // Download fav points
        recommendationsCount = SuApi.getAvailableRecommendations();
        hasFavPointLoadError = false;

        activity.onLoadFinished();
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<String> loader) {
        // nothing to do
    }

    @Override
    public int getFavoritePoints() {
        return (hasLoaderError() || hasFavPointLoadError()) ? 0 : recommendationsCount;
    }

    @Override
    public boolean hasFavPointLoadError() {
        return hasFavPointLoadError;
    }

    static class SuLoggingLoader extends AsyncTaskLoader<String> {
        SuLoggingLoader(final Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public String loadInBackground() {
            // XXX: We need this dummy Loader because in other case button "Send log" will complain
            // about "Downloading data in progress"
            return "nothing to load here";
        }
    }
}
