package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.HtmlImage;

public class GCAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {


    public static final CredentialsAuthParameters GEOCACHING_CREDENTIAL_AUTH_PARAMS = new CredentialsAuthParameters(
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

        StatusCode status = GCLogin.getInstance().login(credentials);
        if (status == StatusCode.NO_ERROR) {
            // Obtain avatar URL
            final String avatarUrl = GCLogin.getInstance().getAvatarUrl();
            // Force refresh/store avatar
            final HtmlImage imgGetter = new HtmlImage(HtmlImage.SHARED, false, false, true);
            imgGetter.getDrawable(avatarUrl);

            // Save avatar url
            Settings.setAvatarUrl(GCConnector.getInstance(), avatarUrl);
        }
        return  status;
    }
}
