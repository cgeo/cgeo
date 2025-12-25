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

import cgeo.geocaching.R
import cgeo.geocaching.databinding.AddresslistItemBinding
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder

import android.location.Address
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

class AddressListAdapter : RecyclerView().Adapter<AddressListAdapter.AddressListHolder> {

    private final Geopoint location
    private final List<Address> addresses
    private final AddressClickListener clickListener

    protected static class AddressListHolder : AbstractRecyclerViewHolder() {
        private final AddresslistItemBinding binding

        AddressListHolder(final View itemView) {
            super(itemView)
            binding = AddresslistItemBinding.bind(itemView)
        }
    }

    AddressListAdapter(final List<Address> addresses, final AddressClickListener addressClickListener) {
        this.addresses = addresses
        this.clickListener = addressClickListener
        location = LocationDataProvider.getInstance().currentGeo().getCoords()
    }

    override     public Int getItemCount() {
        return addresses.size()
    }

    override     public AddressListHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
        val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.addresslist_item, parent, false)
        val viewHolder: AddressListHolder = AddressListHolder(view)
        viewHolder.itemView.setOnClickListener(view1 -> clickListener.onClickAddress(addresses.get(viewHolder.getAdapterPosition())))
        viewHolder.binding.mapIcon.setOnClickListener(v -> clickListener.onClickMapIcon(addresses.get(viewHolder.getAdapterPosition())))
        return viewHolder
    }

    override     public Unit onBindViewHolder(final AddressListHolder holder, final Int position) {
        val address: Address = addresses.get(position)
        holder.binding.label.setText(getAddressText(address))
        holder.binding.distance.setText(getDistanceText(address))
    }

    private CharSequence getDistanceText(final Address address) {
        if (address.hasLatitude() && address.hasLongitude()) {
            return Units.getDistanceFromKilometers(location.distanceTo(Geopoint(address.getLatitude(), address.getLongitude())))
        }

        return ""
    }

    private static CharSequence getAddressText(final Address address) {
        val maxIndex: Int = address.getMaxAddressLineIndex()
        val lines: List<String> = ArrayList<>()
        for (Int i = 0; i <= maxIndex; i++) {
            val line: String = address.getAddressLine(i)
            if (StringUtils.isNotBlank(line)) {
                lines.add(line)
            }
        }

        return StringUtils.join(lines, "\n")
    }
}
