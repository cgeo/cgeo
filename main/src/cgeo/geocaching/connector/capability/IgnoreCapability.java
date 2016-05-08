package cgeo.geocaching.connector.capability;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.connector.IConnector;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Connector capability to ignore caches.
 */
public interface IgnoreCapability extends IConnector {
    boolean canIgnoreCache(@NonNull final Geocache cache);
    void ignoreCache(@NonNull final Geocache cache);
}
