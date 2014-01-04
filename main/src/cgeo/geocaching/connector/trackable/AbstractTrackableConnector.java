package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.UserAction;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

public abstract class AbstractTrackableConnector implements TrackableConnector {

    @Override
    public boolean isLoggable() {
        return false;
    }

    @Override
    public String getTrackableCodeFromUrl(@NonNull String url) {
        return null;
    }

    @Override
    public @NonNull
    List<UserAction> getUserActions() {
        return AbstractConnector.getDefaultUserActions();
    }
}
