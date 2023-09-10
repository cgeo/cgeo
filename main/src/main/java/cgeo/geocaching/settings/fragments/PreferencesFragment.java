package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class PreferencesFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        if (prefScreen != null) {
            final Preference pref = prefScreen.findPreference(getString(R.string.preference_menu_offlinedata));
            if (pref != null) {
                pref.setEnabled(Settings.extendedSettingsAreEnabled());
            }
        }
    }
}
