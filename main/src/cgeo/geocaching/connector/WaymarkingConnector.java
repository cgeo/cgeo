package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

class WaymarkingConnector extends AbstractConnector {

    @Override
    public String getName() {
        return "Waymarking";
    }

    @Override
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    public String getHost() {
        return "www.waymarking.com";
    }

    @Override
    public boolean isOwner(final Geocache cache) {
        // this connector has no user management
        return false;
    }

    @Override
    protected String getCacheUrlPrefix() {
        return "http://" + getHost() + "/waymarks/";
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return StringUtils.startsWith(geocode, "WM");
    }
}
