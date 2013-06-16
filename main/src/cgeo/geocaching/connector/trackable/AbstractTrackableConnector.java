package cgeo.geocaching.connector.trackable;


public abstract class AbstractTrackableConnector implements TrackableConnector {

    @Override
    public boolean isLoggable() {
        return false;
    }
}
