package cgeo.geocaching.connector;

import cgeo.geocaching.TrackableLog;

import java.util.Collections;
import java.util.List;

public abstract class AbstractLoggingManager implements ILoggingManager {

    @Override
    public boolean hasLoaderError() {
        return false;
    }

    @Override
    public List<TrackableLog> getTrackables() {
        return Collections.emptyList();
    }

}
