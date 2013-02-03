package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;

import org.apache.commons.lang3.StringUtils;

public class GeocachingAustraliaConnector extends AbstractConnector {

    @Override
    public String getName() {
        return "Geocaching Australia";
    }

    @Override
    public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    public String getHost() {
        return "geocaching.com.au";
    }

    @Override
    public boolean isOwner(final ICache cache) {
        return false;
    }

    @Override
    public boolean canHandle(final String geocode) {
        return (StringUtils.startsWithIgnoreCase(geocode, "GA") || StringUtils.startsWithIgnoreCase(geocode, "TP")) && isNumericId(geocode.substring(2));
    }

    @Override
    protected String getCacheUrlPrefix() {
        return "http://" + getHost() + "/cache/";
    }
}
