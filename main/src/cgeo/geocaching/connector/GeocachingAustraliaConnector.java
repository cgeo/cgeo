package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

public class GeocachingAustraliaConnector extends AbstractConnector {

    @Override
    public String getName() {
        return "Geocaching Australia";
    }

    @Override
    public String getCacheUrl(final cgCache cache) {
        return "http://" + getHost() + "/cache/" + cache.getGeocode();
    }

    @Override
    public String getHost() {
        return "geocaching.com.au";
    }

    @Override
    public boolean canHandle(final String geocode) {
        return (StringUtils.startsWithIgnoreCase(geocode, "GA") || StringUtils.startsWithIgnoreCase(geocode, "TP")) && isNumericId(geocode.substring(2));
    }
}
