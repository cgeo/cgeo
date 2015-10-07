package cgeo.geocaching.ui;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class DistanceView extends TextView {
    private Geopoint cacheCoords = null;

    public DistanceView(final Context context) {
        super(context);
    }

    public DistanceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public DistanceView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setContent(final Geopoint cacheCoordsIn) {
        cacheCoords = cacheCoordsIn;
    }

    public void update(@NonNull final Geopoint coords) {
        if (cacheCoords == null) {
            return;
        }
        setText(Units.getDistanceFromKilometers(coords.distanceTo(cacheCoords)));
    }

    public void setDistance(final Float distance) {
        setText("~" + Units.getDistanceFromKilometers(distance));
    }
}