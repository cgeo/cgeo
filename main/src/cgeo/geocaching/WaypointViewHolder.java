package cgeo.geocaching;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;

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

    private CoordinatesFormatSwitcher coordinateFormatSwitcher;

    public WaypointViewHolder(final View rowView) {
        super(rowView);
        this.coordinateFormatSwitcher = new CoordinatesFormatSwitcher().setView(this.coordinatesView);
    }

    public void setCoordinate(final Geopoint coordinate) {
        this.coordinateFormatSwitcher.setCoordinate(coordinate);

    }

}
