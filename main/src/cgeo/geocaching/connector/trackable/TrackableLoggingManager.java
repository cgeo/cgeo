package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogTypeTrackable;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Calendar;

public interface TrackableLoggingManager {

    /**
     * Post a log for a cache online
     *
     * @param trackable
     * @param logType
     * @param date
     * @param log
     * @return
     */
    LogResult postLog(Geocache cache,
                      TrackableLog trackableLog,
                      Calendar date,
                      String log);

    ImageResult postLogImage(String logId,
                             String imageCaption,
                             String imageDescription,
                             Uri imageUri);

    public boolean hasLoaderError();

    public ArrayList<LogTypeTrackable> getPossibleLogTypesTrackable();

    public boolean canLogTime();

    public boolean canLogCoordinates();

    public void setGuid(final String guid);

    public boolean isRegistered();

    public boolean postReady();
}
