package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.OCPreferenceKeys;
import cgeo.geocaching.utils.SettingsUtils;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceOpencachingNlFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_opencaching_nl, rootKey);

        // Open website Preference
        final Preference openWebsite = findPreference(getString(R.string.pref_fakekey_ocnl_website));
        final String urlOrHost = OCPreferenceKeys.OC_NL.authParams.host;
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.init_oc_nl);

        // Update authentication preference
        SettingsUtils.updateOpenCachingAuthPreference(this, R.string.pref_fakekey_ocnl_authorization);
    }
}
