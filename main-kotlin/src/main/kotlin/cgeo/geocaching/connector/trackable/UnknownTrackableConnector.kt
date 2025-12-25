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

import cgeo.geocaching.connector.UserAction
import cgeo.geocaching.models.Trackable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collections
import java.util.List

class UnknownTrackableConnector : AbstractTrackableConnector() {

    override     public Boolean canHandleTrackable(final String geocode) {
        return false
    }

    override     public String getServiceTitle() {
        throw IllegalStateException("this connector does not have a corresponding name.")
    }

    override     public Boolean hasTrackableUrls() {
        return false
    }

    override     public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return null
    }

    override     public TrackableBrand getBrand() {
        return TrackableBrand.UNKNOWN
    }

    override     public List<UserAction> getUserActions(final UserAction.UAContext user) {
        return Collections.emptyList()
    }

    override     public String getHost() {
        throw IllegalStateException("Unknown trackable connector does not have a host.")
    }

    override     public String getHostUrl() {
        throw IllegalStateException("Unknown trackable connector does not have a host url.")
    }
}
