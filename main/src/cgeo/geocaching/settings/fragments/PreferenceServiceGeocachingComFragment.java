package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceServiceGeocachingComFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_geocaching_com, rootKey);

        // Authentication Preference
        // pref_fakekey_gc_authorization

        // Open website Preference
        Preference openWebsite = findPreference(getString(R.string.pref_fakekey_gc_website));
        String urlOrHost = GCConnector.getInstance().getHost();
        openWebsite.setOnPreferenceClickListener(preference -> {
            final String url = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost;
            ShareUtils.openUrl(getContext(), url);
            return true;
        });

        // Facebook Login Hint
        Preference login_facebook = findPreference(getString(R.string.pref_gc_fb_login_hint));
        login_facebook.setOnPreferenceClickListener(preference -> {
            final AlertDialog.Builder builder = Dialogs.newBuilder(getContext());
            // TODO: Open Intent to c:geo website
            builder.setMessage(R.string.settings_info_facebook_login)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.settings_info_facebook_login_title)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .setNegativeButton(R.string.more_information, (dialog, id) -> dialog.cancel());
            builder.create().show();
            return true;
        });
    }
}
