package cgeo.geocaching.ui;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.utils.Log;

import android.widget.TextView;

/**
 * view click listener to automatically switch different coordinate formats
 */
public class CoordinatesFormatSwitcher {

    /**
     * Callback interface for position changes
     */
    public interface OnPositionChangedListener {
        void onPositionChanged(int position);
    }

    private static final GeopointFormatter.Format[] availableFormats = {
            GeopointFormatter.Format.LAT_LON_DECMINUTE,
            GeopointFormatter.Format.LAT_LON_DECSECOND,
            GeopointFormatter.Format.LAT_LON_DECDEGREE,
            GeopointFormatter.Format.UTM
    };

    private int position = 0;
    private Geopoint coordinates;
    private TextView view;
    private OnPositionChangedListener positionChangedListener;

    public CoordinatesFormatSwitcher setView(final TextView view) {
        this.view = view;
        this.view.setOnClickListener(v -> {
            position = (position + 1) % availableFormats.length;
            renderView();
            if (positionChangedListener != null) {
                positionChangedListener.onPositionChanged(position);
            }
        });
        renderView();
        return this;
    }

    public CoordinatesFormatSwitcher setCoordinate(final Geopoint coordinate) {
        this.coordinates = coordinate;
        renderView();
        return this;
    }

    public CoordinatesFormatSwitcher setPosition(final int position) {
        if (position < 0) {
            Log.w(String.format("CoordinatesFormatSwitcher: Invalid negative position %d, defaulting to 0", position));
            this.position = 0;
        } else {
            this.position = position % availableFormats.length;
        }
        renderView();
        return this;
    }

    public CoordinatesFormatSwitcher setOnPositionChangedListener(final OnPositionChangedListener listener) {
        this.positionChangedListener = listener;
        return this;
    }

    public int getPosition() {
        return position;
    }

    private void renderView() {
        if (this.view != null && this.coordinates != null) {
            this.view.setText(coordinates.format(availableFormats[position]));
        }
    }

}
