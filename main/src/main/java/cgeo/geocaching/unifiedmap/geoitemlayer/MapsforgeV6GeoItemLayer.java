package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Pair;

import androidx.annotation.ColorInt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polygon;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.util.MapViewProjection;

public class MapsforgeV6GeoItemLayer extends Layer implements IProviderGeoItemLayer<Pair<Layer, Layer>> {

    private LayerManager layerManager;
    private MapViewProjection projection;

    public final List<Layer> layers = new ArrayList<>();
    public final Lock layerLock = new ReentrantLock();

    public MapsforgeV6GeoItemLayer(final LayerManager layerManager, final MapViewProjection projection) {
        this.layerManager = layerManager;
        this.projection = projection;
    }

    @Override
    public void init(final int zLevel) {
        Log.iForce("AsyncMapWrapper: Init Layer");
        this.layerManager.getLayers().add(this);
    }

    @Override
    public void destroy() {
        Log.iForce("Destroy Layer");
        if (this.layerManager != null) {
            this.layerManager.getLayers().remove(this);
        }
        this.layerManager = null;
    }

    @Override
    public Pair<Layer, Layer> add(final GeoPrimitive item) {

        final Paint strokePaint = createPaint(GeoStyle.getStrokeColor(item.getStyle()));
        strokePaint.setStrokeWidth(ViewUtils.dpToPixel(GeoStyle.getStrokeWidth(item.getStyle())));
        strokePaint.setStyle(Style.STROKE);
        final Paint fillPaint = createPaint(GeoStyle.getFillColor(item.getStyle()));
        fillPaint.setStyle(Style.FILL);
        final Layer goLayer;
        switch (item.getType()) {
            case MARKER:
                goLayer = null;
                break;
            case CIRCLE:
                if (item.getCenter() == null || item.getRadius() <= 0) {
                    goLayer = null;
                } else {
                    goLayer = new Circle(latLong(item.getCenter()), item.getRadius() * 1000, fillPaint, strokePaint);
                }
                break;
            case POLYLINE:
                final Polyline pl = new Polyline(strokePaint, AndroidGraphicFactory.INSTANCE);
                pl.addPoints(CollectionStream.of(item.getPoints()).map(MapsforgeV6GeoItemLayer::latLong).toList());
                goLayer = pl;
                break;
            case POLYGON:
            default:
                final Polygon po = new Polygon(fillPaint, strokePaint, AndroidGraphicFactory.INSTANCE);
                po.addPoints(CollectionStream.of(item.getPoints()).map(MapsforgeV6GeoItemLayer::latLong).toList());
                goLayer = po;
                break;
        }
        if (goLayer != null) {
            goLayer.setDisplayModel(getDisplayModel());
        }

        final Marker marker = createMarker(item.getCenter(), item.getIcon());
        if (marker != null) {
            marker.setDisplayModel(getDisplayModel());
        }

        layerLock.lock();
        try {
            if (goLayer != null) {
                layers.add(goLayer);
            }
            if (marker != null) {
                layers.add(marker);
            }
        } finally {
            layerLock.unlock();
        }
        requestRedraw();
        return new Pair<>(goLayer, marker);
    }

    @Override
    public void remove(final GeoPrimitive item, final Pair<Layer, Layer> context) {
        layerLock.lock();
        try {
            layers.remove(context.first);
            if (context.second != null) {
                layers.remove(context.second);
            }
        } finally {
            layerLock.unlock();
        }
        requestRedraw();
    }

    private static Marker createMarker(final Geopoint point, final GeoIcon icon) {
        if (point == null || icon == null || icon.getBitmap() == null) {
            return null;
        }
        final Bitmap bitmap = icon.getRotatedBitmap();
        if (bitmap == null) {
            return null;
        }

        return new Marker(
                latLong(point),
                AndroidGraphicFactory.convertToBitmap(new BitmapDrawable(CgeoApplication.getInstance().getResources(), bitmap)),
                (int) ((-icon.getXAnchor() + 0.5f) * bitmap.getWidth()),
                (int) ((-icon.getYAnchor() + 0.5f) * bitmap.getHeight()));
    }

    private static Paint createPaint(@ColorInt final int color) {
        final Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
        p.setColor(color);
        return p;
    }

    private static LatLong latLong(final Geopoint gp) {
        return new LatLong(gp.getLatitude(), gp.getLongitude());
    }


    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        layerLock.lock();
        try {
            for (Layer layer : layers) {
                layer.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
            }
        } finally {
            layerLock.unlock();
        }
    }

    @Override
    public ToScreenProjector getScreenCoordCalculator() {
        return gp -> {
            final Point pt = projection.toPixels(latLong(gp));
            return new int[]{(int) pt.x, (int) pt.y};
        };
    }

}
