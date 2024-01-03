package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLogEntry;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.List;

public interface TrackableLoggingManager {

    String getTrackableCode();

    /**
     * Post a log for a trackable online
     */
    @WorkerThread
    LogResult postLog(Geocache cache, TrackableLogEntry trackableLog);

    @NonNull
    List<LogTypeTrackable> getPossibleLogTypesTrackable();

    @WorkerThread
    @NonNull
    List<LogTypeTrackable> getPossibleLogTypesTrackableOnline();

    boolean canLogTime();

    boolean canLogCoordinates();

    boolean isTrackingCodeNeededToPostNote();


}
