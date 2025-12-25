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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.settings.PreferenceTextAlwaysShow
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.functions.Action2

import android.os.Bundle
import android.view.View

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen

import java.util.ArrayList

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    // callback data
    protected Action1<ArrayList<PrefSearchDescriptor>> searchdataCallback = null
    protected var scrolltoCallback: Action2<String, String> = null
    protected var scrolltoBaseKey: String = null
    protected var scrolltoPrefKey: String = null
    protected var icon: Int = 0

    // which preferences should be shown in default (non-extended) config mode?
    static final Int[] basicPreferencesInt = Int[] {
            // services (all)
            R.string.preference_screen_services,
            // appearance
            R.string.pref_theme_setting,
            R.string.pref_selected_language, R.string.pref_units_imperial,
            R.string.pref_cacheListInfo,
            // cache details
            R.string.pref_friendlogswanted,
            R.string.pref_livelist,
            R.string.pref_live_compass_in_navigation_action,
            R.string.pref_rot13_hint,
            // map sources
            R.string.pref_mapsource, R.string.pref_fakekey_info_offline_maps, R.string.pref_fakekey_start_downloader,
            R.string.pref_persistablefolder_offlinemaps,
            R.string.pref_fakekey_info_offline_mapthemes, R.string.pref_persistablefolder_offlinemapthemes,
            R.string.pref_tileprovider, R.string.pref_tileprovider_hidden, R.string.pref_useLegacyMap,
            // map content & behavior
            R.string.pref_maptrail,
            R.string.pref_bigSmileysOnMap, R.string.pref_dtMarkerOnCacheIcon,
            R.string.pref_excludeWpOriginal, R.string.pref_excludeWpParking, R.string.pref_excludeWpVisited,
            R.string.pref_longTapOnMap,
            R.string.pref_mapRotation,
            // logging
            R.string.pref_signature, R.string.pref_sigautoinsert, R.string.preference_category_logging_logtemplates, R.string.pref_trackautovisit, R.string.pref_log_offline,
            // offline data
            // whole section disabled in basic mode
            // routing / navigation
            R.string.pref_brouterDistanceThreshold,
            R.string.pref_defaultNavigationTool, R.string.pref_defaultNavigationTool2, R.string.pref_fakekey_navigationMenu,
            // system
            R.string.pref_persistablefolder_basedir,
            R.string.pref_extended_settings_enabled, R.string.pref_debug, R.string.pref_fakekey_generate_logcat,
            // backup / restore
            R.string.pref_backup_logins, R.string.pref_fakekey_preference_startbackup, R.string.pref_fakekey_startrestore, R.string.pref_fakekey_startrestore_dirselect
    }
    static final String[] basicPreferences = String[basicPreferencesInt.length]; // lazy init

    // automatic key generator
    private var nextKey: Int = 0

    public static class PrefSearchDescriptor {
        public String baseKey
        public String prefKey
        public CharSequence prefTitle
        public CharSequence prefSummary
        public Int prefCategoryIconRes
        public Boolean isBasicSetting

        PrefSearchDescriptor(final String baseKey, final String prefKey, final CharSequence prefTitle, final CharSequence prefSummary, @DrawableRes final Int prefCategoryIconRes, final Boolean isBasicSetting) {
            this.baseKey = baseKey
            this.prefKey = prefKey
            this.prefTitle = prefTitle
            this.prefSummary = prefSummary
            this.prefCategoryIconRes = prefCategoryIconRes
            this.isBasicSetting = isBasicSetting
        }
    }

    // sets icon resource for search info
    public BasePreferenceFragment setIcon(@DrawableRes final Int icon) {
        this.icon = icon
        return this
    }

    // sets callback to deliver searchable info about prefs to SettingsActivity
    public Unit setSearchdataCallback(final Action1<ArrayList<PrefSearchDescriptor>> searchdataCallback) {
        this.searchdataCallback = searchdataCallback
    }

    // sets a callback to scroll to a specific pref after view has been created
    public Unit setScrollToPrefCallback(final Action2<String, String> scrolltoCallback, final String scrolltoBaseKey, final String scrolltoPrefKey) {
        this.scrolltoCallback = scrolltoCallback
        this.scrolltoBaseKey = scrolltoBaseKey
        this.scrolltoPrefKey = scrolltoPrefKey
    }

    override     public Unit onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState)
        if (searchdataCallback != null) {
            val prefScreen: PreferenceScreen = getPreferenceScreen()
            if (prefScreen != null) {
                val baseKey: String = prefScreen.getKey()
                val data: ArrayList<PrefSearchDescriptor> = ArrayList<>()
                doSearch(baseKey, data, prefScreen)
                searchdataCallback.call(data)
                searchdataCallback = null
            }
        }
        if (scrolltoCallback != null) {
            scrolltoCallback.call(scrolltoBaseKey, scrolltoPrefKey)
            scrolltoCallback = null
        }
    }

    override     public Unit onResume() {
        super.onResume()
        checkExtendedSettingsVisibility(Settings.extendedSettingsAreEnabled() || this is PreferencesFragmentRoot)
    }

    /**
     * searches recursively in all elements of given prefGroup for first occurrence of s
     * returns found preference on success, null else
     * (prefers preference entries over preference groups)
     */
    private Unit doSearch(final String baseKey, final ArrayList<PrefSearchDescriptor> data, final PreferenceGroup start) {
        lazyInitPreferenceKeys()
        val prefCount: Int = start.getPreferenceCount()
        for (Int i = 0; i < prefCount; i++) {
            val pref: Preference = start.getPreference(i)
            // we can only address prefs that have a key, so create a generic one
            if (StringUtils.isBlank(pref.getKey())) {
                synchronized (this) {
                    pref.setKey(baseKey + "-" + (nextKey++))
                }
            }
            data.add(PrefSearchDescriptor(baseKey, pref.getKey(), pref.getTitle(), pref.getSummary(), icon, ArrayUtils.contains(basicPreferences, pref.getKey()) || pref is PreferenceTextAlwaysShow))
            if (pref is PreferenceGroup) {
                doSearch(baseKey, data, (PreferenceGroup) pref)
            }
        }
    }

    public Unit initPreferences(final @XmlRes Int preferenceResource, final String rootKey) {
        setPreferencesFromResource(preferenceResource, rootKey)
        checkExtendedSettingsVisibility(Settings.extendedSettingsAreEnabled() || this is PreferencesFragmentRoot)
    }

    protected Unit checkExtendedSettingsVisibility(final Boolean forceShowAll) {
        lazyInitPreferenceKeys()
        val prefScreen: PreferenceScreen = getPreferenceScreen()
        if (prefScreen != null) {
            checkExtendedSettingsVisibilityHelper(prefScreen, forceShowAll)
        }
    }

    private Unit lazyInitPreferenceKeys() {
        if (basicPreferences[0] == null) {
            for (Int i = 0; i < basicPreferencesInt.length; i++) {
                basicPreferences[i] = CgeoApplication.getInstance().getString(basicPreferencesInt[i])
            }
        }
    }

    private Int checkExtendedSettingsVisibilityHelper(final PreferenceGroup start, final Boolean forceShowAll) {
        // if key of start group is included, all entries below will be included as well
        val showAll: Boolean = forceShowAll || ArrayUtils.contains(basicPreferences, start.getKey())
        // recursively test all items below
        Int visibleCount = 0
        val prefCount: Int = start.getPreferenceCount()
        for (Int i = 0; i < prefCount; i++) {
            val pref: Preference = start.getPreference(i)
            if (showAll || ArrayUtils.contains(basicPreferences, pref.getKey()) || pref is PreferenceTextAlwaysShow) {
                pref.setVisible(true)
                visibleCount++
            } else if (!(pref is PreferenceGroup)) {
                pref.setVisible(false)
            }
            if (pref is PreferenceGroup) {
                visibleCount += checkExtendedSettingsVisibilityHelper((PreferenceGroup) pref, showAll)
            }
        }
        if (visibleCount == 0) {
            start.setVisible(false)
        }
        return visibleCount
    }

    protected Unit setFlagForRestartRequired() {
        requireActivity().setResult(SettingsActivity.RESTART_NEEDED)
    }

    protected Unit setFlagForRestartRequired(final @StringRes Int... prefKeyIds) {
        for (Int prefKeyId : prefKeyIds) {
            PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(prefKeyId)), (preference, newValue) -> {
                setFlagForRestartRequired()
                return true
            })
        }
    }
}
