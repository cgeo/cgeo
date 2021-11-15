package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.List;

public class PreferenceNavigationNavigationFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_navigation_navigation, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_navigation_menu);
    }

    /**
     * Fill the choice list for default navigation tools.
     */
    private void initDefaultNavigationPreferences() {

        final List<NavigationAppFactory.NavigationAppsEnum> apps = NavigationAppFactory.getInstalledDefaultNavigationApps();

        final CharSequence[] entries = new CharSequence[apps.size()];
        final CharSequence[] values = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); ++i) {
            entries[i] = apps.get(i).toString();
            values[i] = String.valueOf(apps.get(i).id);
        }

        final ListPreference defaultNavigationTool = (ListPreference) findPreference(getString(R.string.pref_defaultNavigationTool));
        defaultNavigationTool.setEntries(entries);
        defaultNavigationTool.setEntryValues(values);
        final ListPreference defaultNavigationTool2 = (ListPreference) findPreference(getString(R.string.pref_defaultNavigationTool2));
        defaultNavigationTool2.setEntries(entries);
        defaultNavigationTool2.setEntryValues(values);
    }
}
