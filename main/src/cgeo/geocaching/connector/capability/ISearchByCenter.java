package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.location.Geopoint;

import org.eclipse.jdt.annotation.NonNull;

/**
 * connector capability for online searching caches around a center coordinate, sorted by distance
 *
 */
public interface ISearchByCenter extends IConnector {
    public SearchResult searchByCenter(final @NonNull Geopoint center, final @NonNull RecaptchaReceiver recaptchaReceiver);
}
