package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.al.ALConnector;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceGeocachingComAdventureLabsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(
                R.xml.preferences_services_geocaching_com_adventure_lab,
                rootKey);

        // Open website Preference
        final Preference openWebsite = findPreference(getString(R.string.pref_fakekey_al_website));
        final String urlOrHost = ALConnector.getInstance().getHost();
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        initLCServicePreference(Settings.isGCConnectorActive());
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_lc);
    }

    private void initLCServicePreference(final boolean gcConnectorActive) {
        final boolean isActiveGCPM = gcConnectorActive && Settings.isGCPremiumMember();
        findPreference(getString((R.string.preference_screen_al))).setSummary(
                getLcServiceSummary(Settings.isALConnectorActive(), gcConnectorActive));
        if (isActiveGCPM) {
            findPreference(getString(R.string.pref_connectorALActive)).setEnabled(true);
        }
    }

    private String getLcServiceSummary(final boolean lcConnectorActive, final boolean gcConnectorActive) {
        if (!lcConnectorActive) {
            return StringUtils.EMPTY;
        }

        //lc service is set to active by user. Check whether it can actually be actived due to GC conditions
        final int lcStatusTextId = gcConnectorActive && Settings.isGCPremiumMember() ?
                R.string.settings_service_active : R.string.settings_service_active_unavailable;

        return CgeoApplication.getInstance().getString(lcStatusTextId);
    }
}
