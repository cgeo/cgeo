package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PreferenceServicesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_services);
    }
}
