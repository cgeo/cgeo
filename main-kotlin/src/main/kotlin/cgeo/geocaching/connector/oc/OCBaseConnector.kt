// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.oc

import cgeo.geocaching.R
import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.models.Geocache

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

class OCBaseConnector : AbstractConnector() {

    private final String host
    private final Boolean https
    private final String name
    private final Pattern codePattern
    private final String[] sqlLikeExpressions
    protected final String abbreviation

    public OCBaseConnector(final String name, final String host, final Boolean https, final String prefix, final String abbreviation) {
        this.name = name
        this.host = host
        this.https = https
        this.abbreviation = abbreviation
        codePattern = Pattern.compile(prefix + "[A-Z0-9]+", Pattern.CASE_INSENSITIVE)
        sqlLikeExpressions = String[]{prefix + "%"}
    }

    override     public Boolean canHandle(final String geocode) {
        return codePattern.matcher(geocode).matches()
    }

    override     public String[] getGeocodeSqlLikeExpressions() {
        return sqlLikeExpressions
    }

    override     public String getName() {
        return name
    }

    override     public String getNameAbbreviated() {
        return abbreviation
    }

    override     public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode()
    }

    override     public String getHost() {
        return host
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     protected String getCacheUrlPrefix() {
        return getSchemeAndHost() + "/viewcache.php?wp="
    }

    override     public Int getCacheMapMarkerId() {
        return R.drawable.marker_oc
    }

    override     public Int getCacheMapMarkerBackgroundId() {
        return R.drawable.background_oc
    }

    override     public Int getCacheMapDotMarkerId() {
        return R.drawable.dot_marker_oc
    }

    override     public Int getCacheMapDotMarkerBackgroundId() {
        return R.drawable.dot_background_oc
    }

    override     public String getGeocodeFromUrl(final String url) {
        // different opencaching installations have different supported URLs

        // host.tld/geocode
        val shortHost: String = getShortHost()
        val uri: Uri = Uri.parse(url)
        if (!StringUtils.containsIgnoreCase(uri.getHost(), shortHost)) {
            return null
        }
        val path: String = uri.getPath()
        if (StringUtils.isBlank(path)) {
            return null
        }
        val firstLevel: String = path.substring(1)
        if (canHandle(firstLevel)) {
            return firstLevel
        }

        // host.tld/viewcache.php?wp=geocode
        val secondLevel: String = path.startsWith("/viewcache.php") ? uri.getQueryParameter("wp") : ""
        return (secondLevel != null && canHandle(secondLevel)) ? secondLevel : super.getGeocodeFromUrl(url)
    }

    override     public Boolean isHttps() {
        return https
    }

    /**
     * Return the scheme part including the colon and the slashes.
     *
     * @return either "https://" or "http://"
     */
    protected String getSchemePart() {
        return https ? "https://" : "http://"
    }

    /**
     * Return the scheme part and the host (e.g., "https://opencache.uk").
     */
    protected String getSchemeAndHost() {
        return getSchemePart() + host
    }

}
