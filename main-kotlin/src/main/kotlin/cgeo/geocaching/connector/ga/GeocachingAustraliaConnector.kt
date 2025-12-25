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

package cgeo.geocaching.connector.ga

import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.NotNull

class GeocachingAustraliaConnector : AbstractConnector() {

    override     public String getName() {
        return "Geocaching Australia"
    }

    override     public String getNameAbbreviated() {
        return "GCAU"
    }

    override     public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode()
    }

    override     public String getHost() {
        return "geocaching.com.au"
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     public Boolean canHandle(final String geocode) {
        return (StringUtils.startsWithIgnoreCase(geocode, "GA") || StringUtils.startsWithIgnoreCase(geocode, "TP")) && isNumericId(geocode.substring(2))
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"GA%", "TP%"}
    }


    override     protected String getCacheUrlPrefix() {
        return getHostUrl() + "/cache/"
    }
}
