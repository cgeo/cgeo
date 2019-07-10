package cgeo.geocaching.maps.mapsforge.v6.caches;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.overlay.Circle;

public class GeoitemCircle extends Circle {
    public GeoitemCircle(final LatLong latLong, final float radius, final Paint paintFill, final Paint paintStroke) {
        super(latLong, radius, paintFill, paintStroke);
    }
}
