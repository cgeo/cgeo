package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.UserAction;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class UnknownTrackableConnector extends AbstractTrackableConnector {

    @Override
    public boolean canHandleTrackable(@Nullable final String geocode) {
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

    @Override
    @NonNull
    public List<UserAction> getUserActions() {
        return Collections.emptyList();
    }
}
