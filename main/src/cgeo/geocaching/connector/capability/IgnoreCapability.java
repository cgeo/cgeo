package cgeo.geocaching.connector.capability;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.connector.IConnector;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Connector capability to ignore caches.
 */
public interface IgnoreCapability extends IConnector {
    public boolean canIgnoreCache(final @NonNull Geocache cache);
    public void ignoreCache(final @NonNull Geocache cache);
}
