package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.PreferenceTextAlwaysShow;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.utils.PreferenceUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    // callback data
    protected Action1<ArrayList<PrefSearchDescriptor>> searchdataCallback = null;
    protected Action2<String, String> scrolltoCallback = null;
    protected String scrolltoBaseKey = null;
    protected String scrolltoPrefKey = null;
    protected int icon = 0;

    // which preferences should be shown in default (non-extended) config mode?
    static final int[] basicPreferencesInt = new int[] {
            // services (all)
            R.string.preference_screen_services,
            // appearance
            R.string.pref_theme_setting,
            R.string.pref_selected_language, R.string.pref_units_imperial,
            R.string.pref_cacheListInfo,
            // cache details
            R.string.pref_friendlogswanted,
            R.string.pref_livelist,
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
    };
    static final String[] basicPreferences = new String[basicPreferencesInt.length]; // lazy init

    // automatic key generator
    private int nextKey = 0;

    public static class PrefSearchDescriptor {
        public String baseKey;
        public String prefKey;
        public CharSequence prefTitle;
        public CharSequence prefSummary;
        public int prefCategoryIconRes;
        public boolean isBasicSetting;

        PrefSearchDescriptor(final String baseKey, final String prefKey, final CharSequence prefTitle, final CharSequence prefSummary, @DrawableRes final int prefCategoryIconRes, final boolean isBasicSetting) {
            this.baseKey = baseKey;
            this.prefKey = prefKey;
            this.prefTitle = prefTitle;
            this.prefSummary = prefSummary;
            this.prefCategoryIconRes = prefCategoryIconRes;
            this.isBasicSetting = isBasicSetting;
        }
    }

    // sets icon resource for search info
    public BasePreferenceFragment setIcon(@DrawableRes final int icon) {
        this.icon = icon;
        return this;
    }

    // sets callback to deliver searchable info about prefs to SettingsActivity
    public void setSearchdataCallback(final Action1<ArrayList<PrefSearchDescriptor>> searchdataCallback) {
        this.searchdataCallback = searchdataCallback;
    }

    // sets a callback to scroll to a specific pref after view has been created
    public void setScrollToPrefCallback(final Action2<String, String> scrolltoCallback, final String scrolltoBaseKey, final String scrolltoPrefKey) {
        this.scrolltoCallback = scrolltoCallback;
        this.scrolltoBaseKey = scrolltoBaseKey;
        this.scrolltoPrefKey = scrolltoPrefKey;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (searchdataCallback != null) {
            final PreferenceScreen prefScreen = getPreferenceScreen();
            if (prefScreen != null) {
                final String baseKey = prefScreen.getKey();
                final ArrayList<PrefSearchDescriptor> data = new ArrayList<>();
                doSearch(baseKey, data, prefScreen);
                searchdataCallback.call(data);
                searchdataCallback = null;
            }
        }
        if (scrolltoCallback != null) {
            scrolltoCallback.call(scrolltoBaseKey, scrolltoPrefKey);
            scrolltoCallback = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkExtendedSettingsVisibility(Settings.extendedSettingsAreEnabled() || this instanceof PreferencesFragmentRoot);
    }

    /**
     * searches recursively in all elements of given prefGroup for first occurrence of s
     * returns found preference on success, null else
     * (prefers preference entries over preference groups)
     */
    private void doSearch(final String baseKey, final ArrayList<PrefSearchDescriptor> data, final PreferenceGroup start) {
        lazyInitPreferenceKeys();
        final int prefCount = start.getPreferenceCount();
        for (int i = 0; i < prefCount; i++) {
            final Preference pref = start.getPreference(i);
            // we can only address prefs that have a key, so create a generic one
            if (StringUtils.isBlank(pref.getKey())) {
                synchronized (this) {
                    pref.setKey(baseKey + "-" + (nextKey++));
                }
            }
            data.add(new PrefSearchDescriptor(baseKey, pref.getKey(), pref.getTitle(), pref.getSummary(), icon, ArrayUtils.contains(basicPreferences, pref.getKey()) || pref instanceof PreferenceTextAlwaysShow));
            if (pref instanceof PreferenceGroup) {
                doSearch(baseKey, data, (PreferenceGroup) pref);
            }
        }
    }

    public void initPreferences(final @XmlRes int preferenceResource, final String rootKey) {
        setPreferencesFromResource(preferenceResource, rootKey);
        checkExtendedSettingsVisibility(Settings.extendedSettingsAreEnabled() || this instanceof PreferencesFragmentRoot);
    }

    protected void checkExtendedSettingsVisibility(final boolean forceShowAll) {
        lazyInitPreferenceKeys();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        if (prefScreen != null) {
            checkExtendedSettingsVisibilityHelper(prefScreen, forceShowAll);
        }
    }

    private void lazyInitPreferenceKeys() {
        if (basicPreferences[0] == null) {
            for (int i = 0; i < basicPreferencesInt.length; i++) {
                basicPreferences[i] = CgeoApplication.getInstance().getString(basicPreferencesInt[i]);
            }
        }
    }

    private int checkExtendedSettingsVisibilityHelper(final PreferenceGroup start, final boolean forceShowAll) {
        // if key of start group is included, all entries below will be included as well
        final boolean showAll = forceShowAll || ArrayUtils.contains(basicPreferences, start.getKey());
        // recursively test all items below
        int visibleCount = 0;
        final int prefCount = start.getPreferenceCount();
        for (int i = 0; i < prefCount; i++) {
            final Preference pref = start.getPreference(i);
            if (showAll || ArrayUtils.contains(basicPreferences, pref.getKey()) || pref instanceof PreferenceTextAlwaysShow) {
                pref.setVisible(true);
                visibleCount++;
            } else if (!(pref instanceof PreferenceGroup)) {
                pref.setVisible(false);
            }
            if (pref instanceof PreferenceGroup) {
                visibleCount += checkExtendedSettingsVisibilityHelper((PreferenceGroup) pref, showAll);
            }
        }
        if (visibleCount == 0) {
            start.setVisible(false);
        }
        return visibleCount;
    }

    protected void setFlagForRestartRequired() {
        requireActivity().setResult(SettingsActivity.RESTART_NEEDED);
    }

    protected void setFlagForRestartRequired(final @StringRes int... prefKeyIds) {
        for (int prefKeyId : prefKeyIds) {
            PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(prefKeyId)), (preference, newValue) -> {
                setFlagForRestartRequired();
                return true;
            });
        }
    }
}
