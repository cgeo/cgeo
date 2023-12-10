package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class PreferencesFragmentContent extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final @Nullable Bundle savedInstanceState, final @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services, rootKey);
    }
}
