package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.OCPreferenceKeys;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceOpencacheUkFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_opencache_uk, rootKey);

        // Open website Preference
        Preference openWebsite = findPreference(getString(R.string.pref_fakekey_ocuk_website));
        String urlOrHost = OCPreferenceKeys.OC_UK.authParams.host;
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        // TODO
        final OCPreferenceKeys key = OCPreferenceKeys.getByAuthId(R.string.pref_fakekey_ocuk_authorization);
        if (key != null) {
            findPreference(getString(key.authPrefId)).setTitle(getString(Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId) ? R.string.settings_reauthorize : R.string.settings_authorize));
            findPreference(getString(R.string.pref_fakekey_ocuk_authorization)).setSummary(getString(Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId) ? R.string.auth_connected : R.string.auth_unconnected));
        } else {
            findPreference(getString(R.string.pref_fakekey_ocuk_authorization)).setSummary(getString(R.string.auth_unconnected));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.init_oc_uk);
    }
}
