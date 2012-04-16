package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.cgeocaches;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.HumanDistance;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class AddressListAdapter extends ArrayAdapter<Address> {

    private LayoutInflater inflater;
    private AddressListView holder;
    private Geopoint location;

    public AddressListAdapter(final Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = ((Activity) getContext()).getLayoutInflater();
        }

        final Address address = getItem(position);

        // holder pattern implementation
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.addresses_item, null);

            holder = new AddressListView();
            holder.label = (TextView) view.findViewById(R.id.label);
            holder.distance = (TextView) view.findViewById(R.id.distance);

            view.setTag(holder);
        } else {
            holder = (AddressListView) view.getTag();
        }

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Activity activity = (Activity) v.getContext();
                cgeocaches.startActivityAddress(activity, new Geopoint(address.getLatitude(), address.getLongitude()), StringUtils.defaultString(address.getAddressLine(0)));
                activity.finish();
            }
        });

        holder.label.setText(getAddressText(address));
        holder.distance.setText(getDistanceText(address));

        return view;
    }

    private CharSequence getDistanceText(final Address address) {
        if (location == null) {
            location = cgeoapplication.getInstance().getLastCoords();
        }

        if (location != null && address.hasLatitude() && address.hasLongitude()) {
            return HumanDistance.getHumanDistance(location.distanceTo(new Geopoint(address.getLatitude(), address.getLongitude())));
        }

        return "";
    }

    private static CharSequence getAddressText(final Address address) {
        final int maxIndex = address.getMaxAddressLineIndex();
        final ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i <= maxIndex; i++) {
            String line = address.getAddressLine(i);
            if (StringUtils.isNotBlank(line)) {
                lines.add(line);
            }
        }

        return StringUtils.join(lines, "\n");
    }
}