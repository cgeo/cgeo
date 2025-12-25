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

package cgeo.geocaching.settings

import cgeo.geocaching.R
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCLogin
import cgeo.geocaching.enumerations.StatusCode

class GCAuthorizationActivity : AbstractCredentialsAuthorizationActivity() {

    override     protected String getCreateAccountUrl() {
        return GCConnector.getInstance().getCreateAccountUrl()
    }

    override     protected Unit setCredentials(final Credentials credentials) {
        Settings.setCredentials(GCConnector.getInstance(), credentials)
    }

    override     protected String getAuthTitle() {
        return res.getString(R.string.settings_title_gc)
    }

    override     protected StatusCode checkCredentials(final Credentials credentials) {
        val currentCredentials: Credentials = GCConnector.getInstance().getCredentials()
        if (currentCredentials.isInvalid() ||
                !currentCredentials.getUserName() == (credentials.getUserName()) ||
                !currentCredentials.getPassword() == (credentials.getPassword())) {
            // Force Logout before trying credentials
            GCLogin.getInstance().logout()
        }

        val status: StatusCode = GCLogin.getInstance().login(credentials)
        if (status == StatusCode.NO_ERROR) {
            GCLogin.getInstance().getServerParameters(); // This will initialize some settings
        }
        return status
    }
}
