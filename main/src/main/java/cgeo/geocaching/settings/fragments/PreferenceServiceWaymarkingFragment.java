package cgeo.geocaching.settings.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.wm.WMConnector;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.PreferenceUtils;
import cgeo.geocaching.utils.ShareUtils;

public class PreferenceServiceWaymarkingFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(
                R.xml.preferences_services_waymarking,
                rootKey);

        // Open website Preference
        final Preference openWebsite = findPreference(getString(R.string.pref_fakekey_wm_website));
        final String urlOrHost = WMConnector.getInstance().getHost();
        PreferenceUtils.setOnPreferenceClickListener(openWebsite, preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        initWmServicePreference(Settings.isGCConnectorActive());
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.init_wm);
    }

    private void initWmServicePreference(final boolean gcConnectorActive) {
        PreferenceUtils.setSummary(findPreference(getString((R.string.preference_screen_wm))), getWmServiceSummary(Settings.isWMConnectorActive(), gcConnectorActive));
        if (gcConnectorActive) {
            PreferenceUtils.setEnabled(findPreference(getString(R.string.pref_connectorWMActive)), true);
        }
    }

    private String getWmServiceSummary(final boolean wmConnectorActive, final boolean gcConnectorActive) {
        if (!wmConnectorActive) {
            return StringUtils.EMPTY;
        }

        //lc service is set to active by user. Check whether it can actually be actived due to GC conditions
        final int wmStatusTextId = gcConnectorActive ? R.string.settings_service_active : R.string.settings_service_active_unavailable;

        return CgeoApplication.getInstance().getString(wmStatusTextId);
    }
}
