package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PreferenceSystemFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_system, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_system);
    }
}
