// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.address

import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.DefaultMap
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.app.ProgressDialog
import android.location.Address
import android.os.Bundle

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList

import io.reactivex.rxjava3.core.Observable
import org.apache.commons.lang3.StringUtils

class AddressListActivity : AbstractActionBarActivity() : AddressClickListener {

    private val addresses: ArrayList<Address> = ArrayList<>()

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.addresslist_activity)

        val adapter: AddressListAdapter = AddressListAdapter(addresses, this)
        val view: RecyclerView = RecyclerViewProvider.provideRecyclerView(this, R.id.address_list, false, true)
        view.setAdapter(adapter)

        val keyword: String = getIntent().getStringExtra(Intents.EXTRA_KEYWORD)
        val waitDialog: ProgressDialog =
                ProgressDialog.show(this, res.getString(R.string.search_address_started), keyword, true)
        waitDialog.setCancelable(true)
        lookupAddressInBackground(keyword, adapter, waitDialog)
    }

    private Unit lookupAddressInBackground(final String keyword, final AddressListAdapter adapter, final ProgressDialog waitDialog) {
        val geocoderObservable: Observable<Address> = AndroidGeocoder(this).getFromLocationName(keyword)
                .onErrorResumeNext(throwable -> {
                    Log.w("AddressList: Problem retrieving address data from AndroidGeocoder", throwable)
                    return OsmNominatumGeocoder.getFromLocationName(keyword)
                })
        AndroidRxUtils.bindActivity(this, geocoderObservable.toList()).subscribe(foundAddresses -> {
            waitDialog.dismiss()
            addresses.addAll(foundAddresses)
            adapter.notifyItemRangeInserted(0, foundAddresses.size())
        }, throwable -> {
            finish()
            Log.w("AddressList: Problem retrieving address data", throwable)
            showToast(res.getString(R.string.err_unknown_address))
        })
    }

    override     public Unit onClickAddress(final Address address) {
        Settings.addToHistoryList(R.string.pref_search_history_address, StringUtils.defaultString(address.getAddressLine(0).replace(",", "")))
        CacheListActivity.startActivityAddress(this, Geopoint(address.getLatitude(), address.getLongitude()), StringUtils.defaultString(address.getAddressLine(0)))
        ActivityMixin.finishWithFadeTransition(this)
    }

    override     public Unit onClickMapIcon(final Address address) {
        Settings.addToHistoryList(R.string.pref_search_history_address, StringUtils.defaultString(address.getAddressLine(0).replace(",", "")))
        DefaultMap.startActivityInitialCoords(this, Geopoint(address.getLatitude(), address.getLongitude()))
        ActivityMixin.finishWithFadeTransition(this)
    }
}
