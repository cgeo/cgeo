package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.maps.google.v2.BitmapDescriptorCache;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.Pair;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class GoogleV2GeoItemLayer implements IProviderGeoItemLayer<Pair<Object, Object>> {

    private static final GeopointConverter<LatLng> GP_CONVERTER = new GeopointConverter<>(
            gc -> new LatLng(gc.getLatitude(), gc.getLongitude()),
            ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    private GoogleMap map;
    private Resources resources;
    private int zLevel;

    public GoogleV2GeoItemLayer(final GoogleMap map, final Resources resources) {
        this.map = map;
        this.resources = resources;
    }

    public GoogleV2GeoItemLayer(final GoogleMap map) {
        this(map, CgeoApplication.getInstance().getResources());
    }

    @Override
    public void init(final int zLevel) {

        this.zLevel = zLevel;
        if (map != null) {
            map.setOnMarkerClickListener(m -> {
                Log.iForce("GoogleV2: Clicked on marker: " + m);
                return true;
            });
        }
    }

    @Override
    public void destroy() {
        this.map = null;
        this.resources = null;
    }

    @Override
    public Pair<Object, Object> add(final GeoPrimitive item) {
        final GoogleMap map = this.map;
        if (map == null) {
            return null;
        }

        final int strokeColor = GeoStyle.getStrokeColor(item.getStyle());
        final int fillColor = GeoStyle.getFillColor(item.getStyle());
        final float strokeWidth = ViewUtils.dpToPixel(GeoStyle.getStrokeWidth(item.getStyle()));

        final Object context;
        switch (item.getType()) {
            case MARKER:
                context = null;
                break;
            case CIRCLE:
                context = map.addCircle(new CircleOptions()
                        .strokeWidth(strokeWidth)
                        .strokeColor(strokeColor)
                        .fillColor(fillColor)
                        .center(GP_CONVERTER.to(item.getCenter()))
                        .radius(item.getRadius() * 1000)
                        .zIndex(zLevel));
                break;
            case POLYGON:
                context = map.addPolygon(new PolygonOptions()
                        .strokeWidth(strokeWidth)
                        .strokeColor(strokeColor)
                        .fillColor(fillColor)
                        .addAll(GP_CONVERTER.toList(item.getPoints()))
                        .zIndex(zLevel));
                break;
            case POLYLINE:
            default:
                context = map.addPolyline(new PolylineOptions()
                        .width(ViewUtils.dpToPixel(GeoStyle.getStrokeWidth(item.getStyle())))
                        .color(GeoStyle.getStrokeColor(item.getStyle()))
                        .addAll(GP_CONVERTER.toList(item.getPoints()))
                        .zIndex(zLevel));
                break;
        }

        Object marker = null;
        final GeoIcon icon = item.getIcon();
        if (icon != null && item.getCenter() != null && icon.getBitmap() != null) {
            marker = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorCache.toBitmapDescriptor(new BitmapDrawable(resources, icon.getBitmap())))
                .rotation(icon.getAngle())
                .position(GP_CONVERTER.to(item.getCenter()))
                .anchor(icon.getXAnchor(), icon.getYAnchor())
                .zIndex(zLevel));
        }

        return new Pair<>(context, marker);
    }

    @Override
    public void remove(final GeoPrimitive item, final Pair<Object, Object> context) {
        removeSingle(context.first);
        removeSingle(context.second);
    }

    private void removeSingle(final Object context) {
        if (context instanceof Polyline) {
            ((Polyline) context).remove();
        } else if (context instanceof Polygon) {
            ((Polygon) context).remove();
        } else if (context instanceof Circle) {
            ((Circle) context).remove();
        } else if (context instanceof Marker) {
            ((Marker) context).remove();
        }
    }

    @Override
    public Func1<Geopoint, Point> getScreenCoordCalculator() {
        final Projection proj = map.getProjection();
        return gp -> proj.toScreenLocation(GP_CONVERTER.to(gp));
    }

}
