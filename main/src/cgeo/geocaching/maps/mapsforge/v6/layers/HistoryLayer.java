package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.settings.Settings;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

import android.location.Location;

import java.util.ArrayList;

public class HistoryLayer extends Layer {

    private final PositionHistory positionHistory = new PositionHistory();
    private Location coordinates;
    private Paint historyLine;
    private Paint historyLineShadow;

    public HistoryLayer(final ArrayList<Location> locationHistory) {
        super();
        if (locationHistory != null) {
            positionHistory.setHistory(locationHistory);
        }
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (coordinates == null) {
            return;
        }

        if (historyLine == null) {
            historyLine = AndroidGraphicFactory.INSTANCE.createPaint();
            historyLine.setStrokeWidth(3.0f);
            historyLine.setColor(0xFFFFFFFF);
        }

        if (historyLineShadow == null) {
            historyLineShadow = AndroidGraphicFactory.INSTANCE.createPaint();
            historyLineShadow.setStrokeWidth(7.0f);
            historyLineShadow.setColor(0x66000000);
        }

        positionHistory.rememberTrailPosition(coordinates);

        if (Settings.isMapTrail()) {
            // always add current position to drawn history to have a closed connection
            final ArrayList<Location> paintHistory = new ArrayList<>(positionHistory.getHistory());
            paintHistory.add(coordinates);

            final int size = paintHistory.size();
            if (size > 1) {
//                int alphaCnt = size - 201;
//                if (alphaCnt < 1) {
//                    alphaCnt = 1;
//                }

                final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

                final Location prev = paintHistory.get(0);
                Point pointPrevious = MercatorProjection.getPixelRelative(new LatLong(prev.getLatitude(), prev.getLongitude()), mapSize, topLeftPoint);

                for (int cnt = 1; cnt < size; cnt++) {
                    final Location now = paintHistory.get(cnt);
                    final Point pointNow = MercatorProjection.getPixelRelative(new LatLong(now.getLatitude(), now.getLongitude()), mapSize, topLeftPoint);

//                    final int alpha;
//                    if ((alphaCnt - cnt) > 0) {
//                        alpha = 255 / (alphaCnt - cnt);
//                    }
//                    else {
//                        alpha = 255;
//                    }

//                    historyLineShadow.setAlpha(alpha);
//                    historyLine.setAlpha(alpha);

                    canvas.drawLine((int) pointPrevious.x, (int) pointPrevious.y, (int) pointNow.x, (int) pointNow.y, historyLineShadow);
                    canvas.drawLine((int) pointPrevious.x, (int) pointPrevious.y, (int) pointNow.x, (int) pointNow.y, historyLine);

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
