package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.utils.CollectionStream;

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

    public static Layer createGeoObjectLayer(final List<GeoPrimitive> objects, final DisplayModel displayModel) {
        final GroupLayer gl = new GroupLayer();
        gl.setDisplayModel(displayModel);
        for (GeoPrimitive go : objects) {
            final Paint strokePaint = createPaint(GeoStyle.getStrokeColor(go.getStyle()));
            strokePaint.setStrokeWidth(GeoStyle.getStrokeWidth(go.getStyle()));
            strokePaint.setStyle(Style.STROKE);
            final Paint fillPaint = createPaint(GeoStyle.getFillColor(go.getStyle()));
            fillPaint.setStyle(Style.FILL);
            final Layer goLayer;
            switch (go.getType()) {
                case MARKER:
                    goLayer = new FixedPixelCircle(latLong(go.getPoints().get(0)), 5f, strokePaint, strokePaint);
                    break;
                case POLYLINE:
                    final Polyline pl = new Polyline(strokePaint, AndroidGraphicFactory.INSTANCE);
                    pl.addPoints(CollectionStream.of(go.getPoints()).map(GeoObjectLayer::latLong).toList());
                    goLayer = pl;
                    break;
                case POLYGON:
                default:
                    final Polygon po = new Polygon(fillPaint, strokePaint, AndroidGraphicFactory.INSTANCE);
                    po.addPoints(CollectionStream.of(go.getPoints()).map(GeoObjectLayer::latLong).toList());
                    goLayer = po;
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
