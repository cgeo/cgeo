package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

public interface IConnector {
    public boolean canHandle(final String geocode);

    public boolean supportsRefreshCache(final cgCache cache);

    public String getCacheUrl(final cgCache cache);

    public boolean supportsWatchList();

    public boolean supportsLogging();
}
