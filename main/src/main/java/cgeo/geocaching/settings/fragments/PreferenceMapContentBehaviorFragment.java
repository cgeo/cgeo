package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.os.Bundle;

public class PreferenceMapContentBehaviorFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_map_content_behavior, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_map_content_behavior);

        // Clear icon cache when modifying settings that influence icon appearance
        findPreference(getString(R.string.pref_dtMarkerOnCacheIcon)).setOnPreferenceChangeListener((preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
        findPreference(getString(R.string.pref_bigSmileysOnMap)).setOnPreferenceChangeListener((preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
    }
}
