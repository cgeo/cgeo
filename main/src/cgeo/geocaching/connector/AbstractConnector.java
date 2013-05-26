package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheRealm;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;

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
    public boolean addToWatchlist(Geocache cache) {
        return false;
    }

    @Override
    public boolean removeFromWatchlist(Geocache cache) {
        return false;
    }

    @Override
    public boolean supportsOwnCoordinates() {
        return false;
    }

    /**
     * Uploading modified coordinates to website
     *
     * @param cache
     * @param wpt
     * @return success
     */
    @Override
    public boolean uploadModifiedCoordinates(Geocache cache, Geopoint wpt) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@link IConnector}
     */
    @Override
    public boolean deleteModifiedCoordinates(Geocache cache) {
        throw new UnsupportedOperationException();
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
    public boolean supportsLogImages() {
        return false;
    }

    @Override
    public ILoggingManager getLoggingManager(Activity activity, Geocache cache) {
        return new NoLoggingManager();
    }

    @Override
    public String getLicenseText(final Geocache cache) {
        return null;
    }

    @Override
    public boolean supportsUserActions() {
        return false;
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

    @Override
    public String getGeocodeFromUrl(final String url) {
        final String urlPrefix = getCacheUrlPrefix();
        if (StringUtils.startsWith(url, urlPrefix)) {
            String geocode = url.substring(urlPrefix.length());
            if (canHandle(geocode)) {
                return geocode;
            }
        }
        return null;
    }

    abstract protected String getCacheUrlPrefix();

    @Override
    public String getLongCacheUrl(final Geocache cache) {
        return getCacheUrl(cache);
    }

    /**
     * {@link IConnector}
     */
    @Override
    public CacheRealm getCacheRealm() {
        return CacheRealm.OTHER;
    }

    /**
     * {@link IConnector}
     */
    @Override
    public boolean isActivated() {
        return false;
    }
}
