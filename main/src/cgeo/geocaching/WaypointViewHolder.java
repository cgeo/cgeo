package cgeo.geocaching;

import cgeo.geocaching.databinding.WaypointItemBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;

import android.view.View;

public class WaypointViewHolder extends AbstractViewHolder {
    public WaypointItemBinding binding;
    private CoordinatesFormatSwitcher coordinateFormatSwitcher = null;

    public WaypointViewHolder(final View rowView) {
        super(rowView);
    }

    public void setCoordinate(final Geopoint coordinate) {
        if (this.coordinateFormatSwitcher == null) {
            this.coordinateFormatSwitcher = new CoordinatesFormatSwitcher().setView(this.binding.coordinates);
        }
        this.coordinateFormatSwitcher.setCoordinate(coordinate);
    }

}
