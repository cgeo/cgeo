package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.UserAction;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

/**
 * Methods to be implemented by any connector for handling trackables
 *
 */
public interface TrackableConnector {

    public boolean canHandleTrackable(final String geocode);

    @NonNull
    public String getUrl(final Trackable trackable);

    public boolean isLoggable();

    public Trackable searchTrackable(String geocode, String guid, String id);

    public @Nullable
    String getTrackableCodeFromUrl(final @NonNull String url);

    public @NonNull
    List<UserAction> getUserActions();

}
