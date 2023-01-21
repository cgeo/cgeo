package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.ShareUtils;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.app.Activity;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceSendToCgeoFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_send_to_cgeo, rootKey);

        // Open website Preference
        final Preference openWebsite = findPreference(getString(R.string.pref_fakekey_sendtocgeo_website));
        final String urlOrHost = "send2.cgeo.org";
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        activity.setTitle(R.string.init_sendToCgeo);
        setPrefClick(this, R.string.pref_fakekey_sendtocgeo_info, () -> ShareUtils.openUrl(activity, activity.getString(R.string.settings_send2cgeo_url)));
    }
}
