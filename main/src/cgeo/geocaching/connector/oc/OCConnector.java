package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.Smiley;
import cgeo.geocaching.connector.capability.SmileyCapability;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class OCConnector extends AbstractConnector implements SmileyCapability {

    @NonNull
    private final String host;
    private final boolean https;
    @NonNull
    private final String name;
    private final Pattern codePattern;
    private static final Pattern GPX_ZIP_FILE_PATTERN = Pattern.compile("oc[a-z]{2,3}\\d{5,}\\.zip", Pattern.CASE_INSENSITIVE);

    private static final List<LogType> STANDARD_LOG_TYPES = Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.NOTE);
    private static final List<LogType> EVENT_LOG_TYPES = Arrays.asList(LogType.WILL_ATTEND, LogType.ATTENDED, LogType.NOTE);

    public OCConnector(@NonNull final String name, @NonNull final String host, final boolean https, final String prefix) {
        this.name = name;
        this.host = host;
        this.https = https;
        codePattern = Pattern.compile(prefix + "[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return codePattern.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode();
    }

    @Override
    @NonNull
    public String getHost() {
        return host;
    }

    @Override
    public boolean isZippedGPXFile(@NonNull final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches();
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getSchemeAndHost() + "/viewcache.php?wp=";
    }

    @Override
    public int getCacheMapMarkerId(final boolean disabled) {
        if (disabled) {
            return R.drawable.marker_disabled_oc;
        }
        return R.drawable.marker_oc;
    }

    @Override
    @NonNull
    public final List<LogType> getPossibleLogTypes(@NonNull final Geocache cache) {
        if (cache.isEventCache()) {
            return EVENT_LOG_TYPES;
        }

        return STANDARD_LOG_TYPES;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        // different opencaching installations have different supported URLs

        // host.tld/geocode
        final String shortHost = StringUtils.remove(getHost(), "www.");
        final String firstLevel = StringUtils.substringAfter(url, shortHost + "/");
        if (canHandle(firstLevel)) {
            return firstLevel;
        }

        // host.tld/viewcache.php?wp=geocode
        final String secondLevel = StringUtils.substringAfter(url, shortHost + "/viewcache.php?wp=");
        return canHandle(secondLevel) ? secondLevel : super.getGeocodeFromUrl(url);
    }

    @Override
    @Nullable
    public String getCreateAccountUrl() {
        return getSchemeAndHost() + "/register.php";
    }

    @Override
    public List<Smiley> getSmileys() {
        return OCSmileysProvider.getSmileys();
    }

    @Override
    public boolean getHttps() {
        return https;
    }

    /**
     * Return the scheme part including the colon and the slashes.
     *
     * @return either "https://" or "http://"
     */
    protected String getSchemePart() {
        return https ? "https://" : "http://";
    }

    /**
     * Return the scheme part and the host (e.g., "https://opencache.uk").
     */
    protected String getSchemeAndHost() {
        return getSchemePart() + host;
    }
}
