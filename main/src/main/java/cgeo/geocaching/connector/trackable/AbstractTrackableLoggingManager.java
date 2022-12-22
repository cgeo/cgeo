package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.models.Image;

import android.content.Context;

import androidx.loader.content.AsyncTaskLoader;

import java.util.List;

public abstract class AbstractTrackableLoggingManager extends AsyncTaskLoader<List<LogTypeTrackable>> implements TrackableLoggingManager {

    public AbstractTrackableLoggingManager(final Context context) {
        super(context);
    }

    @Override
    public boolean isTrackingCodeNeededToPostNote() {
        return false;
    }

    @Override
    public ImageResult postLogImage(final String logId, final Image image) {
        // No support for images
        return null;
    }

}
