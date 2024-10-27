package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.PreferenceUtils;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.Objects;

public class PreferencesFragmentRoot extends BasePreferenceFragment {
    private Preference lastPreference = null;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull final Preference preference) {
        // toggle icon tint to highlight selected preference screen
        if (preference.getIcon() != null) {
            if (lastPreference != null) {
                Objects.requireNonNull(lastPreference.getIcon()).setTint(Settings.isLightSkin(requireContext()) ? Color.BLACK : Color.WHITE);
            }
            lastPreference = preference;
            preference.getIcon().setTint(getResources().getColor(R.color.colorAccent));
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        adjustScreen(Settings.extendedSettingsAreEnabled());

        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_extended_settings_enabled)), (pref, newValue) -> {
            adjustScreen((boolean) newValue);
            return true;
        });
    }

    private void adjustScreen(final boolean value) {
        final PreferenceScreen prefScreen = getPreferenceScreen();
        if (prefScreen != null) {
            final Preference pref = prefScreen.findPreference(getString(R.string.preference_menu_offlinedata));
            if (pref != null) {
                pref.setVisible(value);
            }
        }
    }
}
