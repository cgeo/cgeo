package cgeo.geocaching.settings.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import cgeo.geocaching.R;

public class PreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
