package cgeo.geocaching.connector;

import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.LogType;

import android.net.Uri;

import java.util.Calendar;
import java.util.List;

public interface ILoggingManager {

    /**
     * Post a log for a cache online
     * 
     * @param logType
     * @param date
     * @param log
     * @param logPassword
     *            optional, maybe null
     * @param trackableLogs
     * @return
     */
    LogResult postLog(LogType logType,
                      Calendar date,
                      String log,
                      String logPassword,
                      List<TrackableLog> trackableLogs);

    ImageResult postLogImage(String logId,
            String imageCaption,
            String imageDescription,
            Uri imageUri);

    public boolean hasLoaderError();

    public List<TrackableLog> getTrackables();

    public List<LogType> getPossibleLogTypes();

    public void init();
}
