package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class cgDistanceView extends TextView {
    private cgBase base = null;
    private Geopoint cacheCoords = null;

    public cgDistanceView(Context context) {
        super(context);
    }

    public cgDistanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public cgDistanceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setContent(cgBase baseIn, final Geopoint cacheCoordsIn) {
        base = baseIn;
        cacheCoords = cacheCoordsIn;
    }

    public void update(final Geopoint coords) {
        if (cacheCoords == null || coords == null || base == null) {
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