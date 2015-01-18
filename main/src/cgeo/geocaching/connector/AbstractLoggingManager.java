package cgeo.geocaching.connector;

import cgeo.geocaching.TrackableLog;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public abstract class AbstractLoggingManager implements ILoggingManager {

    @Override
    public boolean hasLoaderError() {
        return false;
    }

    @Override
    @NonNull
    public List<TrackableLog> getTrackables() {
        return Collections.emptyList();
    }

}
