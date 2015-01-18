package cgeo.geocaching.connector;

import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.LogType;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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
    @NonNull
    LogResult postLog(@NonNull LogType logType,
            @NonNull Calendar date,
            @NonNull String log,
            @Nullable String logPassword,
            @NonNull List<TrackableLog> trackableLogs);

    @NonNull
    ImageResult postLogImage(String logId,
            String imageCaption,
            String imageDescription,
            Uri imageUri);

    public boolean hasLoaderError();

    @NonNull
    public List<TrackableLog> getTrackables();

    @NonNull
    public List<LogType> getPossibleLogTypes();

    public void init();
}
