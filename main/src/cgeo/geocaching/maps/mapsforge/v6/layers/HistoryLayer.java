package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import java.util.ArrayList;
import java.util.Iterator;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
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
    private final int trailColor;

    public HistoryLayer(final ArrayList<Location> locationHistory) {
        super();
        if (locationHistory != null) {
            positionHistory.setHistory(locationHistory);
        }
        trailColor = MapLineUtils.getTrailColor();
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
            historyLine.setStrokeWidth(MapLineUtils.getHistoryLineWidth());
            historyLine.setStyle(Style.STROKE);
            historyLine.setColor(trailColor);
        }

        positionHistory.rememberTrailPosition(coordinates);

        if (Settings.isMapTrail()) {
            final ArrayList<Location> paintHistory = new ArrayList<>(positionHistory.getHistory());
            final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

            final Iterator<Location> iterator = paintHistory.iterator();
            if (!iterator.hasNext()) {
                return;
            }

            Location point = iterator.next();
            final Path path = AndroidGraphicFactory.INSTANCE.createPath();
            path.moveTo((float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y));
            Location prev = point;

            while (iterator.hasNext()) {
                point = iterator.next();

                // connect points by line, but only if distance between previous and current point is less than defined max
                if (point.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                    path.lineTo((float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y));
                } else {
                    path.moveTo((float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y));
                }
                prev = point;
            }
            canvas.drawPath(path, historyLine);
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
