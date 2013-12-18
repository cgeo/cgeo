package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.loaders.RecaptchaReceiver;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Connector capability of searching online for a cache by keyword.
 * 
 */
public interface ISearchByKeyword extends IConnector {
    // TODO: The recaptcha receiver is only needed for GC. Would be good to refactor this away from the generic interface.
    public SearchResult searchByKeyword(final @NonNull String keyword, final @NonNull RecaptchaReceiver recaptchaReceiver);
}
