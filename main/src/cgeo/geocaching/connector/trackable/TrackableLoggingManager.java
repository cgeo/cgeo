package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLog;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

public interface TrackableLoggingManager {

    /**
     * Post a log for a trackable online
     */
    LogResult postLog(Geocache cache,
                      TrackableLog trackableLog,
                      Calendar date,
                      String log);

    @Nullable
    ImageResult postLogImage(String logId,
                             Image image);

    @NonNull
    List<LogTypeTrackable> getPossibleLogTypesTrackable();

    boolean canLogTime();

    boolean canLogCoordinates();

    void setGuid(final String guid);

    boolean isTrackingCodeNeededToPostNote();

    boolean postReady();
}
