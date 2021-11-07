package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceGeokretyOrgFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_geokrety_org, rootKey);

        // Open website Preference
        Preference openWebsite = findPreference(getString(R.string.pref_fakekey_geokrety_website));
        String urlOrHost = "https://geokrety.org";
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        // Open website Map Preference
        Preference openWebsite2 = findPreference(getString(R.string.pref_fakekey_geokretymap_website));
        String urlOrHost2 = "https://geokretymap.org";
        openWebsite2.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost2, "http") ? urlOrHost2 : "http://" + urlOrHost2;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        // TODO
        findPreference(getString(R.string.pref_fakekey_geokrety_authorization))
            .setTitle(getString(
                Settings.hasGeokretyAuthorization() ? R.string.settings_reauthorize : R.string.settings_authorize));
    }
}
