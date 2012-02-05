package cgeo.geocaching.ui;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class DistanceView extends TextView {
    private Geopoint cacheCoords = null;

    public DistanceView(Context context) {
        super(context);
    }

    public DistanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DistanceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setContent(final Geopoint cacheCoordsIn) {
        cacheCoords = cacheCoordsIn;
    }

    public void update(final Geopoint coords) {
        if (cacheCoords == null || coords == null) {
            return;
        }
        setText(cgBase.getHumanDistance(coords.distanceTo(cacheCoords)));
    }

    public void setDistance(Float distance) {
        setText("~" + cgBase.getHumanDistance(distance));
    }

    public void clear() {
        setText(null);
    }
}