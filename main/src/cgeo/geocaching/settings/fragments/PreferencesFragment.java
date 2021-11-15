package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
