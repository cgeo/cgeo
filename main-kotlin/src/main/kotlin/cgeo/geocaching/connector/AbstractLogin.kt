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

package cgeo.geocaching.connector

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.connector.gc.GCMemberState
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.network.Cookies
import cgeo.geocaching.network.Network
import cgeo.geocaching.settings.Credentials
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.connector.capability.ILogin.UNKNOWN_FINDS

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import org.apache.commons.lang3.StringUtils

abstract class AbstractLogin {

    /**
     * {@code true} if logged in, {@code false} otherwise
     */
    private var actualLoginStatus: Boolean = false
    private var actualUserName: String = StringUtils.EMPTY
    /**
     * Number of caches found. An unknown number is signaled by the value -1, while 0 really indicates zero caches found
     * by the user.
     */
    private var actualCachesFound: Int = UNKNOWN_FINDS
    /**
     * use "not logged in" as default. Connectors should update there status as soon as they start establishing a connection.
     */
    private var actualStatus: String = LocalizationUtils.getString(R.string.init_login_popup_not_logged_in)

    public Unit setActualCachesFound(final Int found) {
        actualCachesFound = found
    }

    public String getActualStatus() {
        return actualStatus
    }

    protected Unit setActualStatus(final String status) {
        actualStatus = status
    }

    public Boolean isActualLoginStatus() {
        return actualLoginStatus
    }

    protected Unit setActualLoginStatus(final Boolean loginStatus) {
        actualLoginStatus = loginStatus
    }

    public String getActualUserName() {
        return actualUserName
    }

    protected Unit setActualUserName(final String userName) {
        actualUserName = userName
    }

    public Int getActualCachesFound() {
        return actualCachesFound
    }

    protected Unit resetLoginStatus() {
        Settings.setGCMemberStatus(GCMemberState.UNKNOWN)
        Cookies.clearCookies()

        setActualLoginStatus(false)
    }

    protected Unit clearLoginInfo() {
        resetLoginStatus()

        setActualCachesFound(-1)
        setActualStatus(CgeoApplication.getInstance().getString(R.string.err_login))
    }

    @WorkerThread
    public StatusCode login() {
        return login(null)
    }

    @WorkerThread
    public StatusCode login(final Credentials credentials) {
        if (!Network.isConnected()) {
            return StatusCode.COMMUNICATION_ERROR
        }
        if (credentials == null) {
            return login(true)
        }
        return login(true, credentials)
    }

    @WorkerThread
    protected abstract StatusCode login(Boolean retry)

    @WorkerThread
    protected abstract StatusCode login(Boolean retry, Credentials credentials)

    public Unit increaseActualCachesFound(final Int by) {
        if (actualCachesFound >= 0) {
            actualCachesFound += by
        }
    }

}
