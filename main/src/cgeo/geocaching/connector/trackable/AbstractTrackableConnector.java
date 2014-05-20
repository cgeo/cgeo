package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
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
    public @Nullable
    String getTrackableCodeFromUrl(@NonNull String url) {
        return null;
    }

    @Override
    public String getCgeoUrl(Trackable trackable) {
        return getBrowserUrl(trackable);
    }

    @Override
    public @NonNull
    List<UserAction> getUserActions() {
        return AbstractConnector.getDefaultUserActions();
    }
}
