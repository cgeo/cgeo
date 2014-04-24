package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

public class GeopeitusConnector extends AbstractConnector {

    @Override
    public String getName() {
        return "geopeitus.ee";
    }

    @Override
    public String getCacheBrowserUrl(final @NonNull Geocache cache) {
        return getCacheUrlPrefix() + StringUtils.stripStart(cache.getGeocode().substring(2), "0");
    }

    @Override
    public String getHost() {
        return "www.geopeitus.ee";
    }

    @Override
    public boolean isOwner(final ICache cache) {
        return false;
    }

    @Override
    public boolean canHandle(@NonNull String geocode) {
        return StringUtils.startsWith(geocode, "GE") && isNumericId(geocode.substring(2));
    }

    @Override
    protected String getCacheUrlPrefix() {
        return "http://" + getHost() + "/aare/";
    }
}
