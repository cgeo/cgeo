package cgeo.geocaching.connector;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Calendar;
import java.util.List;

public interface ILoggingManager {

    /**
     * Post a log for a cache online
     *
     * @param logPassword optional, maybe null
     */
    @NonNull
    @WorkerThread
    LogResult postLog(@NonNull LogType logType,
                      @NonNull Calendar date,
                      @NonNull String log,
                      @Nullable String logPassword,
                      @NonNull List<TrackableLog> trackableLogs,
                      @NonNull ReportProblemType reportProblem);

    @NonNull
    @WorkerThread
    ImageResult postLogImage(String logId,
                             Image image);

    boolean hasLoaderError();

    @NonNull
    List<TrackableLog> getTrackables();

    @NonNull
    List<LogType> getPossibleLogTypes();

    void init();


    Long getMaxImageUploadSize();

    boolean isImageCaptionMandatory();

    @NonNull
    List<ReportProblemType> getReportProblemTypes(@NonNull Geocache geocache);

    boolean hasTrackableLoadError();

}
