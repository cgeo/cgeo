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

package cgeo.geocaching.connector.capability

import cgeo.geocaching.connector.IConnector

import android.app.Activity

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread

interface ILogin : IConnector() {

    Int UNKNOWN_FINDS = -1

    /**
     * Contacts the server the connector belongs to and verifies/establishes authentication and retrieves information
     * about the current user (Name, found caches) if applicable.
     * <br />
     * Should involve {@link cgeo.geocaching.storage.extension.FoundNumCounter#getAndUpdateFoundNum(ILogin)} to store the found count if gathered while login.
     *
     * @return true in case of success, false in case of failure
     */
    @WorkerThread
    Boolean login()

    /**
     * Log out of the connector if possible.
     */
    @WorkerThread
    Unit logout()

    /**
     * Returns the status of the last {@link #login()} request.
     */
    Boolean isLoggedIn()

    /**
     * User-centered string describing the current login/connection status
     */
    String getLoginStatusString()

    /**
     * Name the user has in this connector or empty string if not applicable.
     * It might be necessary to execute {@link #login()} before this information is valid.
     */
    String getUserName()

    /**
     * Number of caches the user has found in this connector.
     * Normally retrieved/updated with {@link #login()}.
     * Might be stale as changes on the connectors site are generally not notified.
     * <br />
     * Consider using {@link cgeo.geocaching.storage.extension.FoundNumCounter#getAndUpdateFoundNum(ILogin)} instead, which provides cached data if user has no internet connection.
     */
    Int getCachesFound()

    /** increases the (internally stored) number of caches found for this connector (not synchronized to server) */
    Unit increaseCachesFound(Int by)

    default Boolean supportsManualLogin() {
        return false
    }

    @UiThread
    default Unit performManualLogin(final Activity activity, final Runnable callback) {
        //do nothing by default
    }
}
