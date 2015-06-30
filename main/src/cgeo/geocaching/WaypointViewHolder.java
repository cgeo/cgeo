package cgeo.geocaching;

import butterknife.Bind;

import cgeo.geocaching.ui.AbstractViewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class WaypointViewHolder extends AbstractViewHolder {

    @Bind(R.id.coordinates) protected TextView coordinatesView;
    @Bind(R.id.info) protected TextView infoView;
    @Bind(R.id.name) protected TextView nameView;
    @Bind(R.id.note) protected TextView noteView;
    @Bind(R.id.wpDefaultNavigation) protected ImageView wpNavView;

    public WaypointViewHolder(final View rowView) {
        super(rowView);
    }

}
