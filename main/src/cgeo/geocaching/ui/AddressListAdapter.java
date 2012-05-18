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

    final private LayoutInflater inflater;
    final private Geopoint location;

    private static final class ViewHolder {
        TextView label;
        TextView distance;
    }

    public AddressListAdapter(final Context context) {
        super(context, 0);
        inflater = ((Activity) context).getLayoutInflater();
        location = cgeoapplication.getInstance().currentGeo().getCoords();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final Address address = getItem(position);

        View view = convertView;

        // holder pattern implementation
        final ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.addresses_item, null);

            holder = new ViewHolder();
            holder.label = (TextView) view.findViewById(R.id.label);
            holder.distance = (TextView) view.findViewById(R.id.distance);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                final Activity activity = (Activity) v.getContext();
                cgeocaches.startActivityAddress(activity, new Geopoint(address.getLatitude(), address.getLongitude()), StringUtils.defaultString(address.getAddressLine(0)));
                activity.finish();
            }
        });

        holder.label.setText(getAddressText(address));
        holder.distance.setText(getDistanceText(address));

        return view;
    }

    private CharSequence getDistanceText(final Address address) {
        if (location != null && address.hasLatitude() && address.hasLongitude()) {
            return HumanDistance.getHumanDistance(location.distanceTo(new Geopoint(address.getLatitude(), address.getLongitude())));
        }

        return "";
    }

    private static CharSequence getAddressText(final Address address) {
        final int maxIndex = address.getMaxAddressLineIndex();
        final ArrayList<String> lines = new ArrayList<String>();
        for (int i = 0; i <= maxIndex; i++) {
            final String line = address.getAddressLine(i);
            if (StringUtils.isNotBlank(line)) {
                lines.add(line);
            }
        }

        return StringUtils.join(lines, "\n");
    }
}