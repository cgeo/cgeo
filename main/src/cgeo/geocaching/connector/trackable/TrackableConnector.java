package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.AbstractLoggingActivity;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.loaders.AbstractCacheInventoryLoader;
import cgeo.geocaching.loaders.AbstractInventoryLoader;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Context;

import java.util.List;

/**
 * Methods to be implemented by any connector for handling trackables
 *
 */
public interface TrackableConnector {

    /**
     * Return the preference activity for which the connector is attached to.
     * The service could be launched to ask user to configure something.
     *
     * @return the service ID corresponding to the preference activity for the connector
     */
    public int getPreferenceActivity();

    public boolean canHandleTrackable(final String geocode);

    /**
     * Return the Title of the service the connector is attached to.
     * Title may be used in messages given to the user, like to say which connector need to
     * be activated for a specific feature.
     *
     * @return the service TITLE corresponding to this connector
     */
    @NonNull
    public String getServiceTitle();

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
    public Trackable searchTrackable(final String geocode, final String guid, final String id);

    @NonNull
    public List<Trackable> searchTrackables(final String geocode);

    @Nullable
    public String getTrackableCodeFromUrl(final @NonNull String url);

    @NonNull
    public List<UserAction> getUserActions();

    @NonNull
    public TrackableBrand getBrand();

    @NonNull
    public List<Trackable> loadInventory();

    @Nullable
    public AbstractTrackableLoggingManager getTrackableLoggingManager(final AbstractLoggingActivity activity);

    boolean isGenericLoggable();

    boolean isActive();

    public boolean isRegistered();

    public int getInventoryLoaderId();

    public int getCacheInventoryLoaderId();

    public int getTrackableLoggingManagerLoaderId();

    public AbstractInventoryLoader getInventoryLoader(final Context context);

    public AbstractCacheInventoryLoader getCacheInventoryLoader(final Context context, final String geocode);
}
