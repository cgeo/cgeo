package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import org.eclipse.jdt.annotation.NonNull;

public class UnknownTrackableConnector extends AbstractTrackableConnector {

    @Override
    public boolean canHandleTrackable(String geocode) {
        return false;
    }

    @Override
    @NonNull
    public String getUrl(Trackable trackable) {
        throw new IllegalStateException("getUrl cannot be called on unknown trackables");
    }

    @Override
    public Trackable searchTrackable(String geocode, String guid, String id) {
        return null;
    }

}
