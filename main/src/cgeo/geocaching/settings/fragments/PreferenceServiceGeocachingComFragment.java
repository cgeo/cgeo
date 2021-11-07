package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceGeocachingComFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_geocaching_com, rootKey);

        setAuthTitle(R.string.pref_fakekey_gc_authorization, GCConnector.getInstance());
        setConnectedUsernameTitle(R.string.pref_fakekey_gc_authorization, GCConnector.getInstance());
        initBasicMemberPreferences();

        // Authentication Preference
        final GCConnector connector = GCConnector.getInstance();
        final Credentials credentials = Settings.getCredentials(connector);
        findPreference(getString(R.string.pref_fakekey_gc_authorization)).setTitle(getString(StringUtils.isNotBlank(credentials.getUsernameRaw()) ? R.string.settings_reauthorize : R.string.settings_authorize));
        findPreference(getString(R.string.pref_fakekey_gc_authorization)).setSummary(credentials.isValid() ? getString(R.string.auth_connected_as, credentials.getUserName()) : getString(R.string.auth_unconnected));

        // Open website Preference
        Preference openWebsite = findPreference(getString(R.string.pref_fakekey_gc_website));
        String urlOrHost = GCConnector.getInstance().getHost();
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        // Facebook Login Hint
        Preference login_facebook = findPreference(getString(R.string.pref_gc_fb_login_hint));
        login_facebook.setOnPreferenceClickListener(preference -> {
            final AlertDialog.Builder builder = Dialogs.newBuilder(getContext());
            builder.setMessage(R.string.settings_info_facebook_login)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.settings_info_facebook_login_title)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .setNegativeButton(R.string.more_information,
                    (dialog, id) -> {
                        ShareUtils.openUrl(getContext(), getString(R.string.settings_facebook_login_url));
                });
            builder.create().show();
            return true;
        });
    }

    void initBasicMemberPreferences() {
        findPreference(getString((R.string.preference_screen_basicmembers)))
            .setEnabled(!Settings.isGCPremiumMember());
        findPreference(getString((R.string.pref_loaddirectionimg)))
            .setEnabled(!Settings.isGCPremiumMember());
    }

    private void setConnectedUsernameTitle(final int prefKeyId, @NonNull final ICredentials connector) {
        final Credentials credentials = Settings.getCredentials(connector);

        findPreference(getString(prefKeyId))
            .setSummary(credentials.isValid()
                ? getString(R.string.auth_connected_as, credentials.getUserName())
                : getString(R.string.auth_unconnected));
    }

    private void setAuthTitle(final int prefKeyId, @NonNull final ICredentials connector) {
        final Credentials credentials = Settings.getCredentials(connector);

        findPreference(getString(prefKeyId))
            .setTitle(getString(StringUtils.isNotBlank(credentials.getUsernameRaw())
                ? R.string.settings_reauthorize
                : R.string.settings_authorize));
    }
}
