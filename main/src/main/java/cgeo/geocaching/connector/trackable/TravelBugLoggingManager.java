package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.gc.GCLogAPI;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TravelBugLoggingManager extends AbstractTrackableLoggingManager {

    public TravelBugLoggingManager(final String tbCode) {
        super(tbCode);
    }

    @WorkerThread
    @NonNull
    @Override
    public List<LogTypeTrackable> getPossibleLogTypesTrackableOnline() {

        final String trackableCode = getTrackableCode();

        if (!Settings.hasGCCredentials()) { // allow offline logging
            ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_login));
            return Collections.emptyList();
        }

        final String url = GCLogAPI.getUrlForNewTrackableLog(trackableCode);
        String page = null;
        try {
            page = Network.getResponseData(Network.getRequest(url, null));
        } catch (final Exception e) {
            Log.w("TBLoggingManager: failed to retrieve trackable log page data for '" + url + "'", e);
        }

        if (page == null) {
            ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.err_log_load_data));
            return Collections.emptyList();
        }

        return GCParser.parseLogTypesTrackables(page);

    }

    @Override
    public LogResult postLog(final Geocache cache, final TrackableLogEntry trackableLog) {
        // 'cache' is not used here, but it is for GeokretyLoggingManager
        return GCLogAPI.createLogTrackable(trackableLog, trackableLog.getDate(), trackableLog.getLog());
    }

    @Override
    @NonNull
    public List<LogTypeTrackable> getPossibleLogTypesTrackable() {
        final List<LogTypeTrackable> logTypes = new ArrayList<>();
        logTypes.add(LogTypeTrackable.RETRIEVED_IT);
        logTypes.add(LogTypeTrackable.GRABBED_IT);
        logTypes.add(LogTypeTrackable.NOTE);
        logTypes.add(LogTypeTrackable.DISCOVERED_IT);
        return logTypes;
    }

    @Override
    public boolean canLogTime() {
        return false;
    }

    @Override
    public boolean canLogCoordinates() {
        return false;
    }

}
