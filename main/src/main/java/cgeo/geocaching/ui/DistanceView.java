package cgeo.geocaching.ui;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class DistanceView extends AppCompatTextView {

    private Geopoint cacheCoords = null;
    private Float distance = null;

    public DistanceView(final Context context) {
        super(context);
    }

    public DistanceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public DistanceView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setCacheData(final Geopoint cacheCoords, final Float distance) {
        this.cacheCoords = cacheCoords;
        this.distance = distance;
    }

    public void update(final Geopoint coords) {
        if (cacheCoords == null) {
            setText(distance == null ? "?" : "~" + Units.getDistanceFromKilometers(distance));
        } else {
            setText(coords == null ? "?" : Units.getDistanceFromKilometers(coords.distanceTo(cacheCoords)));
        }
    }
}
