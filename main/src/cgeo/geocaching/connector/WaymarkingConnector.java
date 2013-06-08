package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;

import org.apache.commons.lang3.StringUtils;

public class WaymarkingConnector extends AbstractConnector {

    @Override
    public String getName() {
        return "Waymarking";
    }

    @Override
    public String getCacheUrl(Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    public String getHost() {
        return "www.waymarking.com";
    }

    @Override
    public boolean isOwner(ICache cache) {
        // this connector has no user management
        return false;
    }

    @Override
    protected String getCacheUrlPrefix() {
        return "http://" + getHost() + "/waymarks/";
    }

    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.startsWith(geocode, "WM");
    }
}
