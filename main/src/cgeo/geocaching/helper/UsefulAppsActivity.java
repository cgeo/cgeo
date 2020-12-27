package cgeo.geocaching.helper;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.ButterKnife;

public final class UsefulAppsActivity extends AbstractActionBarActivity {

    private static final HelperApp[] HELPER_APPS = {
            new HelperApp(R.string.helper_sendtocgeo_title, R.string.helper_sendtocgeo_description, R.mipmap.ic_launcher_send2cgeo, R.string.settings_send2cgeo_url),
            new HelperApp(R.string.helper_contacts_title, R.string.helper_contacts_description, R.mipmap.ic_launcher_contacts, R.string.package_cgeo_contacts),
            new HelperApp(R.string.helper_brouter_title, R.string.helper_brouter_description, R.drawable.helper_brouter, R.string.package_brouter),
            new HelperApp(R.string.helper_pocketquery_title, R.string.helper_pocketquery_description, R.drawable.helper_pocketquery, R.string.package_pquery),
            new HelperApp(R.string.helper_google_translate_title, R.string.helper_google_translate_description, R.drawable.helper_google_translate, R.string.package_google_translate),
            new HelperApp(R.string.helper_where_you_go_title, R.string.helper_where_you_go_description, R.mipmap.ic_launcher_whereyougo, R.string.package_whereyougo),
            new HelperApp(R.string.helper_gpsstatus_title, R.string.helper_gpsstatus_description, R.drawable.helper_gpsstatus, R.string.package_gpsstatus),
            new HelperApp(R.string.helper_bluetoothgps_title, R.string.helper_bluetoothgps_description, R.drawable.helper_bluetoothgps, R.string.package_bluetoothgps),
            new HelperApp(R.string.helper_gps_locker_title, R.string.helper_gps_locker_description, R.drawable.helper_gps_locker, R.string.package_gpslocker),
            new HelperApp(R.string.helper_barcode_title, R.string.helper_barcode_description, R.drawable.helper_barcode, R.string.package_barcode_scanner),
            new HelperApp(R.string.helper_chirpwolf, R.string.helper_chirpwolf_description, R.drawable.helper_chirpwolf, R.string.package_chirpwolf),
            new HelperApp(R.string.helper_locus_title, R.string.helper_locus_description, R.drawable.helper_locus, R.string.package_locus),
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.usefulapps_activity);

        ButterKnife.bind(this);

        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.apps_list, false, false);
        view.setAdapter(new HelperAppAdapter(this, HELPER_APPS, helperApp -> {
            final String packageName = getString(helperApp.packageNameResId);
            if (packageName.startsWith("http")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(packageName)));
            } else {
                ProcessUtils.openMarket(UsefulAppsActivity.this, packageName);
            }
        }));

    }
}
