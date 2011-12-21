package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

public class UnknownConnector extends AbstractConnector {

    @Override
    public String getName() {
        return "Unknown caches";
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        return null; // we have no url for these caches
    }

    @Override
    public String getHost() {
        return null; // we have no host for these caches
    }

    @Override
    public boolean canHandle(final String geocode) {
        return StringUtils.isNotBlank(geocode);
    }
}
