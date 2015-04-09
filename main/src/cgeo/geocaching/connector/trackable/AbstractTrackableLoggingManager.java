package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.enumerations.LogTypeTrackable;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.List;

public abstract class AbstractTrackableLoggingManager extends AsyncTaskLoader<List<LogTypeTrackable>> implements TrackableLoggingManager {

    public AbstractTrackableLoggingManager(final Context context) {
        super(context);
    }

    @Override
    public abstract boolean canLogTime();

    @Override
    public abstract boolean canLogCoordinates();

    @Override
    public abstract void setGuid (final String guid);

    @Override
    public abstract boolean isRegistered();

    @Override
    public boolean isTrackingCodeNeededToPostNote() {
        return false;
    }

    @Override
    public abstract boolean postReady();
}
