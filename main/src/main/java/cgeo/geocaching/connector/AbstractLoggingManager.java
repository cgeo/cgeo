package cgeo.geocaching.connector;

import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public abstract class AbstractLoggingManager implements ILoggingManager {

    @Override
    public boolean hasLoaderError() {
        return false;
    }

    @Override
    public boolean hasTrackableLoadError() {
        return false;
    }

    @Override
    @NonNull
    public List<TrackableLog> getTrackables() {
        return Collections.emptyList();
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

}
