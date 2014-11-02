package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.location.Geocoder;
import cgeo.geocaching.ui.AddressListAdapter;

import org.apache.commons.collections4.CollectionUtils;

import android.app.ProgressDialog;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;

import java.util.List;

public class AddressListActivity extends AbstractListActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.addresslist_activity);

        // get parameters
        final String keyword = getIntent().getStringExtra(Intents.EXTRA_KEYWORD);

        if (keyword == null) {
            showToast(res.getString(R.string.err_search_address_forgot));
            finish();
            return;
        }

        final AddressListAdapter adapter = new AddressListAdapter(this);
        setListAdapter(adapter);

        final ProgressDialog waitDialog =
                ProgressDialog.show(this, res.getString(R.string.search_address_started), keyword, true);
        waitDialog.setCancelable(true);

        lookupAddressInBackground(keyword, adapter, waitDialog);
    }

    private void lookupAddressInBackground(final String keyword, final AddressListAdapter adapter, final ProgressDialog waitDialog) {
        new AsyncTask<Void, Void, List<Address>>() {

            @Override
            protected List<Address> doInBackground(final Void... params) {
                final Geocoder geocoder = new Geocoder(AddressListActivity.this);
                return geocoder.getFromLocationName(keyword);
            }

            @Override
            protected void onPostExecute(final List<Address> addresses) {
                waitDialog.dismiss();
                if (CollectionUtils.isNotEmpty(addresses)) {
                    for (final Address address : addresses) {
                        adapter.add(address); // don't use addAll, it's only available with API >= 11
                    }
                } else {
                    finish();
                    CacheListActivity.startActivityAddress(AddressListActivity.this, null, keyword);
                }
            }

        }.execute();
    }
}