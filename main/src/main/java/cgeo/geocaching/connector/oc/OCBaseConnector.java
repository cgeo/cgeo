package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.models.Geocache;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class OCBaseConnector extends AbstractConnector {

    @NonNull
    private final String host;
    private final boolean https;
    @NonNull
    private final String name;
    private final Pattern codePattern;
    private final String[] sqlLikeExpressions;
    @NonNull
    protected final String abbreviation;

    public OCBaseConnector(@NonNull final String name, @NonNull final String host, final boolean https, final String prefix, @NonNull final String abbreviation) {
        this.name = name;
        this.host = host;
        this.https = https;
        this.abbreviation = abbreviation;
        codePattern = Pattern.compile(prefix + "[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
        sqlLikeExpressions = new String[]{prefix + "%"};
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return codePattern.matcher(geocode).matches();
    }

    @NonNull
    @Override
    public String[] getGeocodeSqlLikeExpressions() {
        return sqlLikeExpressions;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public String getNameAbbreviated() {
        return abbreviation;
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
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getSchemeAndHost() + "/viewcache.php?wp=";
    }

    @Override
    public int getCacheMapMarkerId() {
        return R.drawable.marker_oc;
    }

    @Override
    public int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_oc;
    }

    @Override
    public int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_oc;
    }

    @Override
    public int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_oc;
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        // different opencaching installations have different supported URLs

        // host.tld/geocode
        final String shortHost = getShortHost();
        final Uri uri = Uri.parse(url);
        if (!StringUtils.containsIgnoreCase(uri.getHost(), shortHost)) {
            return null;
        }
        final String path = uri.getPath();
        if (StringUtils.isBlank(path)) {
            return null;
        }
        final String firstLevel = path.substring(1);
        if (canHandle(firstLevel)) {
            return firstLevel;
        }

        // host.tld/viewcache.php?wp=geocode
        final String secondLevel = path.startsWith("/viewcache.php") ? uri.getQueryParameter("wp") : "";
        return (secondLevel != null && canHandle(secondLevel)) ? secondLevel : super.getGeocodeFromUrl(url);
    }

    @Override
    public boolean isHttps() {
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
