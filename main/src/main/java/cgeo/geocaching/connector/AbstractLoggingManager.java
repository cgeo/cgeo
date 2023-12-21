package cgeo.geocaching.connector;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.utils.TextUtils;

import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractLoggingManager implements ILoggingManager {

    private final Geocache cache;
    private final IConnector connector;

    protected AbstractLoggingManager(@NonNull final IConnector connector, @NonNull final Geocache cache) {
        this.cache = Objects.requireNonNull(cache);
        this.connector = Objects.requireNonNull(connector);
    }

    @NonNull
    @Override
    public Geocache getCache() {
        return cache;
    }

    @NonNull
    @Override
    public IConnector getConnector() {
        return connector;
    }


    @NonNull
    @Override
    public LogResult editLog(@NonNull final LogEntry newEntry) {
        return LogResult.error(StatusCode.LOG_POST_ERROR);
    }

    @NonNull
    @Override
    public LogResult deleteLog(@NonNull final LogEntry newEntry) {
        return LogResult.error(StatusCode.LOG_POST_ERROR);
    }

    @Override
    public String convertLogTextToEditableText(final String logText) {
        if (TextUtils.containsHtml(logText)) {
            return Html.fromHtml(logText).toString();
        }
        return logText;
    }

    @Override
    public boolean canEditLog(@NonNull final LogEntry entry) {
        return getConnector().canEditLog(getCache(), entry);
    }

    @Override
    public boolean canDeleteLog(@NonNull final LogEntry entry) {
        return getConnector().canDeleteLog(getCache(), entry);
    }

    @NonNull
    @Override
    public ImageResult createLogImage(@NonNull final String logId, @NonNull final Image image) {
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR);
    }

    @NonNull
    @Override
    public ImageResult editLogImage(@NonNull final String logId, @NonNull final String serviceImageId, @Nullable final String title, @Nullable final String description) {
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR);
    }

    @NonNull
    @Override
    public ImageResult deleteLogImage(@NonNull final String logId, @NonNull final String serviceImageId) {
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR);
    }

    @Override
    public boolean supportsEditLogImages() {
        return false;
    }

    @Override
    public boolean supportsDeleteLogImages() {
        return false;
    }

    @Override
    public boolean supportsLogWithFavorite() {
        return false;
    }

    @Override
    public boolean supportsLogWithTrackables() {
        return false;
    }

    @Override
    public boolean supportsLogWithVote() {
        return false;
    }


    @Override
    public Long getMaxImageUploadSize() {
        return null;
    }

    @Override
    public boolean isImageCaptionMandatory() {
        return false;
    }

    @Override
    public boolean canLogReportType(@NonNull final ReportProblemType reportType) {
        return false;
    }

    @Override
    @NonNull
    public List<ReportProblemType> getReportProblemTypes(@NonNull final Geocache geocache) {
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public LogContextInfo getLogContextInfo(@Nullable final String serviceLogId) {
        return new LogContextInfo(this, serviceLogId);
    }

    @Override
    public int getFavoriteCheckboxText() {
        return R.plurals.fav_points_remaining;
    }

}
