package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.ColorInt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.overlay.Polygon;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.model.DisplayModel;

public class GeoObjectLayer extends GroupLayer {

    private final Map<String, Layer> geoObjectLayers = new HashMap<>();

    public void addGeoObjectLayer(final String key, final Layer goLayer) {
        removeGeoObjectLayer(key);
        geoObjectLayers.put(key, goLayer);
        this.layers.add(goLayer);
    }

    public static Layer createGeoObjectLayer(final List<GeoPrimitive> objects, final DisplayModel displayModel,
            final float defaultWidth, final int defaultStrokeColor, final int defaultFillColor, final Func1<Float, Float> widthAdjuster) {
        final GroupLayer gl = new GroupLayer();
        gl.setDisplayModel(displayModel);
        for (GeoPrimitive item : objects) {
            final Paint strokePaint = createPaint(GeoStyle.getStrokeColor(item.getStyle(), defaultStrokeColor));
            strokePaint.setStrokeWidth(widthAdjuster.call(GeoStyle.getStrokeWidth(item.getStyle(), defaultWidth)));
            strokePaint.setStyle(Style.STROKE);
            final Paint fillPaint = createPaint(GeoStyle.getFillColor(item.getStyle(), defaultFillColor));
            fillPaint.setStyle(Style.FILL);
            final Layer goLayer;
            switch (item.getType()) {
                case MARKER:
                case CIRCLE:
                    final float radius = item.getType() == GeoItem.GeoType.MARKER ? 5f : item.getRadius() * 10;
                    goLayer = new FixedPixelCircle(latLong(item.getPoints().get(0)), widthAdjuster.call(radius), strokePaint, strokePaint);
                    break;
                case POLYLINE:
                    final Polyline pl = new Polyline(strokePaint, AndroidGraphicFactory.INSTANCE);
                    pl.addPoints(CollectionStream.of(item.getPoints()).map(GeoObjectLayer::latLong).toList());
                    goLayer = pl;
                    break;
                case POLYGON:
                default:
                    final Polygon po = new Polygon(fillPaint, strokePaint, AndroidGraphicFactory.INSTANCE);
                    po.addPoints(CollectionStream.of(item.getPoints()).map(GeoObjectLayer::latLong).toList());
                    if (item.getHoles() == null) {
                        goLayer = po;
                    } else {
                        final GroupLayer group = new GroupLayer();
                        group.layers.add(po);
                        for (List<Geopoint> hole : item.getHoles()) {
                            final Polyline plHole = new Polyline(strokePaint, AndroidGraphicFactory.INSTANCE);
                            plHole.addPoints(CollectionStream.of(hole).map(GeoObjectLayer::latLong).toList());
                            group.layers.add(plHole);
                        }
                        goLayer = group;
                    }
                    break;
            }
            goLayer.setDisplayModel(displayModel);
            gl.layers.add(goLayer);
        }
        return gl;
    }

    public void removeGeoObjectLayer(final String key) {
        final Layer oldLayer = geoObjectLayers.remove(key);
        if (oldLayer != null) {
            this.layers.remove(oldLayer);
        }
    }

    private static Paint createPaint(@ColorInt final int color) {
        final Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
        p.setColor(color);
        return p;
    }

    private static LatLong latLong(final Geopoint gp) {
        return new LatLong(gp.getLatitude(), gp.getLongitude());
    }


}
