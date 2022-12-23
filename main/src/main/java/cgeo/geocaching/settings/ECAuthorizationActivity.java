package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.ec.ECLogin;
import cgeo.geocaching.enumerations.StatusCode;

public class ECAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {

    @Override
    protected String getCreateAccountUrl() {
        return ECConnector.getInstance().getCreateAccountUrl();
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
