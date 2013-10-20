package cgeo.geocaching;

import butterknife.InjectView;

import cgeo.geocaching.ui.AbstractViewHolder;

import android.view.View;
import android.widget.TextView;

public class WaypointViewHolder extends AbstractViewHolder {

    @InjectView(R.id.coordinates) protected TextView coordinatesView;
    @InjectView(R.id.info) protected TextView infoView;
    @InjectView(R.id.name) protected TextView nameView;
    @InjectView(R.id.note) protected TextView noteView;
    @InjectView(R.id.wpDefaultNavigation) protected View wpNavView;

    public WaypointViewHolder(View rowView) {
        super(rowView);
    }

}
