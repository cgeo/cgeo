package cgeo.geocaching.helper;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;

import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;

public final class UsefulAppsActivity extends AbstractActionBarActivity {

    private static final HelperApp[] HELPER_APPS = {
            new HelperApp(R.string.helper_sendtocgeo_title, R.string.helper_sendtocgeo_description, R.mipmap.ic_launcher_send2cgeo, R.string.settings_send2cgeo_url),
            new HelperApp(R.string.helper_contacts_title, R.string.helper_contacts_description, R.mipmap.ic_launcher_contacts, R.string.package_cgeo_contacts),
            new HelperApp(R.string.helper_google_translate_title, R.string.helper_google_translate_description, R.drawable.helper_google_translate, R.string.package_google_translate),
            new HelperApp(R.string.helper_where_you_go_title, R.string.helper_where_you_go_description, R.drawable.icon_whereyougo, R.string.package_whereyougo),
            new HelperApp(R.string.helper_gpsstatus_title, R.string.helper_gpsstatus_description, R.drawable.helper_gpsstatus, R.string.package_gpsstatus),
            new HelperApp(R.string.helper_bluetoothgps_title, R.string.helper_bluetoothgps_description, R.drawable.helper_bluetoothgps, R.string.package_bluetoothgps),
            new HelperApp(R.string.helper_gps_locker_title, R.string.helper_gps_locker_description, R.drawable.helper_gps_locker, R.string.package_gpslocker),
            new HelperApp(R.string.helper_chirpwolf, R.string.helper_chirpwolf_description, R.drawable.helper_chirpwolf, R.string.package_chirpwolf),
            new HelperApp(R.string.helper_locus_title, R.string.helper_locus_description, R.drawable.helper_locus, R.string.package_locus),
            new HelperApp(R.string.helper_alc, R.string.helper_alc_description, R.drawable.helper_alc, R.string.package_alc),
            new HelperApp(R.string.helper_gcwizard, R.string.helper_gcwizard_description, R.drawable.helper_gcwizard, R.string.package_gcwizward),
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.usefulapps_activity);

        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.apps_list, false, false);
        view.setAdapter(new HelperAppAdapter(this, HELPER_APPS, helperApp -> {
            final String packageName = getString(helperApp.packageNameResId);
            if (packageName.startsWith("http")) {
                ShareUtils.openUrl(this, packageName);
            } else {
                ProcessUtils.openMarket(UsefulAppsActivity.this, packageName);
            }
        }));

    }
}
