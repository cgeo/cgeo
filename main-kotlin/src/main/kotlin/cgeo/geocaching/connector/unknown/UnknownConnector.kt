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

package cgeo.geocaching.connector.unknown

import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

class UnknownConnector : AbstractConnector() {

    override     public String getName() {
        return "Unknown caches"
    }

    override     public String getNameAbbreviated() {
        throw IllegalStateException("no valid name for unknown connector")
    }

    override     public String getCacheUrl(final Geocache cache) {
        return null
    }

    override     public String getHost() {
        return StringUtils.EMPTY; // we have no host for these caches
    }

    override     public Boolean isOwner(final Geocache cache) {
        return false
    }

    override     public Boolean canHandle(final String geocode) {
        return StringUtils.isNotBlank(geocode)
    }

    override     protected String getCacheUrlPrefix() {
        throw IllegalStateException("getCacheUrl cannot be called on unknown caches")
    }

    override     public String getGeocodeFromUrl(final String url) {
        return null
    }

    override     public Boolean supportsSettingFoundState() {
        return true
    }
}
