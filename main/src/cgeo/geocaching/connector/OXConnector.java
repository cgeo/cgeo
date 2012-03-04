package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;

import java.util.regex.Pattern;

/**
 * connector for OpenCaching.com
 *
 */
public class OXConnector extends AbstractConnector {

    private static final Pattern PATTERN_GEOCODE = Pattern.compile("OX[A-Z0-9]+", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandle(String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches();
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        return "http://www.opencaching.com/#!geocache/" + cache.getGeocode();
    }

    @Override
    public String getName() {
        return "OpenCaching.com";
    }

    @Override
    public String getHost() {
        return "www.opencaching.com";
    }

}
