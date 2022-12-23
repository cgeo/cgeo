package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.SettingsUtils;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PreferenceServiceTwitterFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_twitter, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.twitter_title);

        // Update authentication preference
        SettingsUtils.setAuthTitle(this, R.string.pref_fakekey_twitter_authorization, Settings.isTwitterLoginValid());
    }
}
