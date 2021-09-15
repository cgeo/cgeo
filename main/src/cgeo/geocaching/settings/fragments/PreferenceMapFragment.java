package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.settings.ColorpickerPreference;
import cgeo.geocaching.settings.DialogPrefFragCompat;
import cgeo.geocaching.settings.TemplateTextPreference;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Collection;

public class PreferenceMapFragment extends PreferenceFragmentCompat {
    private ListPreference pref_map_sources;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_map, rootKey);
        pref_map_sources = (ListPreference) findPreference(getString(R.string.pref_mapsource));
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ColorpickerPreference || preference instanceof TemplateTextPreference) {
            DialogFragment dialogFragment = DialogPrefFragCompat.newInstance(preference.getKey());
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
        pref_map_sources.setEntries(entries);
        pref_map_sources.setEntryValues(values);
        //pref_map_sources.setOnPreferenceChangeListener(getC);
    }

}
