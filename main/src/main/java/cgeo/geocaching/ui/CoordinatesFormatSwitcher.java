package cgeo.geocaching.ui;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;

import android.widget.TextView;

/**
 * view click listener to automatically switch different coordinate formats
 */
public class CoordinatesFormatSwitcher {

    private static final GeopointFormatter.Format[] availableFormats = {
            GeopointFormatter.Format.LAT_LON_DECMINUTE,
            GeopointFormatter.Format.LAT_LON_DECSECOND,
            GeopointFormatter.Format.LAT_LON_DECDEGREE,
            GeopointFormatter.Format.UTM
    };

    private int position = 0;
    private Geopoint coordinates;
    private TextView view;

    public CoordinatesFormatSwitcher setView(final TextView view) {
        this.view = view;
        this.view.setOnClickListener(v -> {
            position = (position + 1) % availableFormats.length;
            renderView();
        });
        renderView();
        return this;
    }

    public CoordinatesFormatSwitcher setCoordinate(final Geopoint coordinate) {
        this.coordinates = coordinate;
        renderView();
        return this;
    }

    private void renderView() {
        if (this.view != null && this.coordinates != null) {
            this.view.setText(coordinates.format(availableFormats[position]));
        }
    }

}
