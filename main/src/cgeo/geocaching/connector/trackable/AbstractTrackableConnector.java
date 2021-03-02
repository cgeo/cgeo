package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.log.AbstractLoggingActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;

public abstract class AbstractTrackableConnector implements TrackableConnector {

    @Override
    public int getPreferenceActivity() {
        return 0;
    }

    @Override
    public boolean isLoggable() {
        return false;
    }

    @Override
    public boolean canHandleTrackable(@Nullable final String geocode, @Nullable final TrackableBrand brand) {
        if (brand == null || brand == TrackableBrand.UNKNOWN) {
            return canHandleTrackable(geocode);
        }
        return brand == getBrand() && canHandleTrackable(geocode);
    }

    @Override
    public boolean hasTrackableUrls() {
        return true;
    }

    @Override
    @Nullable
    public String getTrackableCodeFromUrl(@NonNull final String url) {
        return null;
    }

    @Override
    @Nullable
    public String getTrackableTrackingCodeFromUrl(@NonNull final String url) {
        return null;
    }

    @Override
    @NonNull
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        return AbstractConnector.getDefaultUserActions();
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        throw new IllegalStateException("this trackable does not have a corresponding URL");
    }

    @Override
    @Nullable
    public String getLogUrl(@NonNull final LogEntry logEntry) {
        return null; //by default, Connector does not support log urls
    }

    @Override
    @NonNull
    public String getHostUrl() {
        return "https://" + getHost();
    }

    @Override
    @NonNull
    public String getTestUrl() {
        return getHostUrl();
    }

    @Override
    @Nullable
    public String getProxyUrl() {
        return null;
    }

    @Override
    @NonNull
    public List<Trackable> searchTrackables(final String geocode) {
        return Collections.emptyList();
    }

    @Override
    @NonNull
    public List<Trackable> loadInventory() {
        return Collections.emptyList();
    }

    @Override
    public boolean isGenericLoggable() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public boolean recommendLogWithGeocode() {
        return false;
    }

    @Override
    @NonNull
    public Observable<TrackableLog> trackableLogInventory() {
        return Observable.empty();
    }

    @Override
    public int getTrackableLoggingManagerLoaderId() {
        return 0;
    }

    @Override
    public AbstractTrackableLoggingManager getTrackableLoggingManager(final AbstractLoggingActivity activity) {
        return null;
    }
}
