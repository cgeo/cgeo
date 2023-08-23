package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;

import java.util.Collection;

/**
 * Connectors implementing this interface are able to amend one or more caches
 * (online) with additional information
 */
public interface ICacheAmendment extends IConnector {

    void amendCaches(Collection<Geocache> caches);
}
