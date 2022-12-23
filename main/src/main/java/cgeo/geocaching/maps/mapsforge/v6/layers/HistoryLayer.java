package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import java.util.ArrayList;

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

    public HistoryLayer(final ArrayList<TrailHistoryElement> locationHistory) {
        super();
        if (locationHistory != null) {
            positionHistory.setHistory(locationHistory);
        }
        resetColor();
    }

    public void reset() {
        positionHistory.reset();
    }

    public void resetColor() {
        historyLine = AndroidGraphicFactory.INSTANCE.createPaint();
        historyLine.setStrokeWidth(MapLineUtils.getHistoryLineWidth(false));
        historyLine.setStyle(Style.STROKE);
        historyLine.setColor(MapLineUtils.getTrailColor());
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (coordinates == null) {
            return;
        }

        positionHistory.rememberTrailPosition(coordinates);

        if (Settings.isMapTrail()) {
            final ArrayList<TrailHistoryElement> paintHistory = new ArrayList<>(getHistory());
            // always add current position to drawn history to have a closed connection, even if it's not yet recorded
            paintHistory.add(new TrailHistoryElement(coordinates));
            final int size = paintHistory.size();
            if (size < 2) {
                return;
            }

            final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

            Location prev = paintHistory.get(0).getLocation();
            final Path path = AndroidGraphicFactory.INSTANCE.createPath();
            int current = 1;
            while (current < size) {
                path.moveTo((float) (MercatorProjection.longitudeToPixelX(prev.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(prev.getLatitude(), mapSize) - topLeftPoint.y));

                boolean paint = false;
                while (!paint && current < size) {
                    final Location now = paintHistory.get(current).getLocation();
                    current++;
                    if (now.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                        path.lineTo((float) (MercatorProjection.longitudeToPixelX(now.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(now.getLatitude(), mapSize) - topLeftPoint.y));
                    } else {
                        paint = true;
                    }
                    prev = now;
                }
                if (!path.isEmpty()) {
                    canvas.drawPath(path, historyLine);
                    path.clear();
                }
            }
        }
    }

    public ArrayList<TrailHistoryElement> getHistory() {
        return positionHistory.getHistory();
    }

    public void setCoordinates(final Location coordinatesIn) {
        coordinates = coordinatesIn;
    }

    public Location getCoordinates() {
        return coordinates;
    }

}
