package cgeo.geocaching.connector.capability;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.connector.IConnector;

import android.support.annotation.NonNull;

/**
 * Connector capability to ignore caches.
 */
public interface IgnoreCapability extends IConnector {
    boolean canIgnoreCache(@NonNull Geocache cache);
    void ignoreCache(@NonNull Geocache cache);
}
