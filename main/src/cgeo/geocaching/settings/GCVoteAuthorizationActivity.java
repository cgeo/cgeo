package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteLogin;

public class GCVoteAuthorizationActivity extends AbstractCredentialsAuthorizationActivity {

    public static final CredentialsAuthParameters GCVOTE_CREDENTIAL_AUTH_PARAMS = new CredentialsAuthParameters(
            Settings.getGCVoteLogin().getUsernameRaw());

    @Override
    protected Credentials getCredentials() {
        return Settings.getGCVoteLogin();
    }

    @Override
    protected void setCredentials(final Credentials credentials) {
        Settings.setCredentials(GCVote.getInstance(), credentials);
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(R.string.init_gcvote);
    }

    @Override
    protected StatusCode checkCredentials(final Credentials credentials) {
        return GCVoteLogin.getInstance().login(credentials);
    }
}
