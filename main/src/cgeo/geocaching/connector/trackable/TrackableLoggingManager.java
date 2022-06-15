package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Calendar;
import java.util.List;

public interface TrackableLoggingManager {

    /**
     * Post a log for a trackable online
     */
    @WorkerThread
    LogResult postLog(Geocache cache,
                      TrackableLog trackableLog,
                      Calendar date,
                      String log);

    @Nullable
    @WorkerThread
    ImageResult postLogImage(String logId,
                             Image image);

    @NonNull
    List<LogTypeTrackable> getPossibleLogTypesTrackable();

    boolean canLogTime();

    boolean canLogCoordinates();

    void setGuid(String guid);

    boolean isTrackingCodeNeededToPostNote();

    boolean postReady();
}
