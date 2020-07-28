package cgeo.geocaching.address;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import org.apache.commons.lang3.StringUtils;

class AddressListAdapter extends RecyclerView.Adapter<AddressListAdapter.AddressListHolder> {

    @NonNull private final Geopoint location;
    @NonNull private final List<Address> addresses;
    @NonNull private final AddressClickListener clickListener;

    protected static final class AddressListHolder extends AbstractRecyclerViewHolder {

        @BindView(R.id.label) TextView label;
        @BindView(R.id.distance) TextView distance;
        @BindView(R.id.mapIcon) ImageView mapIcon;

        AddressListHolder(final View itemView) {
            super(itemView);
        }
    }

    AddressListAdapter(@NonNull final List<Address> addresses, @NonNull final AddressClickListener addressClickListener) {
        this.addresses = addresses;
        this.clickListener = addressClickListener;
        location = Sensors.getInstance().currentGeo().getCoords();
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    @Override
    @NonNull
    public AddressListHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.addresslist_item, parent, false);
        final AddressListHolder viewHolder = new AddressListHolder(view);
        viewHolder.itemView.setOnClickListener(view1 -> clickListener.onClickAddress(addresses.get(viewHolder.getAdapterPosition())));
        viewHolder.mapIcon.setOnClickListener(v -> clickListener.onClickMapIcon(addresses.get(viewHolder.getAdapterPosition())));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final AddressListHolder holder, final int position) {
        final Address address = addresses.get(position);
        holder.label.setText(getAddressText(address));
        holder.distance.setText(getDistanceText(address));
    }

    private CharSequence getDistanceText(final Address address) {
        if (address.hasLatitude() && address.hasLongitude()) {
            return Units.getDistanceFromKilometers(location.distanceTo(new Geopoint(address.getLatitude(), address.getLongitude())));
        }

        return "";
    }

    private static CharSequence getAddressText(final Address address) {
        final int maxIndex = address.getMaxAddressLineIndex();
        final List<String> lines = new ArrayList<>();
        for (int i = 0; i <= maxIndex; i++) {
            final String line = address.getAddressLine(i);
            if (StringUtils.isNotBlank(line)) {
                lines.add(line);
            }
        }

        return StringUtils.join(lines, "\n");
    }
}
