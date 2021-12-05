package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.downloader.DownloadSelectorActivity;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.settings.DialogPrefFragCompat;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.settings.TemplateTextPreference;
import cgeo.geocaching.utils.ShareUtils;

import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Collection;

public class PreferenceMapFragment extends PreferenceFragmentCompat {
    private ListPreference prefMapSources;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_map, rootKey);
        prefMapSources = findPreference(getString(R.string.pref_mapsource));

        initMapSourcePreference();
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

        initPublicFolders(this, activity.getCsah());
    }

    @Override
    public void onDisplayPreferenceDialog(final Preference preference) {
        if (preference instanceof TemplateTextPreference) {
            final DialogFragment dialogFragment = DialogPrefFragCompat.newInstance(preference.getKey());
            // FIXME: Don't use setTargetFragment
            // Instead of using a target fragment to pass results, the fragment requesting a result should use
            // FragmentManager.setFragmentResultListener(String, LifecycleOwner, FragmentResultListener) to register a
            // FragmentResultListener with a requestKey using its parent fragment manager. The fragment delivering a
            // result should then call FragmentManager.setFragmentResult(String, Bundle) using the same requestKey.
            // Consider using setArguments to pass the requestKey if you need to support dynamic request keys.
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), null);
        } else {
            super.onDisplayPreferenceDialog(preference);
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
        //pref_map_sources.setOnPreferenceChangeListener(getC);
    }

}
