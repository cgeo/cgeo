package cgeo.geocaching.connector.trackable;

public abstract class AbstractTrackableLoggingManager implements TrackableLoggingManager {

    @Override
    public boolean hasLoaderError() {
        return false;
    }

    @Override
    public boolean canLogTime() {
        return false;
    }

    @Override
    public boolean canLogCoordinates() {
        return false;
    }

    @Override
    public void setGuid (final String guid) {
    }

    @Override
    public boolean isRegistered() {
        return false;
    }
}
