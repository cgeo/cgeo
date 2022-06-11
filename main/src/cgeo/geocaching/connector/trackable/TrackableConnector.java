package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.log.AbstractLoggingActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;

/**
 * Methods to be implemented by any connector for handling trackables
 */
public interface TrackableConnector {

    /**
     * Return the preference activity for which the connector is attached to.
     * The service could be launched to ask user to configure something.
     *
     * @return the service ID corresponding to the preference activity for the connector
     */
    int getPreferenceActivity();

    boolean canHandleTrackable(@Nullable String geocode);

    boolean canHandleTrackable(@Nullable String geocode, @Nullable TrackableBrand brand);

    /**
     * Return the Title of the service the connector is attached to.
     * Title may be used in messages given to the user, like to say which connector need to
     * be activated for a specific feature.
     *
     * @return the service TITLE corresponding to this connector
     */
    @NonNull
    String getServiceTitle();

    /**
     * Check whether the connector has URLs corresponding to the trackable.
     *
     * @return <tt>true</tt> if the connector handles URLs, <tt>false</tt> otherwise
     */
    boolean hasTrackableUrls();

    /**
     * Return the URL for a trackable. Might throw {@link IllegalStateException} if called
     * on a connector which does not have URLs for trackables. This might be checked using
     * {@link #hasTrackableUrls()}.
     *
     * @param trackable the trackable
     * @return the URL corresponding to this trackable
     */
    @NonNull
    String getUrl(@NonNull Trackable trackable);

    /**
     * Get the browser URL for the given LogEntry. May return null if no url available or identifiable.
     */
    @Nullable
    String getLogUrl(@NonNull LogEntry logEntry);

    /**
     * Tell if the trackable has logging capabilities.
     *
     * @return True if trackable is loggable.
     */
    boolean isLoggable();

    /**
     * Return a Trackable corresponding to the Trackable Geocode (Tracking Code) or Guid.
     * Note: Only GC connector support guid.
     *
     * @param geocode the trackable Tracking Code
     * @param guid    the trackable guid
     * @param id      the trackable id
     * @return the Trackable object.
     */
    @Nullable
    @WorkerThread
    Trackable searchTrackable(String geocode, String guid, String id);

    /**
     * Return a Trackable corresponding to the Trackable Geocode.
     *
     * @param geocode the trackable
     * @return the Trackable object.
     */
    @NonNull
    @WorkerThread
    List<Trackable> searchTrackables(String geocode);

    /**
     * Return a Trackable id from an url.
     *
     * @param url for one trackable
     * @return the Trackable Geocode.
     */
    @Nullable
    String getTrackableCodeFromUrl(@NonNull String url);

    /**
     * Return a Trackable Tracking Code from an url.
     *
     * @param url for one trackable
     * @return the Trackable Tracking Code, {@code null} if the URL cannot be decoded.
     */
    @Nullable
    String getTrackableTrackingCodeFromUrl(@NonNull String url);

    /**
     * Return available user actions for the trackable.
     *
     * @return the List of available user action.
     */
    @NonNull
    List<UserAction> getUserActions(UserAction.UAContext user);

    /**
     * Return the Brand object for the Trackable.
     * If Brand could not be defined, return UNKNOWN_BRAND
     *
     * @return the Trackable Brand object.
     */
    @NonNull
    TrackableBrand getBrand();

    /**
     * Return a list of Trackable in user's inventory.
     * In most case, user must be connected to the service.
     *
     * @return the Trackable list.
     */
    @NonNull
    @WorkerThread
    List<Trackable> loadInventory();

    /**
     * Return the Trackable Logging Manager for the Trackable.
     *
     * @param activity currently running
     * @return the Trackable logging manager.
     */
    @Nullable
    AbstractTrackableLoggingManager getTrackableLoggingManager(AbstractLoggingActivity activity);

    /**
     * Tell if the trackable is loggable via a generic Trackable Connector.
     *
     * @return True if Trackable is loggable via a generic Trackable Connector.
     */
    boolean isGenericLoggable();

    /**
     * Tell if the connector for this trackable is active.
     *
     * @return True if connector is active.
     */
    boolean isActive();

    /**
     * Tell if user is registered to the connector for this trackable.
     *
     * @return True if user is connected to service.
     */
    boolean isRegistered();

    /**
     * Tell if the connector recommend logging a Trackable with Geocode.
     *
     * @return True if connector recommend Geocode.
     */
    boolean recommendLogWithGeocode();

    int getTrackableLoggingManagerLoaderId();

    /**
     * Return list of Trackables in user's inventory converted to TrackableLog object.
     * TrackableLog are used for posting a Trackable Log, they contain necessary
     * information to post the Trackable Log.
     *
     * @return the TrackableLog corresponding to trackables in user's inventory as Observable.
     */
    @NonNull
    Observable<TrackableLog> trackableLogInventory();

    /**
     * Get host name of the connector server for dynamic loading of data.
     */
    @NonNull
    String getHost();

    /**
     * Get url of the connector server, for dynamic loading of data.
     * Contains scheme.
     */
    @NonNull
    String getHostUrl();

    /**
     * Get url to use when testing website availability (because host url may redirect)
     */
    @NonNull
    String getTestUrl();

    /**
     * Get proxy url name of the connector server, if any, for dynamic loading of data.
     * Contains scheme.
     */
    @Nullable
    String getProxyUrl();
}
