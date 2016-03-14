package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.StatusCode;

public class GCAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {


    public static final CredentialsAuthParameters GEOCACHING_CREDENTIAL_AUTH_PARAMS = new CredentialsAuthParameters(
        GCConnector.getInstance().getCreateAccountUrl(),
        (Settings.getCredentials(GCConnector.getInstance()).isValid() ? Settings.getCredentials(GCConnector.getInstance()).getUserName() : ""));

    @Override
    protected Credentials getCredentials() {
        return Settings.getCredentials(GCConnector.getInstance());
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
        Credentials currentCredentials = getCredentials();
        if (currentCredentials.isInvalid() ||
                !currentCredentials.getUserName().equals(credentials.getUserName()) ||
                !currentCredentials.getPassword().equals(credentials.getPassword())) {
            // Force Logout before trying new credentials
            GCLogin.getInstance().logout();
        }
        return GCLogin.getInstance().login(credentials);
    }
}
