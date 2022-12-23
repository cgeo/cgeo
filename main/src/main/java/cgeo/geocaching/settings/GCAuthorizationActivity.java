package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.StatusCode;

public class GCAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {

    @Override
    protected String getCreateAccountUrl() {
        return GCConnector.getInstance().getCreateAccountUrl();
    }

    @Override
    protected void setCredentials(final Credentials credentials) {
        Settings.setCredentials(GCConnector.getInstance(), credentials);
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(R.string.settings_title_gc);
    }

    @Override
    protected StatusCode checkCredentials(final Credentials credentials) {
        final Credentials currentCredentials = GCConnector.getInstance().getCredentials();
        if (currentCredentials.isInvalid() ||
                !currentCredentials.getUserName().equals(credentials.getUserName()) ||
                !currentCredentials.getPassword().equals(credentials.getPassword())) {
            // Force Logout before trying new credentials
            GCLogin.getInstance().logout();
        }

        final StatusCode status = GCLogin.getInstance().login(credentials);
        if (status == StatusCode.NO_ERROR) {
            GCLogin.getInstance().getServerParameters(); // This will initialize some settings
        }
        return status;
    }
}
