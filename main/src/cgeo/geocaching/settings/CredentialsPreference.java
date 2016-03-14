package cgeo.geocaching.settings;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.settings.AbstractCredentialsAuthorizationActivity.CredentialsAuthParameters;
import cgeo.geocaching.R;
import org.eclipse.jdt.annotation.NonNull;

public class CredentialsPreference extends AbstractClickablePreference {

    private static final int NO_KEY = -1;

    private enum CredentialActivityMapping {
        NONE(NO_KEY, null, null),
        GEOCACHING(R.string.pref_fakekey_gc_authorization, GCAuthorizationActivity.class, GCAuthorizationActivity.GEOCACHING_CREDENTIAL_AUTH_PARAMS),
        EXTREMCACHING(R.string.pref_fakekey_ec_authorization, ECAuthorizationActivity.class, ECAuthorizationActivity.EXTREMCACHING_CREDENTIAL_AUTH_PARAMS),
        GCVOTE(R.string.pref_fakekey_gcvote_authorization, GCVoteAuthorizationActivity.class, GCVoteAuthorizationActivity.GCVOTE_CREDENTIAL_AUTH_PARAMS);

        public final int prefKeyId;
        private final Class<?> authActivity;
        private final CredentialsAuthParameters credentialsParams;

        CredentialActivityMapping(final int prefKeyId, @NonNull final Class<?> authActivity, @NonNull final CredentialsAuthParameters credentialsParams) {
            this.prefKeyId = prefKeyId;
            this.authActivity = authActivity;
            this.credentialsParams = credentialsParams;
        }

        public Class<?> getAuthActivity() {
            return authActivity;
        }

        public CredentialsAuthParameters getCredentialsParams() {
            return credentialsParams;
        }
    }

    private CredentialActivityMapping getAuthorization() {
        final String prefKey = getKey();
        for (final CredentialActivityMapping auth : CredentialActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey.equals(CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth;
            }
        }
        return CredentialActivityMapping.NONE;
    }

    private final CredentialActivityMapping credentialsMapping;

    public CredentialsPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.credentialsMapping = getAuthorization();
    }

    public CredentialsPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.credentialsMapping = getAuthorization();
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity settingsActivity) {
        settingsActivity.setAuthTitle(credentialsMapping.prefKeyId);
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                if (credentialsMapping != CredentialActivityMapping.NONE) {
                    final Intent checkIntent = new Intent(preference.getContext(), credentialsMapping.getAuthActivity());
                    credentialsMapping.getCredentialsParams().setCredentialsAuthExtras(checkIntent);
                    settingsActivity.startActivityForResult(checkIntent, credentialsMapping.prefKeyId);
                }
                return false; // no shared preference has to be changed
            }
        };
    }
}
