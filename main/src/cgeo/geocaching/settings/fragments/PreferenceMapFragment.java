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
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.ShareUtils;
import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import java.util.Collection;
import java.util.HashMap;

public class PreferenceMapFragment extends BasePreferenceFragment {
    private ListPreference prefMapSources;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_map, rootKey);
        prefMapSources = findPreference(getString(R.string.pref_mapsource));

        initMapSourcePreference();

        final boolean showUnifiedMap = Settings.getBoolean(R.string.pref_showUnifiedMap, false);
        final MultiSelectListPreference hideTileprovidersPref = findPreference(getString(R.string.pref_tileprovider_hidden));
        hideTileprovidersPref.setVisible(showUnifiedMap);
        if (showUnifiedMap) {
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
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_map);
        setPrefClick(this, R.string.pref_fakekey_info_offline_maps, () -> ShareUtils.openUrl(activity, activity.getString(R.string.manual_url_settings_offline_maps)));
        setPrefClick(this, R.string.pref_fakekey_start_downloader, () -> activity.startActivity(new Intent(activity, DownloadSelectorActivity.class)));
        setPrefClick(this, R.string.pref_fakekey_info_offline_mapthemes, () -> ShareUtils.openUrl(activity, activity.getString(R.string.faq_url_settings_themes)));
        setPrefClick(this, R.string.pref_fakekey_info_offline_maphillshading, () -> ShareUtils.openUrl(activity, activity.getString(R.string.manual_url_hillshading)));

        initPublicFolders(this, activity.getCsah());

        // Clear icon cache when modifying settings that influence icon appearance
        findPreference(getString(R.string.pref_dtMarkerOnCacheIcon)).setOnPreferenceChangeListener((preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });
        findPreference(getString(R.string.pref_bigSmileysOnMap)).setOnPreferenceChangeListener((preference, newValue) -> {
            MapMarkerUtils.clearCachedItems();
            return true;
        });

        // display checkbox pref for unified map, if showUnifiedMap is enabled
        final Preference p = findPreference(activity.getString(R.string.pref_useUnifiedMap));
        if (p != null) {
            p.setVisible(Settings.getBoolean(R.string.pref_showUnifiedMap, false));
        }
    }

    /**
     * Fill the choice list for map sources.
     */
    private void initMapSourcePreference() {
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
    }
}
