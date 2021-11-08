package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferenceNavigationFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_navigation, rootKey);

        // TODO: Logic for ProximityDistance needs to be reimplemented
    }

    private void initNavigationMenuPreferences() {
        for (final NavigationAppFactory.NavigationAppsEnum appEnum : NavigationAppFactory.NavigationAppsEnum.values()) {
            final Preference preference = findPreference(getString(appEnum.preferenceKey));
            if (appEnum.app.isInstalled()) {
                preference.setEnabled(true);
            } else {
                preference.setSummary(R.string.settings_navigation_disabled);
            }
        }
        findPreference(getString(R.string.preference_screen_basicmembers))
            .setEnabled(!Settings.isGCPremiumMember());
        //redrawScreen(R.string.preference_screen_navigation_menu);
    }
}
