package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

public class GeokretyLoggingManager extends AbstractTrackableLoggingManager {


    public GeokretyLoggingManager(final String tbCode) {
        super(tbCode);
    }

    @Override
    public LogResult postLog(final Geocache cache, final TrackableLogEntry trackableLog) {
        try {
            return GeokretyConnector.postLogTrackable(cache, trackableLog);
       } catch (final Exception e) {
            Log.e("GeokretyLoggingManager.postLog", e);
        }

        return LogResult.error(StatusCode.LOG_POST_ERROR);
    }

    @WorkerThread
    @Override
    @NonNull
    public List<LogTypeTrackable> getPossibleLogTypesTrackable() {
        final List<LogTypeTrackable> list = new ArrayList<>();
        list.add(LogTypeTrackable.RETRIEVED_IT);
        list.add(LogTypeTrackable.DISCOVERED_IT);
        list.add(LogTypeTrackable.DROPPED_OFF);
        list.add(LogTypeTrackable.VISITED);
        list.add(LogTypeTrackable.NOTE);
        return list;
    }

    @Override
    public boolean canLogTime() {
        return true;
    }

    @Override
    public boolean canLogCoordinates() {
        return true;
    }

    @Override
    public boolean isTrackingCodeNeededToPostNote() {
        return true;
    }

}
