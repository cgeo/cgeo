package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.utils.MapLineUtils;

import java.util.ArrayList;
import java.util.HashMap;
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

    // used for caching
    private final HashMap<String, CachedRoute> cache = new HashMap<>();
    private long mapSize = -1;

    private static class CachedRoute {
        private boolean isHidden = false;
        private ArrayList<ArrayList<Geopoint>> track = null;
        private Path path = null;
        private Point topLeftPoint = null;
    }

    protected AbstractRouteLayer() {
        width = MapLineUtils.getDefaultThinLineWidth();
    }

    public void updateRoute(final String key, final Route route) {
        resetColor();
        synchronized (cache) {
            CachedRoute c = cache.get(key);
            if (c == null) {
                c = new CachedRoute();
                cache.put(key, c);
            }
            c.track = null;
            c.path = null;
            if (route != null) {
                c.track = route.getAllPoints();
                c.isHidden = route.isHidden();
            }
        }
    }

    public void removeRoute(final String key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    public void resetColor() {
        paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStrokeWidth(width);
        paint.setStyle(Style.STROKE);
        paint.setColor(lineColor);
        paint.setTextSize(20);
    }

    public void setHidden(final String key, final boolean isHidden) {
        synchronized (cache) {
            final CachedRoute c = cache.get(key);
            if (c != null) {
                c.isHidden = isHidden;
            }
        }
    }

    @Override
    public synchronized void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        synchronized (cache) {
            for (CachedRoute c : cache.values()) {
                // route hidden, no route or route too short?
                if (!c.isHidden && c.track != null && c.track.size() > 0) {
                    final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
                    if (null == c.path || this.mapSize != mapSize || c.topLeftPoint != topLeftPoint) {
                        translateRouteToPath(mapSize, topLeftPoint, c);
                    }
                    if (null != c.path) {
                        canvas.drawPath(c.path, paint);
                    }
                }
            }
        }
    }

    private void translateRouteToPath(final long mapSize, final Point topLeftPoint, final CachedRoute c) {
        this.mapSize = mapSize;
        c.topLeftPoint = topLeftPoint;
        c.path = null;

        final Iterator<ArrayList<Geopoint>> segmentIterator = c.track.iterator();
        if (!segmentIterator.hasNext()) {
            return;
        }

        c.path = AndroidGraphicFactory.INSTANCE.createPath();
        ArrayList<Geopoint> segment = segmentIterator.next();
        while (segment != null) {
            final Iterator<Geopoint> geopointIterator = segment.iterator();
            Geopoint geopoint = geopointIterator.next();
            c.path.moveTo((float) (MercatorProjection.longitudeToPixelX(geopoint.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(geopoint.getLatitude(), mapSize) - topLeftPoint.y));

            while (geopointIterator.hasNext()) {
                geopoint = geopointIterator.next();
                c.path.lineTo((float) (MercatorProjection.longitudeToPixelX(geopoint.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(geopoint.getLatitude(), mapSize) - topLeftPoint.y));
            }

            segment = segmentIterator.hasNext() ? segmentIterator.next() : null;
        }
    }

}
