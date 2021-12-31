package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.brouter.util.DefaultFilesUtils;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.ProcessUtils;
import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;

import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.List;


public class PreferenceNavigationFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_navigation, rootKey);

        initDefaultNavigationPreferences();
        initOfflineRoutingPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_navigation);
        initPublicFolders(this, activity.getCsah());

        final Preference tool1 = findPreference(getString(R.string.pref_defaultNavigationTool));
        assert tool1 != null;
        setToolSummary(tool1, Settings.getDefaultNavigationTool());
        tool1.setOnPreferenceChangeListener((preference, newValue) -> {
            setToolSummary(tool1, Integer.parseInt((String) newValue));
            return true;
        });

        final Preference tool2 = findPreference(getString(R.string.pref_defaultNavigationTool2));
        assert tool2 != null;
        setToolSummary(tool2, Settings.getDefaultNavigationTool2());
        tool2.setOnPreferenceChangeListener((preference, newValue) -> {
            setToolSummary(tool2, Integer.parseInt((String) newValue));
            return true;
        });
    }

    private void setToolSummary(final Preference preference, final int value) {
        try {
            preference.setSummary(NavigationAppFactory.getNavigationAppForId(value).getName());
        } catch (Exception ignore) {
            preference.setSummary("");
        }
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
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(newValue.toString());
            return true;
        });
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
