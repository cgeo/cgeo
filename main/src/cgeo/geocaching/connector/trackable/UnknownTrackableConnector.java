package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import org.eclipse.jdt.annotation.NonNull;

public class UnknownTrackableConnector extends AbstractTrackableConnector {

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return false;
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        throw new IllegalStateException("getUrl cannot be called on unknown trackables");
    }

    @Override
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return null;
    }

}
