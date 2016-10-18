package cgeo.geocaching.connector;

import cgeo.geocaching.models.Image;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.TrackableLog;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

public interface ILoggingManager {

    /**
     * Post a log for a cache online
     *
     * @param logPassword
     *            optional, maybe null
     */
    @NonNull
    LogResult postLog(@NonNull LogType logType,
            @NonNull Calendar date,
            @NonNull String log,
            @Nullable String logPassword,
            @NonNull List<TrackableLog> trackableLogs);

    @NonNull
    ImageResult postLogImage(String logId,
            Image image);

    boolean hasLoaderError();

    @NonNull
    List<TrackableLog> getTrackables();

    @NonNull
    List<LogType> getPossibleLogTypes();

    void init();

    int getPremFavoritePoints();
}
