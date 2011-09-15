package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

/**
 * connector for OpenCaching.de (and several other country domains)
 *
 */
public class OCConnector extends AbstractConnector implements IConnector {
    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.isNotBlank(geocode) && geocode.toUpperCase().startsWith("OC");
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        return "http://www.opencaching.de/viewcache.php?wp=" + cache.geocode;
    }
}
