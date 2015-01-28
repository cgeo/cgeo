package cgeo.geocaching.maps;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.settings.Settings;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;

public class DirectionDrawer {
    private Geopoint currentCoords;
    private final Geopoint destinationCoords;
    private final MapItemFactory mapItemFactory;

    private Paint line = null;

    public DirectionDrawer(final Geopoint coords) {
        this.destinationCoords = coords;
        this.mapItemFactory = Settings.getMapProvider().getMapItemFactory();
    }

    public void setCoordinates(final Location coordinatesIn) {
        currentCoords = new Geopoint(coordinatesIn);
    }

    void drawDirection(final Canvas canvas, final MapProjectionImpl projection) {
        if (currentCoords == null) {
            return;
        }

        if (line == null) {
            line = new Paint();
            line.setAntiAlias(true);
            line.setStrokeWidth(2f);
            line.setColor(0xFFEB391E);
        }

        final Point pos = new Point();
        final Point dest = new Point();
        projection.toPixels(mapItemFactory.getGeoPointBase(currentCoords), pos);
        projection.toPixels(mapItemFactory.getGeoPointBase(destinationCoords), dest);

        canvas.drawLine(pos.x, pos.y, dest.x, dest.y, line);
    }
}
