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

package cgeo.geocaching.connector.trackable

import cgeo.geocaching.connector.AbstractConnector
import cgeo.geocaching.connector.UserAction
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.models.Trackable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collections
import java.util.List

abstract class AbstractTrackableConnector : TrackableConnector {

    override     public Int getPreferenceActivity() {
        return 0
    }

    override     public Boolean isLoggable() {
        return false
    }

    override     public Boolean canHandleTrackable(final String geocode, final TrackableBrand brand) {
        if (brand == null || brand == TrackableBrand.UNKNOWN) {
            return canHandleTrackable(geocode)
        }
        return brand == getBrand() && canHandleTrackable(geocode)
    }

    override     public Boolean hasTrackableUrls() {
        return true
    }

    override     public String getTrackableCodeFromUrl(final String url) {
        return null
    }

    override     public String getTrackableTrackingCodeFromUrl(final String url) {
        return null
    }

    override     public List<UserAction> getUserActions(final UserAction.UAContext user) {
        return AbstractConnector.getDefaultUserActions()
    }

    override     public String getUrl(final Trackable trackable) {
        throw IllegalStateException("this trackable does not have a corresponding URL")
    }

    override     public String getLogUrl(final LogEntry logEntry) {
        return null; //by default, Connector does not support log urls
    }

    override     public String getHostUrl() {
        return "https://" + getHost()
    }

    override     public String getTestUrl() {
        return getHostUrl()
    }

    override     public String getProxyUrl() {
        return null
    }

    override     public List<Trackable> searchTrackables(final String geocode) {
        return Collections.emptyList()
    }

    override     public List<Trackable> loadInventory() {
        return Collections.emptyList()
    }

    override     public Boolean isGenericLoggable() {
        return false
    }

    override     public Boolean isActive() {
        return false
    }

    override     public Boolean isRegistered() {
        return false
    }

    override     public Boolean recommendLogWithGeocode() {
        return false
    }

    override     public TrackableLoggingManager getTrackableLoggingManager(final String tbCode) {
        return null
    }
}
