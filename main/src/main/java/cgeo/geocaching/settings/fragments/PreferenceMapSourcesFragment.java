package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.downloader.DownloadSelectorActivity;
import cgeo.geocaching.settings.CustomMapPreference;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.Settings.PrefCustomMap;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import java.util.HashMap;
import static java.util.UUID.randomUUID;

public class PreferenceMapSourcesFragment extends BasePreferenceFragment {
    private PreferenceCategory customMapsCategory;
    private ListPreference prefTileProvicers;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_map_sources, rootKey);
        prefTileProvicers = findPreference(getString(R.string.pref_tileprovider));
        customMapsCategory = findPreference(getString(R.string.preference_category_map_sources_custommaps));

        initMapSourcePreference();
        initHiddenMapSourcesPreference();
        recreateCustomMapPreferences();

        final ListPreference unifiedMapVariants = findPreference(getString(R.string.pref_unifiedMapVariants));
        unifiedMapVariants.setEntries(new String[]{ "Mapsforge", "VTM", "Mapsforge + VTM" });
        unifiedMapVariants.setEntryValues(new String[]{ String.valueOf(Settings.UNIFIEDMAP_VARIANT_MAPSFORGE), String.valueOf(Settings.UNIFIEDMAP_VARIANT_VTM), String.valueOf(Settings.UNIFIEDMAP_VARIANT_BOTH) });
        setFlagForRestartRequired(R.string.pref_unifiedMapVariants);
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_map_sources);
        setPrefClick(this, R.string.pref_fakekey_info_offline_maps, () -> ShareUtils.openUrl(activity, LocalizationUtils.getPlainString(R.string.manual_url_settings_offline_maps)));
        setPrefClick(this, R.string.pref_fakekey_start_downloader, () -> activity.startActivity(new Intent(activity, DownloadSelectorActivity.class)));
        setPrefClick(this, R.string.pref_fakekey_info_offline_mapthemes, () -> ShareUtils.openUrl(activity, LocalizationUtils.getPlainString(R.string.faq_url_settings_themes)));

        initPublicFolders(this, activity.getCsah());
    }

    /**
     * Fill the choice list for map sources.
     */
    private void initMapSourcePreference() {
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

    private void initHiddenMapSourcesPreference() {
        final MultiSelectListPreference hiddenMapSources = findPreference(getString(R.string.pref_tileprovider_hidden));
        final HashMap<String, AbstractTileProvider> tileProviders = TileProviderFactory.getTileProviders();
        final String[] entries = new String[tileProviders.size()];
        final String[] values = new String[tileProviders.size()];
        int index = 0;
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            entries[index] = tileProvider.getTileProviderName();
            values[index] = tileProvider.getId();
            index++;
        }
        hiddenMapSources.setEntries(entries);
        hiddenMapSources.setEntryValues(values);
    }

    private void recreateCustomMapPreferences() {
        customMapsCategory.removeAll();
        customMapsCategory.setVisible(true);
        for (PrefCustomMap customMap : Settings.getCustomMaps()) {
            customMapsCategory.addPreference(createCustomMapPreference(customMap));
        }
        final Preference addMap = new Preference(requireContext());
        addMap.setTitle(R.string.settings_custom_maps_add);
        addMap.setLayoutResource(R.layout.preference_button);
        addMap.setIconSpaceReserved(false);
        addMap.setOnPreferenceClickListener(preference -> {
            createCustomMapPreference(new PrefCustomMap(randomUUID().toString(), "", "")).launchEditDialog();
            return true;
        });
        customMapsCategory.addPreference(addMap);
    }

    private CustomMapPreference createCustomMapPreference(final PrefCustomMap customMap) {
        final CustomMapPreference preference = new CustomMapPreference(requireContext(), customMap);
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            refreshCustomMaps();
            return true;
        });
        return preference;
    }

    private void refreshCustomMaps() {
        TileProviderFactory.buildTileProviderList(true);
        Settings.resetTileProvider();
        Settings.setTileProvider(Settings.getTileProvider());
        initMapSourcePreference();
        initHiddenMapSourcesPreference();
        recreateCustomMapPreferences();
        setFlagForRestartRequired();
    }

}
