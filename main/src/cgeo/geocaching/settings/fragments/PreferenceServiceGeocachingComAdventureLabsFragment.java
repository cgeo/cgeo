package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceGeocachingComAdventureLabsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(
            R.xml.preferences_services_geocaching_com_adventure_lab,
            rootKey);

        initLCServicePreference(Settings.isGCConnectorActive());
    }

    private void initLCServicePreference(final boolean gcConnectorActive) {
        final boolean isActiveGCPM = gcConnectorActive && Settings.isGCPremiumMember();
        findPreference(getString((R.string.preference_screen_lc))).setSummary(
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
