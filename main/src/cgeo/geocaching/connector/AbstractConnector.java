package cgeo.geocaching.connector;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.geopoint.Viewport;

public abstract class AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(String geocode) {
        return false;
    }

    @Override
    public boolean supportsWatchList() {
        return false;
    }

    @Override
    public boolean supportsFavoritePoints() {
        return false;
    }

    @Override
    public boolean supportsLogging() {
        return false;
    }

    @Override
    public String getLicenseText(final cgCache cache) {
        return null;
    }

    @Override
    public boolean supportsUserActions() {
        return false;
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String tokens[]) {
        return null;
    }

    protected static boolean isNumericId(final String string) {
        try {
            return Integer.parseInt(string) > 0;
        } catch (NumberFormatException e) {
        }
        return false;
    }

    @Override
    public boolean isZippedGPXFile(String fileName) {
        // don't accept any file by default
        return false;
    }

    @Override
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon) {
        // let every cache have reliable coordinates by default
        return true;
    }

    @Override
    public String[] getTokens() {
        return null;
    }
}
