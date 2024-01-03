package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.log.LogTypeTrackable;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.List;

public abstract class AbstractTrackableLoggingManager implements TrackableLoggingManager {

    private final String trackableCode;

    public AbstractTrackableLoggingManager(final String tbCode) {
        this.trackableCode = tbCode;
    }

    @Override
    public String getTrackableCode() {
        return trackableCode;
    }

    @Override
    public boolean isTrackingCodeNeededToPostNote() {
        return false;
    }

    @NonNull
    @WorkerThread
    @Override
    public List<LogTypeTrackable> getPossibleLogTypesTrackableOnline() {
        return getPossibleLogTypesTrackable();
    }
}
