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

package cgeo.geocaching.connector.wm

import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

class WaymarkingConnector : AbstractConnector() {

    override     public String getName() {
        return "Waymarking"
    }

    override     public String getNameAbbreviated() {
        return "WM"
    }

    override     public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode()
    }

    override     public String getHost() {
        return "www.waymarking.com"
    }

    override     public Boolean isOwner(final Geocache cache) {
        // this connector has no user management
        return false
    }

    override     protected String getCacheUrlPrefix() {
        return "https://" + getHost() + "/waymarks/"
    }

    override     public Boolean canHandle(final String geocode) {
        return StringUtils.startsWith(geocode, "WM")
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"WM%"}
    }

    override     public String getGeocodeFromUrl(final String url) {
        // coord.info URLs
        val topLevel: String = StringUtils.substringAfterLast(url, "coord.info/")
        if (canHandle(topLevel)) {
            return topLevel
        }
        // waymarking URLs https://www.waymarking.com/waymarks/WMNCDT_American_Legion_Flagpole_1983_University_of_Oregon
        val waymark: String = StringUtils.substringBetween(url, "waymarks/", "_")
        return waymark != null && canHandle(waymark) ? waymark : null
    }
}
