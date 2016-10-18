package cgeo.geocaching.connector;

import cgeo.geocaching.log.TrackableLog;

import android.support.annotation.NonNull;

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

    @Override
    public int getPremFavoritePoints() {
        return 0;
    }
}
