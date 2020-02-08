package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.core.util.Pair;

import java.util.ArrayList;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

public class RouteLayer extends Layer {

    private final float width;
    private Paint line = null;
    private PostRealDistance postRealRouteDistance = null;

    // used for caching
    private ArrayList<Geopoint> route = null;
    private float distance = 0.0f;
    private ArrayList<Pair<Integer, Integer>> pixelRoute = null;
    private long mapSize = 0;


    public void draw(final Canvas canvas, final Paint line, final Point topLeftPoint) {

    }

    public interface PostRealDistance {
        void postRealDistance (float realDistance);
    }

    public RouteLayer(final PostRealDistance postRealRouteDistance) {
        this.postRealRouteDistance = postRealRouteDistance;

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        width = 8f * metrics.density;
    }

    public void updateRoute(final ArrayList<Geopoint> route, final float distance) {
        this.route = new ArrayList<Geopoint>(route);
        this.distance = distance;
        this.pixelRoute = null;
        this.mapSize = 0;

        if (postRealRouteDistance != null) {
            postRealRouteDistance.postRealDistance(distance);
        }
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (this.route == null || this.route.size() < 2) {
            return; // no route or route too short
        }

        if (this.pixelRoute == null && this.mapSize > 0) {
            return; // building cache
        }

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        if (this.pixelRoute == null || this.mapSize != mapSize) {
            this.mapSize = mapSize;
            this.pixelRoute = new ArrayList<Pair<Integer, Integer>>();
            for (int i = 0; i < route.size(); i++) {
                pixelRoute.add(translateToPixels(mapSize, this.route.get(i)));
            }
        }

        if (line == null) {
            line = AndroidGraphicFactory.INSTANCE.createPaint();
            line.setStrokeWidth(width);
            line.setStyle(Style.STROKE);
            line.setColor(0xD00000A0);
            line.setTextSize(20);
        }
        for (int i = 1; i < pixelRoute.size(); i++) {
            final Pair<Integer, Integer> source = pixelRoute.get(i - 1);
            final Pair<Integer, Integer> destination = pixelRoute.get(i);
            canvas.drawLine(source.first - (int) topLeftPoint.x, source.second - (int) topLeftPoint.y, destination.first - (int) topLeftPoint.x, destination.second - (int) topLeftPoint.y, line);
        }

        if (postRealRouteDistance != null) {
            postRealRouteDistance.postRealDistance(distance);
        }

    }

    private static Pair<Integer, Integer> translateToPixels(final long mapSize, final Geopoint coords) {
        final int posX = (int) (MercatorProjection.longitudeToPixelX(coords.getLongitude(), mapSize));
        final int posY = (int) (MercatorProjection.latitudeToPixelY(coords.getLatitude(), mapSize));
        return new Pair<>(posX, posY);
    }
}
