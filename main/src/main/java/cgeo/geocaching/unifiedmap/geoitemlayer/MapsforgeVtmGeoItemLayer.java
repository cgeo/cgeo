package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.Log;

import android.graphics.BitmapFactory;
import android.util.Pair;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.Point;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.CircleDrawable;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

public class MapsforgeVtmGeoItemLayer implements IProviderGeoItemLayer<Pair<Drawable, MarkerInterface>> {

    private static final GeopointConverter<GeoPoint> GP_CONVERTER = new GeopointConverter<>(
            gc -> new GeoPoint(gc.getLatitude(), gc.getLongitude()),
            ll -> new Geopoint(ll.latitudeE6, ll.longitudeE6)
    );

    private static final String LOG_PRAEFIX = "MfVtmGeoItemLayer:";

    private Map map;

    //for markers
    private ItemizedLayer markerLayer;
    private MarkerSymbol defaultMarkerSymbol;

    //for geometries
    private VectorLayer vectorLayer;

    public MapsforgeVtmGeoItemLayer(final Map map) {
        this.map = map;
    }

    @Override
    public void init(final int zLevel) {
        Log.iForce(LOG_PRAEFIX + "init");

        //initialize marker layer
        final Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.cgeo_notification));
        defaultMarkerSymbol = new MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
        markerLayer = new ItemizedLayer(map, defaultMarkerSymbol);
        this.map.layers().add(zLevel, markerLayer);

        //initialize vector layer
        vectorLayer = new VectorLayer(this.map);
        this.map.layers().add(zLevel, vectorLayer);

    }

    @Override
    public Pair<Drawable, MarkerInterface> add(final GeoPrimitive item) {
        Log.iForce(LOG_PRAEFIX + "add " + item);

        final Style style = Style.builder()
                .strokeWidth(ViewUtils.dpToPixel(GeoStyle.getStrokeWidth(item.getStyle()) / 2f))
                .strokeColor(GeoStyle.getStrokeColor(item.getStyle()))
                .fillColor(GeoStyle.getFillColor(item.getStyle()))
                .build();

        Drawable drawable = null;
        switch (item.getType()) {
            case MARKER:
                break;
            case CIRCLE:
                drawable = new CircleDrawable(GP_CONVERTER.to(item.getCenter()), item.getRadius(), style);
                break;
            case POLYGON:
                drawable = new PolygonDrawable(GP_CONVERTER.toList(item.getPoints()), style);
                break;
            case POLYLINE:
            default:
                drawable = new LineDrawable(GP_CONVERTER.toList(item.getPoints()), style);
                break;
        }

        if (drawable != null) {
            vectorLayer.add(drawable);
            vectorLayer.update();
        }

        MarkerItem marker = null;
        if (item.getIcon() != null) {
            final GeoIcon icon = item.getIcon();
            marker = new MarkerItem("", "", GP_CONVERTER.to(item.getCenter()));
            marker.setMarker(new MarkerSymbol(new AndroidBitmap(icon.getBitmap()),
                    icon.getXAnchor(), icon.getYAnchor(), true));
            marker.setRotation(item.getIcon().getRotation());
            markerLayer.addItem(marker);
            markerLayer.update();
        }

        return new Pair<>(drawable, marker);
    }

    @Override
    public void remove(final GeoPrimitive item, final Pair<Drawable, MarkerInterface> context) {
        Log.iForce(LOG_PRAEFIX + "remove");
        if (context.first != null) {
            vectorLayer.remove(context.first);
            vectorLayer.update();
        }
        if (context.second != null) {
            markerLayer.removeItem(context.second);
        }
    }

    @Override
    public void destroy() {
        Log.iForce(LOG_PRAEFIX + "destroy");
        map.layers().remove(markerLayer);
        map.layers().remove(vectorLayer);
        map = null;
        markerLayer = null;
        vectorLayer = null;
        defaultMarkerSymbol = null;
    }

    @Override
    public ToScreenProjector getScreenCoordCalculator() {

          return gp -> {
              if (map == null || map.viewport() == null) {
                  return null;
              }
              final Point pt = new Point();
              map.viewport().toScreenPoint(GP_CONVERTER.to(gp), false, pt);
              return new int[]{(int) pt.x, (int) pt.y};
        };
    }

}

