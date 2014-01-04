package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.loaders.RecaptchaReceiver;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Connector capability to search online by owner name. Implement this in a {@link IConnector} to take part in the
 * global search by owner.
 *
 */
public interface ISearchByOwner extends IConnector {
    public SearchResult searchByOwner(final @NonNull String owner, final @NonNull RecaptchaReceiver recaptchaReceiver);
}
