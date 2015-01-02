package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.UserAction;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

public abstract class AbstractTrackableConnector implements TrackableConnector {

    @Override
    public boolean isLoggable() {
        return false;
    }

    @Override
    @Nullable
    public String getTrackableCodeFromUrl(@NonNull final String url) {
        return null;
    }

    @Override
    @NonNull
    public List<UserAction> getUserActions() {
        return AbstractConnector.getDefaultUserActions();
    }
}
