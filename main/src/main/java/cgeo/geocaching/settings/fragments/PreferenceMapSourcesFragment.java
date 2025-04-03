package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.downloader.DownloadSelectorActivity;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.PreferenceUtils;
import cgeo.geocaching.utils.SettingsUtils;
import cgeo.geocaching.utils.ShareUtils;
import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;

import java.util.Collection;
import java.util.HashMap;

public class PreferenceMapSourcesFragment extends BasePreferenceFragment {
    private ListPreference prefMapSources;
    private ListPreference prefTileProvicers;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_map_sources, rootKey);
        prefMapSources = findPreference(getString(R.string.pref_mapsource));
        prefTileProvicers = findPreference(getString(R.string.pref_tileprovider));

        initMapSourcePreference();

        final MultiSelectListPreference hideTileprovidersPref = findPreference(getString(R.string.pref_tileprovider_hidden));
        // new unified map providers
        final HashMap<String, AbstractTileProvider> tileproviders = TileProviderFactory.getTileProviders();
        final String[] tpEntries = new String[tileproviders.size()];
        final String[] tpValues = new String[tileproviders.size()];
        int i = 0;
        for (AbstractTileProvider tileProvider : tileproviders.values()) {
            tpEntries[i] = tileProvider.getTileProviderName();
            tpValues[i] = tileProvider.getId();
            i++;
        }
        hideTileprovidersPref.setEntries(tpEntries);
        hideTileprovidersPref.setEntryValues(tpValues);

        setUserDefinedTileProviderUriSummary(Settings.getUserDefinedTileProviderUri());
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_userDefinedTileProviderUri)), (preference, newValue) -> {
            setUserDefinedTileProviderUriSummary(String.valueOf(newValue));
            setFlagForRestartRequired();
            return true;
        });

        final ListPreference unifiedMapVariants = findPreference(getString(R.string.pref_unifiedMapVariants));
        unifiedMapVariants.setEntries(new String[]{ "Mapsforge", "VTM", "Mapsforge + VTM" });
        unifiedMapVariants.setEntryValues(new String[]{ String.valueOf(Settings.UNIFIEDMAP_VARIANT_MAPSFORGE), String.valueOf(Settings.UNIFIEDMAP_VARIANT_VTM), String.valueOf(Settings.UNIFIEDMAP_VARIANT_BOTH) });
        setFlagForRestartRequired(R.string.pref_unifiedMapVariants);

        // UnifiedMap/legacy maps switch
        final CheckBoxPreference useLegacyMap = findPreference(getString(R.string.pref_useLegacyMap));
        useLegacyMap.setOnPreferenceChangeListener((preference, newValue) -> {
            final boolean useUnifiedMap = !((boolean) newValue);
            findPreference(getString(R.string.pref_tileprovider)).setEnabled(useUnifiedMap);
            findPreference(getString(R.string.pref_tileprovider_hidden)).setEnabled(useUnifiedMap);
            findPreference(getString(R.string.pref_unifiedMapVariants)).setEnabled(useUnifiedMap);
            findPreference(getString(R.string.pref_userDefinedTileProviderUri)).setEnabled(useUnifiedMap);
            findPreference(getString(R.string.pref_mapsource)).setEnabled(!useUnifiedMap);
            return true;
        });
        useLegacyMap.setChecked(Settings.useLegacyMaps());
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_map_sources);
        setPrefClick(this, R.string.pref_fakekey_info_offline_maps, () -> ShareUtils.openUrl(activity, activity.getString(R.string.manual_url_settings_offline_maps)));
        setPrefClick(this, R.string.pref_fakekey_start_downloader, () -> activity.startActivity(new Intent(activity, DownloadSelectorActivity.class)));
        setPrefClick(this, R.string.pref_fakekey_info_offline_mapthemes, () -> ShareUtils.openUrl(activity, activity.getString(R.string.faq_url_settings_themes)));

        initPublicFolders(this, activity.getCsah());
    }

    /**
     * Fill the choice list for map sources.
     */
    private void initMapSourcePreference() {

        // old map ---------------------------------------------------------------------------------
        final Collection<MapSource> mapSources = MapProviderFactory.getMapSources();
        final CharSequence[] entries = new CharSequence[mapSources.size()];
        final CharSequence[] values = new CharSequence[mapSources.size()];
        int idx = 0;
        for (MapSource mapSource : MapProviderFactory.getMapSources()) {
            entries[idx] = mapSource.getName();
            values[idx] = mapSource.getId();
            idx++;
        }
        prefMapSources.setEntries(entries);
        prefMapSources.setEntryValues(values);
        prefMapSources.setOnPreferenceChangeListener((preference, newValue) -> {
            final String newMapSource = (String) newValue;

            // reset the cached map source
            MapSource mapSource;
            try {
                mapSource = MapProviderFactory.getMapSource(newMapSource);
            } catch (final NumberFormatException e) {
                Log.e("PreferenceMapFragment.onMapSourcesChange: bad source id '" + newMapSource + "'", e);
                mapSource = null;
            }
            // If there is no corresponding map source (because some map sources were
            // removed from the device since) then use the first one available.
            if (mapSource == null) {
                mapSource = MapProviderFactory.getAnyMapSource();
                if (mapSource == null) {
                    // There are no map source. There is little we can do here, except log an error and
                    // return to avoid triggering a null pointer exception.
                    Log.e("PreferenceMapFragment.onMapSourcesChange: no map source available");
                    return true;
                }
            }
            Settings.setMapSource(mapSource);
            return true;
        });


        // UnifiedMap ------------------------------------------------------------------------------
        final HashMap<String, AbstractTileProvider> tileProviders = TileProviderFactory.getTileProviders();
        final CharSequence[] entriesUM = new CharSequence[tileProviders.size()];
        final CharSequence[] valuesUM = new CharSequence[tileProviders.size()];
        int idxUM = 0;
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            entriesUM[idxUM] = tileProvider.getTileProviderName();
            valuesUM[idxUM] = tileProvider.getId();
            idxUM++;
        }
        prefTileProvicers.setEntries(entriesUM);
        prefTileProvicers.setEntryValues(valuesUM);
        prefTileProvicers.setOnPreferenceChangeListener((preference, newValue) -> {
            final String newTileProvider = (String) newValue;

            // reset the cached map source
            AbstractTileProvider tileProvider;
            try {
                tileProvider = TileProviderFactory.getTileProvider(newTileProvider);
            } catch (final NumberFormatException e) {
                Log.e("PreferenceMapFragment.onMapSourcesChange: bad source id '" + newTileProvider + "'", e);
                tileProvider = null;
            }
            // If there is no corresponding map source (because some map sources were
            // removed from the device since) then use the first one available.
            if (tileProvider == null) {
                tileProvider = TileProviderFactory.getAnyTileProvider();
                if (tileProvider == null) {
                    // There are no map source. There is little we can do here, except log an error and
                    // return to avoid triggering a null pointer exception.
                    Log.e("PreferenceMapFragment.onMapSourcesChange: no map source available");
                    return true;
                }
            }
            Settings.setTileProvider(tileProvider);
            return true;
        });

    }

    private void setUserDefinedTileProviderUriSummary(final String uri) {
        SettingsUtils.setPrefSummary(this, R.string.pref_userDefinedTileProviderUri, getString(R.string.settings_userDefinedTileProviderUri) + "\n\n" + uri);
    }

}
