package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.ec.ECLogin;
import cgeo.geocaching.enumerations.StatusCode;

public class ECAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {

    public static final CredentialsAuthParameters EXTREMCACHING_CREDENTIAL_AUTH_PARAMS = new CredentialsAuthParameters(
            Settings.getCredentials(ECConnector.getInstance()).getUsernameRaw());

    @Override
    protected Credentials getCredentials() {
        return Settings.getCredentials(ECConnector.getInstance());
    }

    @Override
    protected void setCredentials(final Credentials credentials) {
        Settings.setCredentials(ECConnector.getInstance(), credentials);
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(R.string.settings_title_ec);
    }

    @Override
    protected StatusCode checkCredentials(final Credentials credentials) {
        return ECLogin.getInstance().login(credentials);
    }
}
