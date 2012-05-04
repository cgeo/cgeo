package cgeo.geocaching.connector.oc;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.AbstractConnector;

import java.util.regex.Pattern;

public class OCConnector extends AbstractConnector {

    private final String host;
    private final String name;
    private final Pattern codePattern;
    private static final Pattern gpxZipFilePattern = Pattern.compile("oc[a-z]{2,3}\\d{5,}\\.zip", Pattern.CASE_INSENSITIVE);

    public OCConnector(final String name, final String host, final String prefix) {
        this.name = name;
        this.host = host;
        codePattern = Pattern.compile(prefix + "[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean canHandle(String geocode) {
        if (geocode == null) {
            return false;
        }
        return codePattern.matcher(geocode).matches();
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
