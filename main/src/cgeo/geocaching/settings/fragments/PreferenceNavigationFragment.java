package cgeo.geocaching.settings.fragments;

import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;

import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.brouter.util.DefaultFilesUtils;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.ProcessUtils;

public class PreferenceNavigationFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_navigation, rootKey);

        initDefaultNavigationPreferences();
        initOfflineRoutingPreferences();

        // TODO: Logic for ProximityDistance needs to be reimplemented
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_navigation);
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

        final ListPreference defaultNavigationTool = findPreference(getString(R.string.pref_defaultNavigationTool));
        defaultNavigationTool.setEntries(entries);
        defaultNavigationTool.setEntryValues(values);
        final ListPreference defaultNavigationTool2 = findPreference(getString(R.string.pref_defaultNavigationTool2));
        defaultNavigationTool2.setEntries(entries);
        defaultNavigationTool2.setEntryValues(values);
    }

    private void initOfflineRoutingPreferences() {
        DefaultFilesUtils.checkDefaultFiles();
        findPreference(getString(R.string.pref_useInternalRouting)).setOnPreferenceChangeListener((preference, newValue) -> {
            updateRoutingPrefs(!Settings.useInternalRouting());
            return true;
        });
        updateRoutingPrefs(Settings.useInternalRouting());
        updateRoutingProfilesPrefs();
    }

    private void updateRoutingProfilesPrefs() {
        final ArrayList<String> profiles = new ArrayList<>();
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(PersistableFolder.ROUTING_BASE);
        for (ContentStorage.FileInformation file : files) {
            if (file.name.endsWith(BRouterConstants.BROUTER_PROFILE_FILEEXTENSION)) {
                profiles.add(file.name);
            }
        }
        final CharSequence[] entries = profiles.toArray(new CharSequence[0]);
        final CharSequence[] values = profiles.toArray(new CharSequence[0]);
        updateRoutingProfilePref(R.string.pref_brouterProfileWalk, RoutingMode.WALK, entries, values);
        updateRoutingProfilePref(R.string.pref_brouterProfileBike, RoutingMode.BIKE, entries, values);
        updateRoutingProfilePref(R.string.pref_brouterProfileCar, RoutingMode.CAR, entries, values);
    }

    private void updateRoutingProfilePref(@StringRes final int prefId, final RoutingMode mode, final CharSequence[] entries, final CharSequence[] values) {
        final String current = Settings.getRoutingProfile(mode);
        final ListPreference pref = findPreference(getString(prefId));
        pref.setEntries(entries);
        pref.setEntryValues(values);
        pref.setSummary(current);
        if (current != null) {
            for (int i = 0; i < entries.length; i++) {
                if (current.contentEquals(entries[i])) {
                    pref.setValueIndex(i);
                    break;
                }
            }
        }
    }

    private void updateRoutingPrefs(final boolean useInternalRouting) {
        final boolean anyRoutingAvailable = useInternalRouting || ProcessUtils.isInstalled(getString(R.string.package_brouter));
        findPreference(getString(R.string.pref_brouterDistanceThreshold)).setEnabled(anyRoutingAvailable);
        findPreference(getString(R.string.pref_brouterShowBothDistances)).setEnabled(anyRoutingAvailable);
        findPreference(getString(R.string.pref_brouterProfileWalk)).setEnabled(useInternalRouting);
        findPreference(getString(R.string.pref_brouterProfileBike)).setEnabled(useInternalRouting);
        findPreference(getString(R.string.pref_brouterProfileCar)).setEnabled(useInternalRouting);
    }
}
