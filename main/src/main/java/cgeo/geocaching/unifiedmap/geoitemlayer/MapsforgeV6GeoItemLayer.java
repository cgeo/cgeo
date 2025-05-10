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
import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.List;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polygon;
import org.mapsforge.map.layer.overlay.Polyline;

public class MapsforgeV6GeoItemLayer implements IProviderGeoItemLayer<int[]> {

    private MapView mapView;
    private LayerManager layerManager;
    private int defaultZLevel;

    private MapsforgeV6ZLevelGroupLayer groupLayer;

    public MapsforgeV6GeoItemLayer(final MapView mapView) {
        this.mapView = mapView;
        this.layerManager = mapView.getLayerManager();
    }

    @Override
    public void init(final int defaultZLevel) {
        Log.iForce("AsyncMapWrapper: Init Layer");
        this.defaultZLevel = defaultZLevel;

        //find or create zLeve-aware group layer
        synchronized (this.layerManager.getLayers()) {
            for (Layer layer : this.layerManager.getLayers()) {
                if (layer instanceof MapsforgeV6ZLevelGroupLayer) {
                    groupLayer = (MapsforgeV6ZLevelGroupLayer) layer;
                    break;
                }
            }
            if (groupLayer == null) {
                groupLayer = new MapsforgeV6ZLevelGroupLayer();
                this.layerManager.getLayers().add(groupLayer);
            }
        }
    }

    @Override
    public void destroy(final Collection<Pair<GeoPrimitive, int[]>> values) {
        Log.iForce("Destroy Layer");
        this.layerManager = null;
        if (groupLayer != null) {
            for (Pair<GeoPrimitive, int[]> entry : values) {
                groupLayer.remove(false, entry.second);
            }
            groupLayer.requestRedraw();
        }
        this.mapView = null;
    }

    @Override
    public int[] add(final GeoPrimitive item) {

        final Paint strokePaint = createPaint(GeoStyle.getStrokeColor(item.getStyle()));
        strokePaint.setStrokeWidth(ViewUtils.dpToPixelFloat(GeoStyle.getStrokeWidth(item.getStyle())));
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
                if (item.getHoles() != null) {
                    for (List<Geopoint> hole : item.getHoles()) {
                        po.addHole(CollectionStream.of(hole).map(MapsforgeV6GeoItemLayer::latLong).toList());
                    }
                }
                goLayer = po;
                break;
        }
        if (goLayer != null) {
            goLayer.setDisplayModel(groupLayer.getDisplayModel());
        }

        final Marker marker = createMarker(item.getCenter(), item.getIcon());
        if (marker != null) {
            marker.setDisplayModel(groupLayer.getDisplayModel());
        }

        final int zlevel = item.getZLevel() <= 0 ? defaultZLevel : item.getZLevel();

        return groupLayer.add(zlevel, false, goLayer, marker);
    }

    @Override
    public void remove(final GeoPrimitive item, final int[] context) {
        groupLayer.remove(false, context);
    }

    @Override
    public String onMapChangeBatchEnd(final long processedCount) {
        if (layerManager == null || processedCount == 0) {
            return null;
        }
        groupLayer.requestRedraw();
        return null;
    }

    private static Marker createMarker(final Geopoint point, final GeoIcon icon) {
        if (point == null || icon == null || icon.getBitmap() == null) {
            return null;
        }
        final Bitmap bitmap = icon.getRotatedBitmap();
        if (bitmap == null) {
            return null;
        }

        final Marker newMarker = new Marker(
                latLong(point),
                AndroidGraphicFactory.convertToBitmap(new BitmapDrawable(CgeoApplication.getInstance().getResources(), bitmap)),
                (int) ((-icon.getXAnchor() + 0.5f) * bitmap.getWidth()),
                (int) ((-icon.getYAnchor() + 0.5f) * bitmap.getHeight()));
        newMarker.setBillboard(!icon.isFlat());
        return newMarker;
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
    public ToScreenProjector getScreenCoordCalculator() {
        return gp -> {
            if (mapView == null || gp == null) {
                return new int[] { 0, 0 };
            }
            final Point pt = projectLatLon(mapView, latLong(gp));
            return new int[]{(int) pt.x, (int) pt.y};
        };
    }

    /**
     * projects a latlon to a screen coordinate. Accomodates for all visual effects
     * eg zooming, rotation, tilting.
     */
    @NonNull
    private static Point projectLatLon(@NonNull final MapView mapView, @NonNull final LatLong latLong) {
        return mapView.getMapViewProjection().toPixels(latLong, true);
    }

}
