package cgeo.geocaching.connector.opencaching;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.AbstractConnector;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class OpenCachingConnector extends AbstractConnector {

    private final String host;
    private final String name;
    private final String prefix;
    private static final Pattern gpxZipFilePattern = Pattern.compile("oc[a-z]{2,3}\\d{5,}\\.zip", Pattern.CASE_INSENSITIVE);

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

    @Override
    public boolean isZippedGPXFile(String fileName) {
        return gpxZipFilePattern.matcher(fileName).matches();
    }
}
