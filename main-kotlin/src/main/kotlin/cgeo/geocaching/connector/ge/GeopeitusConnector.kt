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

package cgeo.geocaching.connector.ge

import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

class GeopeitusConnector : AbstractConnector() {

    override     public String getName() {
        return "geopeitus.ee"
    }

    override     public String getNameAbbreviated() {
        return getName()
    }

    override     public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + StringUtils.stripStart(cache.getGeocode().substring(2), "0")
    }

    override     public String getHost() {
        return "www.geopeitus.ee"
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     public Boolean canHandle(final String geocode) {
        return StringUtils.startsWith(geocode, "GE") && isNumericId(geocode.substring(2))
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"GE%"}
    }


    override     protected String getCacheUrlPrefix() {
        return getHostUrl() + "/aare/"
    }
}
