package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.maps.AbstractItemizedOverlay;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnCacheTapListener;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import android.content.res.Resources.NotFoundException;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.location.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MapsforgeCachesList extends AbstractItemizedOverlay {

    private List<MapsforgeCacheOverlayItem> items = new ArrayList<>();
    private boolean displayCircles = false;
    private Paint blockedCircle = null;
    private PaintFlagsDrawFilter setFilter = null;
    private PaintFlagsDrawFilter removeFilter = null;
    private MapItemFactory mapItemFactory = null;
    private OnCacheTapListener onCacheTapListener;

    public MapsforgeCachesList(final ItemizedOverlayImpl ovlImpl) {
        super(ovlImpl);

        populate();

        final MapProvider mapProvider = Settings.getMapProvider();
        mapItemFactory = mapProvider.getMapItemFactory();
    }

    public void updateItems(final Collection<MapsforgeCacheOverlayItem> itemsPre) {
        if (itemsPre == null) {
            return;
        }

        for (final CachesOverlayItemImpl item : itemsPre) {
            boundCenterBottom(item.getMarker(0).getDrawable());
        }

        // ensure no interference between the draw and content changing routines
        getOverlayImpl().lock();
        try {
            items = new ArrayList<>(itemsPre);

            setLastFocusedItemIndex(-1); // to reset tap during data change
            populate();
        } finally {
            getOverlayImpl().unlock();
        }
    }

    public boolean getCircles() {
        return displayCircles;
    }

    public void switchCircles() {
        displayCircles = !displayCircles;
    }

    public void setOnTapListener(final OnCacheTapListener listener) {
        this.onCacheTapListener = listener;
    }

    @Override
    public void draw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {

        // prevent content changes
        getOverlayImpl().lock();
        try {
            drawInternal(canvas, mapView.getMapProjection());

            super.draw(canvas, mapView, false);
        } finally {
            getOverlayImpl().unlock();
        }
    }

    @Override
    public void drawOverlayBitmap(final Canvas canvas, final Point drawPosition,
            final MapProjectionImpl projection, final byte drawZoomLevel) {

        drawInternal(canvas, projection);

        super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
    }

    private void drawInternal(final Canvas canvas, final MapProjectionImpl projection) {
        if (!displayCircles || items.isEmpty()) {
            return;
        }

        lazyInitializeDrawingObjects();
        canvas.setDrawFilter(setFilter);
        final int height = canvas.getHeight();
        final int width = canvas.getWidth();

        final int radius = calculateDrawingRadius(projection);
        final Point center = new Point();

        for (final CachesOverlayItemImpl item : items) {
            if (item.applyDistanceRule()) {
                final Geopoint itemCoord = item.getCoord().getCoords();
                final GeoPointImpl itemGeo = mapItemFactory.getGeoPointBase(itemCoord);
                projection.toPixels(itemGeo, center);
                if (center.x > -radius && center.y > -radius && center.x < width + radius && center.y < height + radius) {
                    // dashed circle around the waypoint
                    blockedCircle.setColor(0x66BB0000);
                    blockedCircle.setStyle(Style.STROKE);
                    canvas.drawCircle(center.x, center.y, radius, blockedCircle);

                    // filling the circle area with a transparent color
                    blockedCircle.setColor(0x44BB0000);
                    blockedCircle.setStyle(Style.FILL);
                    canvas.drawCircle(center.x, center.y, radius, blockedCircle);
                }
            }
        }
        canvas.setDrawFilter(removeFilter);
    }

    /**
     * Calculate the radius of the circle to be drawn for the first item only. Those circles are only 528 feet
     * (approximately 161 meters) in reality and therefore the minor changes due to the projection will not make any
     * visible difference at the zoom levels which are used to see the circles.
     *
     */
    private int calculateDrawingRadius(final MapProjectionImpl projection) {
        final float[] distanceArray = new float[1];
        final Geopoint itemCoord = items.get(0).getCoord().getCoords();

        Location.distanceBetween(itemCoord.getLatitude(), itemCoord.getLongitude(),
                itemCoord.getLatitude(), itemCoord.getLongitude() + 1, distanceArray);
        final float longitudeLineDistance = distanceArray[0];

        final GeoPointImpl itemGeo = mapItemFactory.getGeoPointBase(itemCoord);

        final Geopoint leftCoords = new Geopoint(itemCoord.getLatitude(),
                itemCoord.getLongitude() - 528.0 * IConversion.FEET_TO_KILOMETER * 1000.0 / longitudeLineDistance);
        final GeoPointImpl leftGeo = mapItemFactory.getGeoPointBase(leftCoords);

        final Point center = new Point();
        projection.toPixels(itemGeo, center);

        final Point left = new Point();
        projection.toPixels(leftGeo, left);

        return center.x - left.x;
    }

    private void lazyInitializeDrawingObjects() {
        if (blockedCircle == null) {
            blockedCircle = new Paint();
            blockedCircle.setAntiAlias(true);
            blockedCircle.setStrokeWidth(2.0f);
            blockedCircle.setARGB(127, 0, 0, 0);
            blockedCircle.setPathEffect(new DashPathEffect(new float[] { 3, 2 }, 0));
        }

        if (setFilter == null) {
            setFilter = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
        }
        if (removeFilter == null) {
            removeFilter = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);
        }
    }

    @Override
    public boolean onTap(final int index) {

        if (onCacheTapListener == null) {
            return false;
        }

        try {
            if (items.size() <= index) {
                return false;
            }

            // prevent concurrent changes
            getOverlayImpl().lock();
            CachesOverlayItemImpl item = null;
            try {
                if (index < items.size()) {
                    item = items.get(index);
                }
            } finally {
                getOverlayImpl().unlock();
            }

            if (item == null) {
                return false;
            }

            onCacheTapListener.onCacheTap(item.getCoord());

        } catch (final NotFoundException e) {
            Log.e("MapsforgeCachesList.onTap", e);
        }

        return true;
    }

    @Override
    public MapsforgeCacheOverlayItem createItem(final int index) {
        try {
            return items.get(index);
        } catch (final Exception e) {
            Log.w("MapsforgeCachesList.createItem", e);
        }

        return null;
    }

    @Override
    public int size() {
        return items.size();
    }


}
