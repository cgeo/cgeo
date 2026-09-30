package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.downloader.DownloadSelectorActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.settings.TileOverlayPreference;
import cgeo.geocaching.settings.UserDefinedTileProviderPreference;
import cgeo.geocaching.unifiedmap.overlays.TileOverlay;
import cgeo.geocaching.unifiedmap.overlays.TileOverlays;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.PrefUserDefinedTileProvider;
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
    private ListPreference prefTileProvicers;
    private PreferenceCategory userDefinedTileProvidersCategory;
    private PreferenceCategory tileOverlaysCategory;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_map_sources, rootKey);
        prefTileProvicers = findPreference(getString(R.string.pref_tileprovider));
        tileOverlaysCategory = findPreference(getString(R.string.preference_category_map_tileoverlays));

        initMapSourcePreference();
        recreateTileOverlayPreferences();

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

        userDefinedTileProvidersCategory = findPreference(getString(R.string.preference_category_userdefined_tileproviders));
        recreateUserDefinedTileProviderPreferences();

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

    private void recreateUserDefinedTileProviderPreferences() {
        userDefinedTileProvidersCategory.removeAll();
        for (PrefUserDefinedTileProvider provider : Settings.getUserDefinedTileProviders()) {
            userDefinedTileProvidersCategory.addPreference(createUserDefinedTileProviderPreference(provider));
        }
        userDefinedTileProvidersCategory.addPreference(createUserDefinedTileProviderPreferenceAddNew());
    }

    private Preference createUserDefinedTileProviderPreferenceAddNew() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle(R.string.settings_userDefinedTileProvider_addnew);
        preference.setLayoutResource(R.layout.preference_button);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(pref -> {
            createUserDefinedTileProviderPreference(new PrefUserDefinedTileProvider(randomUUID().toString(), "", "")).launchEditDialog();
            return true;
        });
        return preference;
    }

    /** (Re-)builds the rows of the user-defined tile overlays, plus the trailing "add" button */
    private void recreateTileOverlayPreferences() {
        tileOverlaysCategory.removeAll();
        for (TileOverlay overlay : TileOverlays.getAll()) {
            tileOverlaysCategory.addPreference(new TileOverlayPreference(requireContext(), overlay, this::recreateTileOverlayPreferences));
        }
        tileOverlaysCategory.addPreference(createTileOverlayPreferenceAddNew());
    }

    private Preference createTileOverlayPreferenceAddNew() {
        final Preference preference = new Preference(requireContext());
        preference.setTitle(R.string.settings_tileOverlays_add);
        preference.setLayoutResource(R.layout.preference_button);
        preference.setIconSpaceReserved(false);
        preference.setPersistent(false);
        preference.setOnPreferenceClickListener(pref -> {
            new TileOverlayPreference(requireContext(), TileOverlay.create("", ""), this::recreateTileOverlayPreferences).showEditDialog();
            return true;
        });
        return preference;
    }

    private UserDefinedTileProviderPreference createUserDefinedTileProviderPreference(final PrefUserDefinedTileProvider provider) {
        final UserDefinedTileProviderPreference preference = new UserDefinedTileProviderPreference(requireContext());
        preference.setKey(provider.getKey());
        preference.setPersistent(false);
        preference.setTitle(provider.getDisplayName());
        preference.setSummary(provider.getUri());
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            recreateUserDefinedTileProviderPreferences();
            setFlagForRestartRequired();
            return true;
        });
        return preference;
    }

}
