package cgeo.geocaching.connector.capability;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.connector.IConnector;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Connector capability to ignore caches.
 */
public interface IgnoreCapability extends IConnector {
    boolean canIgnoreCache(final @NonNull Geocache cache);
    void ignoreCache(final @NonNull Geocache cache);
}
