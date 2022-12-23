package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        return Collections.emptyList();
    }

    @Override
    @NonNull
    public String getHost() {
        throw new IllegalStateException("Unknown trackable connector does not have a host.");
    }

    @Override
    @NonNull
    public String getHostUrl() {
        throw new IllegalStateException("Unknown trackable connector does not have a host url.");
    }
}
