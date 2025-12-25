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
import cgeo.geocaching.downloader.DownloadSelectorActivity
import cgeo.geocaching.maps.MapProviderFactory
import cgeo.geocaching.maps.interfaces.MapSource
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.SettingsUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.SettingsUtils.initPublicFolders
import cgeo.geocaching.utils.SettingsUtils.setPrefClick

import android.content.Intent
import android.os.Bundle

import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference

import java.util.Collection
import java.util.HashMap

import org.apache.commons.lang3.StringUtils

class PreferenceMapSourcesFragment : BasePreferenceFragment() {
    private ListPreference prefMapSources
    private ListPreference prefTileProvicers

    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_map_sources, rootKey)
        prefMapSources = findPreference(getString(R.string.pref_mapsource))
        prefTileProvicers = findPreference(getString(R.string.pref_tileprovider))

        initMapSourcePreference()

        val hideTileprovidersPref: MultiSelectListPreference = findPreference(getString(R.string.pref_tileprovider_hidden))
        // unified map providers
        val tileproviders: HashMap<String, AbstractTileProvider> = TileProviderFactory.getTileProviders()
        final String[] tpEntries = String[tileproviders.size()]
        final String[] tpValues = String[tileproviders.size()]
        Int i = 0
        for (AbstractTileProvider tileProvider : tileproviders.values()) {
            tpEntries[i] = tileProvider.getTileProviderName()
            tpValues[i] = tileProvider.getId()
            i++
        }
        hideTileprovidersPref.setEntries(tpEntries)
        hideTileprovidersPref.setEntryValues(tpValues)

        setUserDefinedTileProviderUriSummary(Settings.getUserDefinedTileProviderUri())
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_userDefinedTileProviderUri)), (preference, newValue) -> {
            setUserDefinedTileProviderUriSummary(String.valueOf(newValue))
            setFlagForRestartRequired()
            return true
        })

        val useLegacyMap: CheckBoxPreference = findPreference(getString(R.string.pref_useLegacyMap))

        val unifiedMapVariants: ListPreference = findPreference(getString(R.string.pref_unifiedMapVariants))
        unifiedMapVariants.setEntries(String[]{ "Mapsforge", "VTM", "Mapsforge + VTM" })
        unifiedMapVariants.setEntryValues(String[]{ String.valueOf(Settings.UNIFIEDMAP_VARIANT_MAPSFORGE), String.valueOf(Settings.UNIFIEDMAP_VARIANT_VTM), String.valueOf(Settings.UNIFIEDMAP_VARIANT_BOTH) })
        setFlagForRestartRequired(R.string.pref_unifiedMapVariants)
        unifiedMapVariants.setOnPreferenceChangeListener((preference, newValue) -> {
            updateBackgroundTransparent((String) newValue, !useLegacyMap.isChecked())
            return true
        })
        updateBackgroundTransparent(unifiedMapVariants.getValue(), !useLegacyMap.isChecked())

        // UnifiedMap/legacy maps switch
        useLegacyMap.setOnPreferenceChangeListener((preference, newValue) -> {
            val useUnifiedMap: Boolean = !((Boolean) newValue)
            findPreference(getString(R.string.pref_tileprovider)).setEnabled(useUnifiedMap)
            findPreference(getString(R.string.pref_tileprovider_hidden)).setEnabled(useUnifiedMap)
            findPreference(getString(R.string.pref_unifiedMapVariants)).setEnabled(useUnifiedMap)
            updateBackgroundTransparent(unifiedMapVariants.getValue(), useUnifiedMap)
            findPreference(getString(R.string.pref_userDefinedTileProviderUri)).setEnabled(useUnifiedMap)
            findPreference(getString(R.string.pref_mapsource)).setEnabled(!useUnifiedMap)
            return true
        })
        useLegacyMap.setChecked(Settings.useLegacyMaps())
    }

    override     public Unit onResume() {
        super.onResume()
        val activity: SettingsActivity = (SettingsActivity) getActivity()
        assert activity != null
        activity.setTitle(R.string.settings_title_map_sources)
        setPrefClick(this, R.string.pref_fakekey_info_offline_maps, () -> ShareUtils.openUrl(activity, activity.getString(R.string.manual_url_settings_offline_maps)))
        setPrefClick(this, R.string.pref_fakekey_start_downloader, () -> activity.startActivity(Intent(activity, DownloadSelectorActivity.class)))
        setPrefClick(this, R.string.pref_fakekey_info_offline_mapthemes, () -> ShareUtils.openUrl(activity, activity.getString(R.string.faq_url_settings_themes)))

        initPublicFolders(this, activity.getCsah())
    }

    /**
     * Fill the choice list for map sources.
     */
    private Unit initMapSourcePreference() {

        // old map ---------------------------------------------------------------------------------
        val mapSources: Collection<MapSource> = MapProviderFactory.getMapSources()
        final CharSequence[] entries = CharSequence[mapSources.size()]
        final CharSequence[] values = CharSequence[mapSources.size()]
        Int idx = 0
        for (MapSource mapSource : MapProviderFactory.getMapSources()) {
            entries[idx] = mapSource.getName()
            values[idx] = mapSource.getId()
            idx++
        }
        prefMapSources.setEntries(entries)
        prefMapSources.setEntryValues(values)
        prefMapSources.setOnPreferenceChangeListener((preference, newValue) -> {
            val newMapSource: String = (String) newValue

            // reset the cached map source
            MapSource mapSource
            try {
                mapSource = MapProviderFactory.getMapSource(newMapSource)
            } catch (final NumberFormatException e) {
                Log.e("PreferenceMapFragment.onMapSourcesChange: bad source id '" + newMapSource + "'", e)
                mapSource = null
            }
            // If there is no corresponding map source (because some map sources were
            // removed from the device since) then use the first one available.
            if (mapSource == null) {
                mapSource = MapProviderFactory.getAnyMapSource()
                if (mapSource == null) {
                    // There are no map source. There is little we can do here, except log an error and
                    // return to avoid triggering a null pointer exception.
                    Log.e("PreferenceMapFragment.onMapSourcesChange: no map source available")
                    return true
                }
            }
            Settings.setMapSource(mapSource)
            return true
        })


        // UnifiedMap ------------------------------------------------------------------------------
        val tileProviders: HashMap<String, AbstractTileProvider> = TileProviderFactory.getTileProviders()
        final CharSequence[] entriesUM = CharSequence[tileProviders.size()]
        final CharSequence[] valuesUM = CharSequence[tileProviders.size()]
        Int idxUM = 0
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            entriesUM[idxUM] = tileProvider.getTileProviderName()
            valuesUM[idxUM] = tileProvider.getId()
            idxUM++
        }
        prefTileProvicers.setEntries(entriesUM)
        prefTileProvicers.setEntryValues(valuesUM)
        prefTileProvicers.setOnPreferenceChangeListener((preference, newValue) -> {
            val newTileProvider: String = (String) newValue

            // reset the cached map source
            AbstractTileProvider tileProvider
            try {
                tileProvider = TileProviderFactory.getTileProvider(newTileProvider)
            } catch (final NumberFormatException e) {
                Log.e("PreferenceMapFragment.onMapSourcesChange: bad source id '" + newTileProvider + "'", e)
                tileProvider = null
            }
            // If there is no corresponding map source (because some map sources were
            // removed from the device since) then use the first one available.
            if (tileProvider == null) {
                tileProvider = TileProviderFactory.getAnyTileProvider()
                if (tileProvider == null) {
                    // There are no map source. There is little we can do here, except log an error and
                    // return to avoid triggering a null pointer exception.
                    Log.e("PreferenceMapFragment.onMapSourcesChange: no map source available")
                    return true
                }
            }
            Settings.setTileProvider(tileProvider)
            return true
        })

    }

    private Unit updateBackgroundTransparent(final String unifiedMapVariant, final Boolean useUnifiedMap) {
        findPreference(getString(R.string.pref_vtmBackgroundTransparent)).setEnabled(useUnifiedMap && !StringUtils == (unifiedMapVariant, "1")); // "1" is "Mapsforge only"
    }

    private Unit setUserDefinedTileProviderUriSummary(final String uri) {
        SettingsUtils.setPrefSummary(this, R.string.pref_userDefinedTileProviderUri, getString(R.string.settings_userDefinedTileProviderUri) + "\n\n" + uri)
    }

}
