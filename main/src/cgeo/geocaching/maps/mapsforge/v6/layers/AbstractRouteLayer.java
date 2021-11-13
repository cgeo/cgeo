package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.utils.MapLineUtils;

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

abstract class AbstractRouteLayer extends Layer {
    protected float width;
    private Paint paint = null;
    protected int lineColor = 0xD00000A0;
    protected boolean isHidden = false;
    private final Boolean pathLock = true;

    // used for caching
    private ArrayList<ArrayList<Geopoint>> track = null;
    private Path path = null;
    private long mapSize = -1;
    private Point topLeftPoint = null;

    protected AbstractRouteLayer() {
        width = MapLineUtils.getDefaultThinLineWidth();
    }

    public void updateRoute(final Route route) {
        resetColor();
        synchronized (pathLock) {
            this.track = null;
            this.path = null;
            if (route != null) {
                this.track = route.getAllPoints();
            }
        }
    }

    public void resetColor() {
        paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStrokeWidth(width);
        paint.setStyle(Style.STROKE);
        paint.setColor(lineColor);
        paint.setTextSize(20);
    }

    public void setHidden(final boolean isHidden) {
        this.isHidden = isHidden;
    }

    @Override
    public synchronized void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (isHidden) {
            return;
        }

        // no route or route too short?
        if (this.track == null || this.track.size() < 1) {
            return;
        }

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        synchronized (pathLock) {
            if (null == this.path || this.mapSize != mapSize || this.topLeftPoint != topLeftPoint) {
                translateRouteToPath(mapSize, topLeftPoint);
            }
        }
        if (null != this.path) {
            canvas.drawPath(path, paint);
        }
    }

    private void translateRouteToPath(final long mapSize, final Point topLeftPoint) {
        this.mapSize = mapSize;
        this.topLeftPoint = topLeftPoint;
        this.path = null;

        final Iterator<ArrayList<Geopoint>> segmentIterator = track.iterator();
        if (!segmentIterator.hasNext()) {
            return;
        }

        path = AndroidGraphicFactory.INSTANCE.createPath();
        ArrayList<Geopoint> segment = segmentIterator.next();
        while (segment != null) {
            final Iterator<Geopoint> geopointIterator = segment.iterator();
            Geopoint geopoint = geopointIterator.next();
            path.moveTo((float) (MercatorProjection.longitudeToPixelX(geopoint.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(geopoint.getLatitude(), mapSize) - topLeftPoint.y));

            while (geopointIterator.hasNext()) {
                geopoint = geopointIterator.next();
                path.lineTo((float) (MercatorProjection.longitudeToPixelX(geopoint.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(geopoint.getLatitude(), mapSize) - topLeftPoint.y));
            }

            segment = segmentIterator.hasNext() ? segmentIterator.next() : null;
        }

    }

}
