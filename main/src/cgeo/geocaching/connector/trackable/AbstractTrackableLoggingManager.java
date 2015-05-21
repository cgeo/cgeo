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
    public boolean isTrackingCodeNeededToPostNote() {
        return false;
    }

}
