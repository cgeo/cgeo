package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.utils.MapLineUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

    // used for caching
    private final HashMap<String, CachedRoute> cache = new HashMap<>();
    private long mapSize = -1;

    private static class CachedRoute {
        private boolean isHidden = false;
        private List<List<Geopoint>> track = null;
        private Path path = null;
        private Point topLeftPoint = null;
        private Paint paint = null;
        protected int lineColor = 0xD00000A0;
    }

    protected AbstractRouteLayer() { }

    public void updateRoute(final String key, final IGeoItemSupplier r, final int color, final int width) {
        if (!(r instanceof Route)) {
            return;
        }
        final Route route = (Route) r;
        synchronized (cache) {
            CachedRoute c = cache.get(key);
            if (c == null) {
                c = new CachedRoute();
                cache.put(key, c);
            }
            c.track = null;
            c.path = null;
            c.paint = resetPaint(color, MapLineUtils.getWidthFromRaw(width, false));
            c.lineColor = color;
            if (route != null) {
                c.track = getAllPoints(route);
                c.isHidden = route.isHidden();
            }
        }
    }

    private static List<List<Geopoint>> getAllPoints(final Route route) {
        final List<List<Geopoint>> result = new ArrayList<>();
        GeoGroup.forAllPrimitives(route.getItem(), p -> result.add(new ArrayList<>(p.getPoints())));
        return result;
    }

    public void removeRoute(final String key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    public Paint resetPaint(final int lineColor, final float width) {
        final Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStrokeWidth(width);
        paint.setStyle(Style.STROKE);
        paint.setColor(lineColor);
        paint.setTextSize(20);
        return paint;
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
                        canvas.drawPath(c.path, c.paint);
                    }
                }
            }
        }
    }

    private void translateRouteToPath(final long mapSize, final Point topLeftPoint, final CachedRoute c) {
        this.mapSize = mapSize;
        c.topLeftPoint = topLeftPoint;
        c.path = null;

        final Iterator<List<Geopoint>> segmentIterator = c.track.iterator();
        if (!segmentIterator.hasNext()) {
            return;
        }

        c.path = AndroidGraphicFactory.INSTANCE.createPath();
        List<Geopoint> segment = segmentIterator.next();
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
