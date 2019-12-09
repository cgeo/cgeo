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
            new HelperApp(R.string.helper_sendtocgeo_title, R.string.helper_sendtocgeo_description, R.drawable.cgeo, "http://send2.cgeo.org"),
            new HelperApp(R.string.helper_contacts_title, R.string.helper_contacts_description, R.drawable.cgeo, "cgeo.contacts"),
            new HelperApp(R.string.helper_brouter_title, R.string.helper_brouter_description, R.drawable.helper_brouter, "btools.routingapp"),
            new HelperApp(R.string.helper_pocketquery_title, R.string.helper_pocketquery_description, R.drawable.helper_pocketquery, "org.pquery"),
            new HelperApp(R.string.helper_google_translate_title, R.string.helper_google_translate_description, R.drawable.helper_google_translate, "com.google.android.apps.translate"),
            new HelperApp(R.string.helper_where_you_go_title, R.string.helper_where_you_go_description, R.drawable.helper_where_you_go, "menion.android.whereyougo"),
            new HelperApp(R.string.helper_gpsstatus_title, R.string.helper_gpsstatus_description, R.drawable.helper_gpsstatus, "com.eclipsim.gpsstatus2"),
            new HelperApp(R.string.helper_bluetoothgps_title, R.string.helper_bluetoothgps_description, R.drawable.helper_bluetoothgps, "googoo.android.btgps"),
            new HelperApp(R.string.helper_gps_locker_title, R.string.helper_gps_locker_description, R.drawable.helper_gps_locker, "com.silentlexx.gpslock"),
            new HelperApp(R.string.helper_barcode_title, R.string.helper_barcode_description, R.drawable.helper_barcode, "com.google.zxing.client.android"),
            new HelperApp(R.string.helper_locus_title, R.string.helper_locus_description, R.drawable.helper_locus, "menion.android.locus"),
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.usefulapps_activity);

        ButterKnife.bind(this);

        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.apps_list, false, false);
        view.setAdapter(new HelperAppAdapter(this, HELPER_APPS, new HelperAppClickListener() {

            @Override
            public void onClickHelperApp(final HelperApp helperApp) {
                if (helperApp.packageName.startsWith("http")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(helperApp.packageName)));
                } else {
                    ProcessUtils.openMarket(UsefulAppsActivity.this, helperApp.packageName);
                }
            }
        }));

    }
}
