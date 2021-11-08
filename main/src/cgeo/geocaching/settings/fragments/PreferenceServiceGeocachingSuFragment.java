package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceGeocachingSuFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_geocaching_su, rootKey);

        // Open website Preference
        Preference openWebsite = findPreference(getString(R.string.pref_fakekey_gc_website));
        String urlOrHost = SuConnector.getInstance().getHost();
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        // TODO
        Preference auth = findPreference(getString(R.string.pref_fakekey_su_authorization));
        if (auth != null) {
            auth.setTitle(
                getString(Settings.hasOAuthAuthorization(R.string.pref_su_tokenpublic, R.string.pref_su_tokensecret)
                    ? R.string.settings_reauthorize : R.string.settings_authorize));
        }

        // TODO
        Preference username = findPreference(getString(R.string.pref_fakekey_su_authorization));
        if (username != null) {
            username.setSummary(
                getString(Settings.hasOAuthAuthorization(R.string.pref_su_tokenpublic, R.string.pref_su_tokensecret)
                    ? R.string.auth_connected : R.string.auth_unconnected));
        }
    }
}
