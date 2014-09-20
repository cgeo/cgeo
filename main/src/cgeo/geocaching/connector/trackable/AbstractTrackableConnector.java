package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.loaders.AbstractCacheInventoryLoader;
import cgeo.geocaching.loaders.AbstractInventoryLoader;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTrackableConnector implements TrackableConnector {

    @Override
    public boolean isLoggable() {
        return false;
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
    @NonNull
    public List<UserAction> getUserActions() {
        return AbstractConnector.getDefaultUserActions();
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        throw new IllegalStateException("this trackable does not have a corresponding URL");
    }

    @Override
    public List<Trackable> searchTrackables(final String geocode) { return new ArrayList<>(); }

    @Override
    public List<Trackable> loadInventory() { return new ArrayList<>(); }

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
    public int getInventoryLoaderId() {
        return 0;
    }

    @Override
    public int getCacheInventoryLoaderId() {
        return 0;
    }

    @Override
    public AbstractInventoryLoader getInventoryLoader(final Context context) {
        return null;
    }

    @Override
    public AbstractCacheInventoryLoader getCacheInventoryLoader(final Context context, final String geocode) {
        return null;
    }

    @Override
    public TrackableLoggingManager getTrackableLoggingManager() {
        return null;
    }
}
