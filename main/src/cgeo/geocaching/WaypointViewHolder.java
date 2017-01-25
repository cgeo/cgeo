package cgeo.geocaching;

import cgeo.geocaching.ui.AbstractViewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;

public class WaypointViewHolder extends AbstractViewHolder {

    @BindView(R.id.coordinates) protected TextView coordinatesView;
    @BindView(R.id.info) protected TextView infoView;
    @BindView(R.id.name) protected TextView nameView;
    @BindView(R.id.note) protected TextView noteView;
    @BindView(R.id.user_note) protected TextView userNoteView;
    @BindView(R.id.wpDefaultNavigation) protected ImageView wpNavView;

    public WaypointViewHolder(final View rowView) {
        super(rowView);
    }

}
