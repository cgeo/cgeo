package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.enumerations.TrackableBrand;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

/**
 * Methods to be implemented by any connector for handling trackables
 *
 */
public interface TrackableConnector {

    public boolean canHandleTrackable(final String geocode);

    /**
     * Check whether the connector has URLs corresponding the the trackable.
     *
     * @return <tt>true</tt> if the connector handles URLs, <tt>false</tt> otherwise
     */
    public boolean hasTrackableUrls();

    /**
     * Return the URL for a trackable. Might throw {@link IllegalStateException} if called
     * on a connector which does not have URLs for trackables. This might be checked using
     * {@link #hasTrackableUrls()}.
     *
     * @param trackable the trackable
     * @return the URL corresponding to this trackable
     */
    @NonNull
    public String getUrl(@NonNull final Trackable trackable);

    public boolean isLoggable();

    @Nullable
    public Trackable searchTrackable(String geocode, String guid, String id);

    @Nullable
    public String getTrackableCodeFromUrl(final @NonNull String url);

    @NonNull
    public List<UserAction> getUserActions();

    public TrackableBrand getBrand();
}
