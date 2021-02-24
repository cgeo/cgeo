package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.ec.ECLogin;
import cgeo.geocaching.connector.lc.LCConnector;
import cgeo.geocaching.connector.lc.LCLogin;
import cgeo.geocaching.enumerations.StatusCode;

public class LCAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {

    @Override
    protected String getCreateAccountUrl() {
        return ECConnector.getInstance().getCreateAccountUrl();
    }

    @Override
    protected void setCredentials(final Credentials credentials) {
        Settings.setCredentials(LCConnector.getInstance(), credentials);
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(R.string.settings_title_lc);
    }

    @Override
    protected StatusCode checkCredentials(final Credentials credentials) {
        return LCLogin.getInstance().login(credentials);
    }
}
