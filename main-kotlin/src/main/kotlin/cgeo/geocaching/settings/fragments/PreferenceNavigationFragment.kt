// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.settings.fragments

import cgeo.geocaching.R
import cgeo.geocaching.apps.navi.NavigationAppFactory
import cgeo.geocaching.brouter.BRouterConstants
import cgeo.geocaching.brouter.util.DefaultFilesUtils
import cgeo.geocaching.maps.routing.RoutingMode
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.utils.SettingsUtils.initPublicFolders

import android.os.Bundle

import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.Preference

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

class PreferenceNavigationFragment : BasePreferenceFragment() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_navigation, rootKey)

        initDefaultNavigationPreferences()
        initOfflineRoutingPreferences()
    }

    override     public Unit onResume() {
        super.onResume()
        val activity: SettingsActivity = (SettingsActivity) getActivity()
        assert activity != null
        activity.setTitle(R.string.settings_title_navigation)
        initPublicFolders(this, activity.getCsah())

        val tool1: Preference = findPreference(getString(R.string.pref_defaultNavigationTool))
        assert tool1 != null
        setToolSummary(tool1, Settings.getDefaultNavigationTool())
        tool1.setOnPreferenceChangeListener((preference, newValue) -> {
            setToolSummary(tool1, Integer.parseInt((String) newValue))
            return true
        })

        val tool2: Preference = findPreference(getString(R.string.pref_defaultNavigationTool2))
        assert tool2 != null
        setToolSummary(tool2, Settings.getDefaultNavigationTool2())
        tool2.setOnPreferenceChangeListener((preference, newValue) -> {
            setToolSummary(tool2, Integer.parseInt((String) newValue))
            return true
        })
    }

    private Unit setToolSummary(final Preference preference, final Int value) {
        try {
            preference.setSummary(NavigationAppFactory.getNavigationAppForId(value).getName())
        } catch (Exception ignore) {
            preference.setSummary("")
        }
    }

    /**
     * Fill the choice list for default navigation tools.
     */
    private Unit initDefaultNavigationPreferences() {

        val apps: List<NavigationAppFactory.NavigationAppsEnum> = NavigationAppFactory.getInstalledDefaultNavigationApps()

        final CharSequence[] entries = CharSequence[apps.size()]
        final CharSequence[] values = CharSequence[apps.size()]
        for (Int i = 0; i < apps.size(); ++i) {
            entries[i] = apps.get(i).toString()
            values[i] = String.valueOf(apps.get(i).id)
        }

        val defaultNavigationTool: ListPreference = findPreference(getString(R.string.pref_defaultNavigationTool))
        defaultNavigationTool.setEntries(entries)
        defaultNavigationTool.setEntryValues(values)
        val defaultNavigationTool2: ListPreference = findPreference(getString(R.string.pref_defaultNavigationTool2))
        defaultNavigationTool2.setEntries(entries)
        defaultNavigationTool2.setEntryValues(values)
    }

    private Unit initOfflineRoutingPreferences() {
        DefaultFilesUtils.checkDefaultFiles()
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_useInternalRouting)), (preference, newValue) -> {
            updateRoutingPrefs(!Settings.useInternalRouting())
            return true
        })
        updateRoutingPrefs(Settings.useInternalRouting())
        updateRoutingProfilesPrefs()
    }

    private Unit updateRoutingProfilesPrefs() {
        val profiles: ArrayList<String> = ArrayList<>()
        val files: List<ContentStorage.FileInformation> = ContentStorage.get().list(PersistableFolder.ROUTING_BASE)
        for (ContentStorage.FileInformation file : files) {
            if (file.name.endsWith(BRouterConstants.BROUTER_PROFILE_FILEEXTENSION)) {
                profiles.add(file.name)
            }
        }

        updateRoutingProfilePref(R.string.pref_brouterProfileWalk, RoutingMode.WALK, profiles, 0)
        updateRoutingProfilePref(R.string.pref_brouterProfileBike, RoutingMode.BIKE, profiles, 0)
        updateRoutingProfilePref(R.string.pref_brouterProfileCar, RoutingMode.CAR, profiles, 0)
        updateRoutingProfilePref(R.string.pref_brouterProfileUser1, RoutingMode.USER1, profiles, 1)
        updateRoutingProfilePref(R.string.pref_brouterProfileUser2, RoutingMode.USER2, profiles, 2)
    }

    private Unit updateRoutingProfilePref(@StringRes final Int prefId, final RoutingMode mode, final ArrayList<String> profiles, final Int number) {
        val current: String = StringUtils.defaultIfBlank(Settings.getRoutingProfile(mode), getString(R.string.routingmode_none))
        val pref: ListPreference = findPreference(getString(prefId))
        assert pref != null

        val prefProfiles: ArrayList<String> = ArrayList<>(profiles)
        if (number > 0) {
            prefProfiles.add(getString(R.string.routingmode_none))

            val title: String = String.format(getString(R.string.init_brouterProfileUserNumber), number)
            pref.setDialogTitle(title)
            pref.setTitle(title)
        }

        final CharSequence[] entries = prefProfiles.toArray(CharSequence[0])
        final CharSequence[] values = prefProfiles.toArray(CharSequence[0])

        pref.setEntries(entries)
        pref.setEntryValues(values)
        pref.setSummary(current)
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(newValue.toString())
            return true
        })

        for (Int i = 0; i < entries.length; i++) {
            if (current.contentEquals(entries[i])) {
                pref.setValueIndex(i)
                break
            }
        }
    }

    private Unit updateRoutingPrefs(final Boolean useInternalRouting) {
        val anyRoutingAvailable: Boolean = useInternalRouting || ProcessUtils.isInstalled(getString(R.string.package_brouter))
        PreferenceUtils.setEnabled(findPreference(getString(R.string.pref_brouterDistanceThreshold)), anyRoutingAvailable)
        PreferenceUtils.setEnabled(findPreference(getString(R.string.pref_brouterShowBothDistances)), anyRoutingAvailable)
    }
}
