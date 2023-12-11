package cgeo.geocaching.connector;

import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;

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

    @Override
    public Long getMaxImageUploadSize() {
        return null;
    }

    @Override
    public boolean isImageCaptionMandatory() {
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

}
