package cgeo.geocaching.ui;

import butterknife.InjectView;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class AddressListAdapter extends ArrayAdapter<Address> {

    final private LayoutInflater inflater;
    final private Geopoint location;

    protected static final class ViewHolder extends AbstractViewHolder {
        @InjectView(R.id.label) protected TextView label;
        @InjectView(R.id.distance) protected TextView distance;

        public ViewHolder(final View view) {
            super(view);
        }
    }

    public AddressListAdapter(final Activity context) {
        super(context, 0);
        inflater = context.getLayoutInflater();
        location = CgeoApplication.getInstance().currentGeo().getCoords();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final Address address = getItem(position);

        View view = convertView;

        // holder pattern implementation
        final ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.addresslist_item, parent, false);
            holder = new ViewHolder(view);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                final Activity activity = (Activity) v.getContext();
                CacheListActivity.startActivityAddress(activity, new Geopoint(address.getLatitude(), address.getLongitude()), StringUtils.defaultString(address.getAddressLine(0)));
                activity.finish();
            }
        });

        holder.label.setText(getAddressText(address));
        holder.distance.setText(getDistanceText(address));

        return view;
    }

    private CharSequence getDistanceText(final Address address) {
        if (location != null && address.hasLatitude() && address.hasLongitude()) {
            return Units.getDistanceFromKilometers(location.distanceTo(new Geopoint(address.getLatitude(), address.getLongitude())));
        }

        return "";
    }

    private static CharSequence getAddressText(final Address address) {
        final int maxIndex = address.getMaxAddressLineIndex();
        final ArrayList<String> lines = new ArrayList<>();
        for (int i = 0; i <= maxIndex; i++) {
            final String line = address.getAddressLine(i);
            if (StringUtils.isNotBlank(line)) {
                lines.add(line);
            }
        }

        return StringUtils.join(lines, "\n");
    }
}