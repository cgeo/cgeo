package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class OkapiLoggingManager extends AbstractLoggingManager implements LoaderManager.LoaderCallbacks<OkapiClient.InstallationInformation> {

    @NonNull
    private final OCApiLiveConnector connector;
    @NonNull
    private final Geocache cache;
    @NonNull
    private final LogCacheActivity activity;
    private boolean hasLoaderError = true;
    private OkapiClient.InstallationInformation installationInformation;

    public OkapiLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final OCApiLiveConnector connector, @NonNull final Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public void init() {
        LoaderManager.getInstance(activity).initLoader(Loaders.LOGGING_GEOCHACHING.getLoaderId(), null, this);
    }

    @NonNull
    @Override
    public Loader<OkapiClient.InstallationInformation> onCreateLoader(final int arg0, final Bundle arg1) {
        activity.onLoadStarted();
        return new AsyncTaskLoader<OkapiClient.InstallationInformation>(activity.getBaseContext()) {
            @Override
            protected void onStartLoading() {
                forceLoad();
            }

            @Override
            public OkapiClient.InstallationInformation loadInBackground() {
                return OkapiClient.getInstallationInformation(connector);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<OkapiClient.InstallationInformation> loader, final OkapiClient.InstallationInformation data) {
        if (data == null) {
            hasLoaderError = true;
        } else {
            installationInformation = data;
            if (connector.isLoggedIn()) {
                hasLoaderError = false;
            }
        }
        activity.onLoadFinished();
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<OkapiClient.InstallationInformation> loader) {
        // nothing to do
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem) {
        final LogResult result = OkapiClient.postLog(cache, logType, date, log, logPassword, connector, reportProblem);
        connector.login();
        return result;
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final Image image) {
        return OkapiClient.postLogImage(logId, image, connector);
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes() {
        if (hasLoaderError) {
            return Collections.emptyList();
        }
        return connector.getPossibleLogTypes(cache);
    }

    @Override
    public boolean hasLoaderError() {
        return hasLoaderError;
    }

    @Override
    public Long getMaxImageUploadSize() {
        return installationInformation != null ? installationInformation.imageMaxUploadSize : null;
    }

    @Override
    public boolean isImageCaptionMandatory() {
        return true;
    }

    @NonNull
    @Override
    public List<ReportProblemType> getReportProblemTypes(@NonNull final Geocache geocache) {
        if (geocache.isEventCache()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(ReportProblemType.NEEDS_MAINTENANCE);
    }

}
