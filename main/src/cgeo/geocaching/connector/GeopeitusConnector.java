package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

public class GeopeitusConnector extends AbstractConnector implements IConnector {

    @Override
    public String getName() {
        return "geopeitus.ee";
    }

    @Override
    public String getCacheUrl(final cgCache cache) {
        return "http://" + getHost() + "/aare/" + StringUtils.stripStart(cache.getGeocode().substring(2), "0");
    }

    @Override
    public String getHost() {
        return "www.geopeitus.ee";
    }

    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.startsWith(geocode, "GE") && isNumericId(geocode.substring(2));
    }
}
