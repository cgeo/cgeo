package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.AbstractLoggingActivity;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.extension.LastTrackableAction;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TravelBugLoggingManager extends AbstractTrackableLoggingManager {

    private final AbstractLoggingActivity activity;
    private String guid;

    private boolean hasLoaderError = true;
    private String[] viewstates = null;
    private List<LogTypeTrackable> possibleLogTypesTrackable;

    public TravelBugLoggingManager(final AbstractLoggingActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public List<LogTypeTrackable> loadInBackground() {
        if (!Settings.hasGCCredentials()) { // allow offline logging
            ActivityMixin.showToast(activity, activity.getString(R.string.err_login));
            return null;
        }

        final String page = TravelBugConnector.getTravelbugViewstates(guid);

        if (page == null) {
            activity.showToast(activity.getString(R.string.err_log_load_data));
            hasLoaderError = true;
        } else {
            viewstates = GCLogin.getViewstates(page);
            possibleLogTypesTrackable = GCParser.parseLogTypesTrackables(page);
            hasLoaderError = possibleLogTypesTrackable.isEmpty();
        }
        return possibleLogTypesTrackable;
    }

    @Override
    public boolean postReady() {
        return !hasLoaderError;
    }

    @Override
    public LogResult postLog(final Geocache cache, final TrackableLog trackableLog, final Calendar date, final String log) {
        // 'cache' is not used here, but it is for GeokretyLoggingManager
        try {
            LastTrackableAction.setAction(trackableLog);
            final StatusCode status = GCParser.postLogTrackable(
                    guid,
                    trackableLog.trackCode,
                    viewstates,
                    trackableLog.action,
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH) + 1,
                    date.get(Calendar.DATE),
                    log);

            return new LogResult(status, "");
        } catch (final Exception e) {
            Log.e("TrackableLoggingManager.postLog", e);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    @Nullable
    public ImageResult postLogImage(final String logId, final Image image) {
        return null;
    }

    @Override
    @NonNull
    public List<LogTypeTrackable> getPossibleLogTypesTrackable() {
        if (hasLoaderError) {
            final List<LogTypeTrackable> logTypes = new ArrayList<>();
            logTypes.add(LogTypeTrackable.RETRIEVED_IT);
            logTypes.add(LogTypeTrackable.GRABBED_IT);
            logTypes.add(LogTypeTrackable.NOTE);
            logTypes.add(LogTypeTrackable.DISCOVERED_IT);
            return logTypes;
        }
        return possibleLogTypesTrackable;
    }

    @Override
    public boolean canLogTime() {
        return false;
    }

    @Override
    public boolean canLogCoordinates() {
        return false;
    }

    @Override
    public void setGuid(final String guid) {
        this.guid = guid;
    }
}
