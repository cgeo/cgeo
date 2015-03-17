package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import org.eclipse.jdt.annotation.Nullable;

public class UnknownTrackableConnector extends AbstractTrackableConnector {

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return false;
    }

    @Override
    public boolean hasTrackableUrls() {
        return false;
    }

    @Override
    @Nullable
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return null;
    }

}
