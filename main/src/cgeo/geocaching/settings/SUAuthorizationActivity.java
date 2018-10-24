package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.connector.su.SuLogin;
import cgeo.geocaching.enumerations.StatusCode;

public class SUAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {

    @Override
    protected String getCreateAccountUrl() {
        return SuConnector.getInstance().getCreateAccountUrl();
    }

    @Override
    protected void setCredentials(final Credentials credentials) {
        Settings.setCredentials(SuConnector.getInstance(), credentials);
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(R.string.settings_title_su);
    }

    @Override
    protected StatusCode checkCredentials(final Credentials credentials) {
        return SuLogin.getInstance().login(credentials);
    }
}
