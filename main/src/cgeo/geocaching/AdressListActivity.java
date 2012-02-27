package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.ui.AddressListAdapter;

import org.apache.commons.collections.CollectionUtils;

import android.app.ProgressDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class AdressListActivity extends AbstractListActivity {
    private String keyword = null;
    private ProgressDialog waitDialog = null;
    private AddressListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.addresses);
        setTitle(res.getString(R.string.search_address_result));

        // get parameters
        Bundle extras = getIntent().getExtras();

        // try to get data from extras
        if (extras != null) {
            keyword = extras.getString("keyword");
        }

        if (keyword == null) {
            showToast(res.getString(R.string.err_search_address_forgot));
            finish();
            return;
        }

        adapter = new AddressListAdapter(this);
        setListAdapter(adapter);

        waitDialog = ProgressDialog.show(this, res.getString(R.string.search_address_started), keyword, true);
        waitDialog.setCancelable(true);

        new AsyncTask<Void, Void, List<Address>>() {

            @Override
            protected List<Address> doInBackground(Void... params) {
                final Geocoder geocoder = new Geocoder(AdressListActivity.this, Locale.getDefault());
                try {
                    return geocoder.getFromLocationName(keyword, 20);
                } catch (Exception e) {
                    Log.e(Settings.tag, "AdressListActivity.doInBackground", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final List<Address> addresses) {
                waitDialog.dismiss();
                try {
                    if (CollectionUtils.isEmpty(addresses)) {
                        showToast(res.getString(R.string.err_search_address_no_match));
                        finish();
                        return;
                    } else {
                        for (Address address : addresses) {
                            adapter.add(address); // don't use addAll, it's only available with API >= 11
                        }
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "AdressListActivity.onPostExecute", e);
                }
            }

        }.execute();
    }
}