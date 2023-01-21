package cgeo.geocaching.address;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.app.ProgressDialog;
import android.location.Address;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.StringUtils;

public class AddressListActivity extends AbstractActionBarActivity implements AddressClickListener {

    @NonNull
    private final ArrayList<Address> addresses = new ArrayList<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.addresslist_activity);

        final AddressListAdapter adapter = new AddressListAdapter(addresses, this);
        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.address_list, false, true);
        view.setAdapter(adapter);

        final String keyword = getIntent().getStringExtra(Intents.EXTRA_KEYWORD);
        final ProgressDialog waitDialog =
                ProgressDialog.show(this, res.getString(R.string.search_address_started), keyword, true);
        waitDialog.setCancelable(true);
        lookupAddressInBackground(keyword, adapter, waitDialog);
    }

    private void lookupAddressInBackground(final String keyword, final AddressListAdapter adapter, final ProgressDialog waitDialog) {
        final Observable<Address> geocoderObservable = new AndroidGeocoder(this).getFromLocationName(keyword)
                .onErrorResumeWith(MapQuestGeocoder.getFromLocationName(keyword));
        AndroidRxUtils.bindActivity(this, geocoderObservable.toList()).subscribe(foundAddresses -> {
            waitDialog.dismiss();
            addresses.addAll(foundAddresses);
            adapter.notifyItemRangeInserted(0, foundAddresses.size());
        }, throwable -> {
            finish();
            showToast(res.getString(R.string.err_unknown_address));
        });
    }

    @Override
    public void onClickAddress(@NonNull final Address address) {
        CacheListActivity.startActivityAddress(this, new Geopoint(address.getLatitude(), address.getLongitude()), StringUtils.defaultString(address.getAddressLine(0)));
        ActivityMixin.finishWithFadeTransition(this);
    }

    @Override
    public void onClickMapIcon(@NonNull final Address address) {
        DefaultMap.startActivityInitialCoords(this, new Geopoint(address.getLatitude(), address.getLongitude()));
        ActivityMixin.finishWithFadeTransition(this);
    }
}
