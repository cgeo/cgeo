package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.loaders.RecaptchaReceiver;

/**
 * connector capability for online searching caches by the next page
 *
 */
public interface ISearchByNextPage extends IConnector {
    SearchResult searchByNextPage(final SearchResult search, final boolean showCaptcha, final RecaptchaReceiver recaptchaReceiver);
}
