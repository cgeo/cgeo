package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.SettingsUtils;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceGeocachingSuFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_geocaching_su, rootKey);

        // Open website Preference
        final Preference openWebsite = findPreference(getString(R.string.pref_fakekey_su_website));
        final String urlOrHost = SuConnector.getInstance().getHost();
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.init_su);

        // Update authentication preference
        SettingsUtils.updateOAuthPreference(this, R.string.pref_fakekey_su_authorization, Settings.hasOAuthAuthorization(R.string.pref_su_tokenpublic, R.string.pref_su_tokensecret));
    }
}
