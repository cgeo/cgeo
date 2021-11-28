package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.HtmlImage;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import org.apache.commons.lang3.StringUtils;

public class CredentialsPreference extends AbstractClickablePreference {

    private static final int NO_KEY = -1;

    private LayoutInflater inflater;
    private final CredentialActivityMapping credentialsMapping;

    private enum CredentialActivityMapping {
        GEOCACHING(R.string.pref_fakekey_gc_authorization, GCAuthorizationActivity.class, GCConnector.getInstance()),
        EXTREMCACHING(R.string.pref_fakekey_ec_authorization, ECAuthorizationActivity.class, ECConnector.getInstance()),
        GCVOTE(R.string.pref_fakekey_gcvote_authorization, GCVoteAuthorizationActivity.class, GCVote.getInstance());

        public final int prefKeyId;
        private final Class<?> authActivity;
        private final ICredentials connector;

        CredentialActivityMapping(final int prefKeyId, @NonNull final Class<?> authActivity, @NonNull final ICredentials connector) {
            this.prefKeyId = prefKeyId;
            this.authActivity = authActivity;
            this.connector = connector;
        }

        public Class<?> getAuthActivity() {
            return authActivity;
        }

        public ICredentials getConnector() {
            return connector;
        }
    }

    private CredentialActivityMapping getAuthorization() {
        final String prefKey = getKey();
        for (final CredentialActivityMapping auth : CredentialActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey.equals(CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth;
            }
        }
        throw new IllegalStateException("Invalid authorization preference");
    }

    public CredentialsPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.credentialsMapping = getAuthorization();
        init(context);
    }

    public CredentialsPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.credentialsMapping = getAuthorization();
        init(context);
    }

    private void init(final Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity settingsActivity) {
        //settingsActivity.setAuthTitle(credentialsMapping.prefKeyId);
        return preference -> {
            final Intent checkIntent = new Intent(preference.getContext(), credentialsMapping.getAuthActivity());

            final Credentials credentials = credentialsMapping.getConnector().getCredentials();
            checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, credentials.getUsernameRaw());
            checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, credentials.getPasswordRaw());

            settingsActivity.startActivityForResult(checkIntent, credentialsMapping.prefKeyId);
            return false; // no shared preference has to be changed
        };
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        addInfoIcon(holder);
    }

    /**
     * Display avatar image if present
     */
    private void addInfoIcon(final PreferenceViewHolder holder) {
        final String avatarUrl = Settings.getAvatarUrl(credentialsMapping.getConnector());
        if (StringUtils.isEmpty(avatarUrl)) {
            return;
        }

        //final ImageView iconView = (ImageView) inflater.inflate(R.layout.preference_info_icon, holder, false);
        final HtmlImage imgGetter = new HtmlImage(HtmlImage.SHARED, false, false, false);
        //iconView.setImageDrawable(imgGetter.getDrawable(avatarUrl));

        final LinearLayout frame = (LinearLayout) holder.findViewById(android.R.id.widget_frame);
        frame.setVisibility(View.VISIBLE);
        //frame.addView(iconView);

        final LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.5f);
        frame.setLayoutParams(param);
    }

    @Override
    protected boolean isAuthorized() {
        return Settings.getCredentials(credentialsMapping.getConnector()).isValid();
    }

    @Override
    protected void revokeAuthorization() {
        if (credentialsMapping == CredentialActivityMapping.GEOCACHING) {
            Cookies.clearCookies();
        }
        Settings.setCredentials(credentialsMapping.getConnector(), Credentials.EMPTY);
        Settings.setAvatarUrl(credentialsMapping.getConnector(), StringUtils.EMPTY);
    }
}
