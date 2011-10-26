package cgeo.geocaching.connector.opencaching;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.IConnector;

import org.apache.commons.lang3.StringUtils;

public class OpenCachingConnector extends AbstractConnector implements IConnector {

    private final String host;
    private final String name;
    private final String prefix;

    public OpenCachingConnector(final String name, final String host, final String prefix) {
        this.name = name;
        this.host = host;
        this.prefix = prefix;
    }

    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.startsWithIgnoreCase(geocode, prefix);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        return "http://" + host + "/viewcache.php?wp=" + cache.getGeocode();
    }

    @Override
    public String getHost() {
        return host;
    }

}
