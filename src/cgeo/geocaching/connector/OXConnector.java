package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

/**
 * connector for OpenCaching.com
 *
 */
public class OXConnector extends AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.isNotBlank(geocode) && geocode.toUpperCase().startsWith("OX");
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        return "http://www.opencaching.com/#!geocache/" + cache.geocode;
    }

}
