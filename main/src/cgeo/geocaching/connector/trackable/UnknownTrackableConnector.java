package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class UnknownTrackableConnector extends AbstractTrackableConnector {

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return false;
    }

    @NonNull
    @Override
    public String getServiceTitle() {
        throw new IllegalStateException("this connector does not have a corresponding name.");
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

    @Override
    @NonNull
    public TrackableBrand getBrand() {
        return TrackableBrand.UNKNOWN;
    }
}
