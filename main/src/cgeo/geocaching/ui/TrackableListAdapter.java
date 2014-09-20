package cgeo.geocaching.ui;

import butterknife.InjectView;

import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;

import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TrackableListAdapter extends ArrayAdapter<Trackable> {

    final private LayoutInflater inflater;

    protected static final class ViewHolder extends AbstractViewHolder {
        @InjectView(R.id.trackable_image_brand) protected ImageView imageBrand;
        @InjectView(R.id.trackable_name) protected TextView name;

        public ViewHolder(final View view) {
            super(view);
        }
    }

    public TrackableListAdapter(final Activity context) {
        super(context, 0);
        inflater = context.getLayoutInflater();
    }

    @Override
    public View getView (final int position, final View convertView, final ViewGroup parent) {
        final Trackable trackable = getItem(position);

        View view = convertView;

        // holder pattern implementation
        final ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.trackable_item, parent, false);
            holder = new ViewHolder(view);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.imageBrand.setImageResource(trackable.getIconBrand());
        holder.name.setText(Html.fromHtml(trackable.getName()).toString());

        return view;
    }
}
