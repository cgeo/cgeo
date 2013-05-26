package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.LogType;

import android.net.Uri;

import java.util.Calendar;
import java.util.List;

public interface ILoggingManager {

    LogResult postLog(Geocache cache,
            LogType logType,
            Calendar date,
            String log,
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
