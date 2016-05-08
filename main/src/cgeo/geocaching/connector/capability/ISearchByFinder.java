package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.loaders.RecaptchaReceiver;

import org.eclipse.jdt.annotation.NonNull;

public interface ISearchByFinder extends IConnector {
    SearchResult searchByFinder(@NonNull final String finder, @NonNull final RecaptchaReceiver recaptchaReceiver);
}
