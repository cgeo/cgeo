package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
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

abstract class AbstractLineLayer extends Layer {
    protected float width;
    private Paint paint = null;
    protected int lineColor = 0xD00000A0;
    protected boolean isHidden = false;
    private final Boolean pathLock = true;

    // used for caching
    private ArrayList<Geopoint> track = null;
    private Path path = null;
    private long mapSize = -1;
    private Point topLeftPoint = null;

    protected AbstractLineLayer() {
        width = MapLineUtils.getDefaultThinLineWidth();
    }

    public void updateTrack(final ArrayList<Geopoint> track) {
        paint = prepareLine();
        synchronized (pathLock) {
            this.track = null == track ? null : new ArrayList<>(track);
            this.path = null;
        }
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
        if (this.track == null || this.track.size() < 2) {
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

        final Iterator<Geopoint> iterator = track.iterator();
        if (!iterator.hasNext()) {
            return;
        }

        Geopoint point = iterator.next();
        path = AndroidGraphicFactory.INSTANCE.createPath();
        path.moveTo((float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y));

        while (iterator.hasNext()) {
            point = iterator.next();
            path.lineTo((float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y));
        }
    }

    private Paint prepareLine() {
        final Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStrokeWidth(width);
        paint.setStyle(Style.STROKE);
        paint.setColor(lineColor);
        paint.setTextSize(20);
        return paint;
    }

}
