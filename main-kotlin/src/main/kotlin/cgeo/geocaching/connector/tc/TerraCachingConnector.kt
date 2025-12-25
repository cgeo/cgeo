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

package cgeo.geocaching.connector.tc

import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.regex.Pattern

import org.jetbrains.annotations.NotNull

class TerraCachingConnector : AbstractConnector() {

    private static val PATTERN_GEOCODE: Pattern = Pattern.compile("(TC|CC|LC)[0-9A-Z]{1,4}", Pattern.CASE_INSENSITIVE)

    override     public String getName() {
        return "TerraCaching"
    }

    override     public String getNameAbbreviated() {
        return "TC"
    }

    override     public String getCacheUrl(final Geocache cache) {
        return getCacheUrlPrefix() + cache.getGeocode()
    }

    override     public String getHost() {
        return "www.terracaching.com/"
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     protected String getCacheUrlPrefix() {
        return "https://play.terracaching.com/Cache/"
    }

    override     public Boolean canHandle(final String geocode) {
        return PATTERN_GEOCODE.matcher(geocode).matches()
    }

    @NotNull
    override     public String[] getGeocodeSqlLikeExpressions() {
        return String[]{"TC%", "CC%", "LC%"}
    }

}
