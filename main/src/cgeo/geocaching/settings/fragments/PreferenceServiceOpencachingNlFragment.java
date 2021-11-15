package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.OCPreferenceKeys;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceOpencachingNlFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_opencaching_nl, rootKey);

        // Open website Preference
        final Preference openWebsite = findPreference(getString(R.string.pref_fakekey_ocnl_website));
        final String urlOrHost = OCPreferenceKeys.OC_NL.authParams.host;
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        // TODO
        final OCPreferenceKeys key = OCPreferenceKeys.getByAuthId(R.string.pref_fakekey_ocnl_authorization);
        if (key != null) {
            findPreference(getString(key.authPrefId)).setTitle(getString(Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId) ? R.string.settings_reauthorize : R.string.settings_authorize));
            findPreference(getString(R.string.pref_fakekey_ocnl_authorization)).setSummary(getString(Settings.hasOAuthAuthorization(key.publicTokenPrefId, key.privateTokenPrefId) ? R.string.auth_connected : R.string.auth_unconnected));
        } else {
            findPreference(getString(R.string.pref_fakekey_ocnl_authorization)).setSummary(getString(R.string.auth_unconnected));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.init_oc_nl);
    }
}
