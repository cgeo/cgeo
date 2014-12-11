package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractListActivity;
import cgeo.geocaching.location.AndroidGeocoder;
import cgeo.geocaching.location.GCGeocoder;
import cgeo.geocaching.ui.AddressListAdapter;

import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;

import android.app.ProgressDialog;
import android.location.Address;
import android.os.Bundle;

import java.util.List;

public class AddressListActivity extends AbstractListActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.addresslist_activity);


        final AddressListAdapter adapter = new AddressListAdapter(this);
        setListAdapter(adapter);

        final String keyword = getIntent().getStringExtra(Intents.EXTRA_KEYWORD);
        final ProgressDialog waitDialog =
                ProgressDialog.show(this, res.getString(R.string.search_address_started), keyword, true);
        waitDialog.setCancelable(true);
        lookupAddressInBackground(keyword, adapter, waitDialog);
    }

    private void lookupAddressInBackground(final String keyword, final AddressListAdapter adapter, final ProgressDialog waitDialog) {
        final Observable<Address> geocoderObservable = new AndroidGeocoder(this).getFromLocationName(keyword).onErrorResumeNext(GCGeocoder.getFromLocationName(keyword));
        AndroidObservable.bindActivity(this, geocoderObservable.toList()).subscribe(new Action1<List<Address>>() {
            @Override
            public void call(final List<Address> addresses) {
                waitDialog.dismiss();
                for (final Address address : addresses) {
                    adapter.add(address); // don't use addAll, it's only available with API >= 11
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(final Throwable throwable) {
                finish();
                showToast(res.getString(R.string.err_unknown_address));
            }
        });
    }
}