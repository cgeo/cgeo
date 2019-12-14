package cgeo.geocaching.connector.capability;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;

import androidx.annotation.NonNull;

public interface ISearchByFinder extends IConnector {
    SearchResult searchByFinder(@NonNull String finder);
}
