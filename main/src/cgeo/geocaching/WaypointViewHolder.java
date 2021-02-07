package cgeo.geocaching;

import cgeo.geocaching.databinding.WaypointItemBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.ui.AbstractViewHolder;
import cgeo.geocaching.ui.CoordinatesFormatSwitcher;

import android.view.View;

public class WaypointViewHolder extends AbstractViewHolder {
    protected final WaypointItemBinding binding;
    private final CoordinatesFormatSwitcher coordinateFormatSwitcher;

    public WaypointViewHolder(final View rowView) {
        super(rowView);
        binding = WaypointItemBinding.bind(rowView);
        coordinateFormatSwitcher = new CoordinatesFormatSwitcher().setView(binding.coordinates);
    }

    public void setCoordinate(final Geopoint coordinate) {
        this.coordinateFormatSwitcher.setCoordinate(coordinate);
    }

}
