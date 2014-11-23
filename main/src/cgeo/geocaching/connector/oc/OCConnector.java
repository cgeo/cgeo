package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.enumerations.LogType;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class OCConnector extends AbstractConnector {

    private final String host;
    private final String name;
    private final Pattern codePattern;
    private static final Pattern GPX_ZIP_FILE_PATTERN = Pattern.compile("oc[a-z]{2,3}\\d{5,}\\.zip", Pattern.CASE_INSENSITIVE);

    private static final List<LogType> STANDARD_LOG_TYPES = Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.NOTE);
    private static final List<LogType> EVENT_LOG_TYPES = Arrays.asList(LogType.WILL_ATTEND, LogType.ATTENDED, LogType.NOTE);

    public OCConnector(final String name, final String host, final String prefix) {
        this.name = name;
        this.host = host;
        codePattern = Pattern.compile(prefix + "[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean canHandle(@NonNull String geocode) {
        return codePattern.matcher(geocode).matches();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCacheUrl(@NonNull Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public boolean isZippedGPXFile(String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches();
    }

    @Override
    public boolean isOwner(final Geocache cache) {
        return false;
    }

    @Override
    protected String getCacheUrlPrefix() {
        return "http://" + host + "/viewcache.php?wp=";
    }

    @Override
    public int getCacheMapMarkerId(boolean disabled) {
        if (disabled) {
            return R.drawable.marker_disabled_oc;
        }
        return R.drawable.marker_oc;
    }

    @Override
    public final List<LogType> getPossibleLogTypes(Geocache cache) {
        if (cache.isEventCache()) {
            return EVENT_LOG_TYPES;
        }

        return STANDARD_LOG_TYPES;
    }
}
