package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.DisplayUtils;

import android.location.Location;

import java.util.ArrayList;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

public class HistoryLayer extends Layer {

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    private final PositionHistory positionHistory = new PositionHistory();
    private Location coordinates;
    private Paint historyLine;
    private Paint historyLineShadow;
    private final int trailColor;

    public HistoryLayer(final ArrayList<Location> locationHistory) {
        super();
        if (locationHistory != null) {
            positionHistory.setHistory(locationHistory);
        }
        trailColor = Settings.getTrailColor();
    }

    public void reset() {
        positionHistory.reset();
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (coordinates == null) {
            return;
        }

        if (historyLine == null) {
            historyLine = AndroidGraphicFactory.INSTANCE.createPaint();
            historyLine.setStrokeWidth(DisplayUtils.getHistoryLineInsetWidth());
            historyLine.setColor(0xFFFFFFFF);
        }

        if (historyLineShadow == null) {
            historyLineShadow = AndroidGraphicFactory.INSTANCE.createPaint();
            historyLineShadow.setStrokeWidth(DisplayUtils.getHistoryLineShadowWidth());
            historyLineShadow.setColor(trailColor);
        }

        positionHistory.rememberTrailPosition(coordinates);

        if (Settings.isMapTrail()) {
            // always add current position to drawn history to have a closed connection
            final ArrayList<Location> paintHistory = new ArrayList<>(positionHistory.getHistory());
            paintHistory.add(coordinates);

            final int size = paintHistory.size();
            if (size > 1) {
                final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

                Location prev = paintHistory.get(0);
                Point pointPrevious = MercatorProjection.getPixelRelative(new LatLong(prev.getLatitude(), prev.getLongitude()), mapSize, topLeftPoint);

                for (int cnt = 1; cnt < size; cnt++) {
                    final Location now = paintHistory.get(cnt);
                    final Point pointNow = MercatorProjection.getPixelRelative(new LatLong(now.getLatitude(), now.getLongitude()), mapSize, topLeftPoint);

                    // connect points by line, but only if distance between previous and current point is less than defined max
                    if (now.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                        canvas.drawLine((int) pointPrevious.x, (int) pointPrevious.y, (int) pointNow.x, (int) pointNow.y, historyLineShadow);
                        canvas.drawLine((int) pointPrevious.x, (int) pointPrevious.y, (int) pointNow.x, (int) pointNow.y, historyLine);
                    }

                    prev = now;
                    pointPrevious = pointNow;
                }
            }
        }
    }

    public ArrayList<Location> getHistory() {
        return positionHistory.getHistory();
    }

    public void setCoordinates(final Location coordinatesIn) {
        coordinates = coordinatesIn;
    }

    public Location getCoordinates() {
        return coordinates;
    }


}
