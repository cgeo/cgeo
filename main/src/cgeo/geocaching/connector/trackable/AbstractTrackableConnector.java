package cgeo.geocaching.connector.trackable;

import org.eclipse.jdt.annotation.NonNull;

public abstract class AbstractTrackableConnector implements TrackableConnector {

    @Override
    public boolean isLoggable() {
        return false;
    }

    @Override
    public String getTrackableCodeFromUrl(@NonNull String url) {
        return null;
    }
}
