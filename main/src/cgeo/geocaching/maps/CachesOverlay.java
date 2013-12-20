package cgeo.geocaching.maps;

import cgeo.geocaching.CachePopup;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.R;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.connector.gc.GCMap;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.location.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CachesOverlay extends AbstractItemizedOverlay {

    private List<CachesOverlayItemImpl> items = new ArrayList<CachesOverlayItemImpl>();
    private Context context = null;
    private boolean displayCircles = false;
    private Progress progress = new Progress();
    private Paint blockedCircle = null;
    private PaintFlagsDrawFilter setFilter = null;
    private PaintFlagsDrawFilter removeFilter = null;
    private MapItemFactory mapItemFactory = null;
    private ScaleDrawer scaleDrawer = null;
    private PositionDrawer positionDrawer;

    public CachesOverlay(ItemizedOverlayImpl ovlImpl, Context contextIn, Activity activity) {
        super(ovlImpl);

        populate();

        context = contextIn;

        final MapProvider mapProvider = Settings.getMapProvider();
        mapItemFactory = mapProvider.getMapItemFactory();
        positionDrawer = new PositionDrawer(activity);
        scaleDrawer = new ScaleDrawer(activity);
    }

    void updateItems(CachesOverlayItemImpl item) {
        List<CachesOverlayItemImpl> itemsPre = new ArrayList<CachesOverlayItemImpl>();
        itemsPre.add(item);

        updateItems(itemsPre);
    }

    void updateItems(List<CachesOverlayItemImpl> itemsPre) {
        if (itemsPre == null) {
            return;
        }

        for (CachesOverlayItemImpl item : itemsPre) {
            item.setMarker(boundCenterBottom(item.getMarker(0)));
        }

        // ensure no interference between the draw and content changing routines
        getOverlayImpl().lock();
        try {
            items = new ArrayList<CachesOverlayItemImpl>(itemsPre);

            setLastFocusedItemIndex(-1); // to reset tap during data change
            populate();
        } finally {
            getOverlayImpl().unlock();
        }
    }

    boolean getCircles() {
        return displayCircles;
    }

    void switchCircles() {
        displayCircles = !displayCircles;
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
        drawInternalBefore(canvas, mapView.getMapProjection(), mapView);
        super.draw(canvas, mapView, false);
        drawInternalAfter(canvas, mapView.getMapProjection(), mapView);
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {
        drawInternalBefore(canvas, projection, getOverlayImpl().getMapViewImpl());
        super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
        drawInternalAfter(canvas, projection, getOverlayImpl().getMapViewImpl());
    }

    private void drawInternalBefore(Canvas canvas, MapProjectionImpl projection, MapViewImpl mapView) {
        // prevent content changes
        getOverlayImpl().lock();
        try {
            drawCircles(canvas, projection);
        } finally {
            getOverlayImpl().unlock();
        }
    }

    private void drawInternalAfter(Canvas canvas, MapProjectionImpl projection, MapViewImpl mapView) {
        // prevent content changes
        getOverlayImpl().lock();
        try {
            scaleDrawer.drawScale(canvas, mapView);
            positionDrawer.drawPosition(canvas, projection);
        } finally {
            getOverlayImpl().unlock();
        }
    }

    private void drawCircles(Canvas canvas, MapProjectionImpl projection) {
        if (!displayCircles || items.isEmpty()) {
            return;
        }
        lazyInitializeDrawingObjects();
        canvas.setDrawFilter(setFilter);
        final int height = canvas.getHeight();
        final int width = canvas.getWidth();

        final int radius = calculateDrawingRadius(projection);
        final Point center = new Point();

        for (CachesOverlayItemImpl item : items) {
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

    public void setCoordinates(Location coordinatesIn) {
        positionDrawer.setCoordinates(coordinatesIn);
    }

    public Location getCoordinates() {
        return positionDrawer.getCoordinates();
    }

    public void setHeading(float bearingNow) {
        positionDrawer.setHeading(bearingNow);
    }

    public float getHeading() {
        return positionDrawer.getHeading();
    }

    public ArrayList<Location> getHistory() {
        return positionDrawer.getHistory();
    }

    public void setHistory(ArrayList<Location> history) {
        positionDrawer.setHistory(history);
    }

    /**
     * calculate the radius of the circle to be drawn for the first item only. Those circles are only 161 meters in
     * reality and therefore the minor changes due to the projection will not make any visible difference at the zoom
     * levels which are used to see the circles.
     *
     * @param projection
     * @return
     */
    private int calculateDrawingRadius(MapProjectionImpl projection) {
        float[] distanceArray = new float[1];
        final Geopoint itemCoord = items.get(0).getCoord().getCoords();

        Location.distanceBetween(itemCoord.getLatitude(), itemCoord.getLongitude(),
                itemCoord.getLatitude(), itemCoord.getLongitude() + 1, distanceArray);
        final float longitudeLineDistance = distanceArray[0];

        final GeoPointImpl itemGeo = mapItemFactory.getGeoPointBase(itemCoord);

        final Geopoint leftCoords = new Geopoint(itemCoord.getLatitude(),
                itemCoord.getLongitude() - 161 / longitudeLineDistance);
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
    public boolean onTap(int index) {

        try {
            if (items.size() <= index) {
                return false;
            }

            progress.show(context, context.getResources().getString(R.string.map_live), context.getResources().getString(R.string.cache_dialog_loading_details), true, null);

            CachesOverlayItemImpl item = null;
            // prevent concurrent changes
            getOverlayImpl().lock();
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

            final IWaypoint coordinate = item.getCoord();

            if (StringUtils.isNotBlank(coordinate.getCoordType()) && coordinate.getCoordType().equalsIgnoreCase("cache") && StringUtils.isNotBlank(coordinate.getGeocode())) {
                Geocache cache = DataStore.loadCache(coordinate.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                RequestDetailsThread requestDetailsThread = new RequestDetailsThread(cache);
                if (!requestDetailsThread.requestRequired()) {
                    // don't show popup if we have enough details
                    progress.dismiss();
                }
                requestDetailsThread.start();
                return true;
            }

            if (coordinate.getCoordType() != null && coordinate.getCoordType().equalsIgnoreCase("waypoint") && coordinate.getId() >= 0) {
                CGeoMap.markCacheAsDirty(coordinate.getGeocode());
                WaypointPopup.startActivity(context, coordinate.getId(), coordinate.getGeocode());
            } else {
                progress.dismiss();
                return false;
            }

            progress.dismiss();
        } catch (NotFoundException e) {
            Log.e("CachesOverlay.onTap", e);
            if (progress != null) {
                progress.dismiss();
            }
        }

        return true;
    }

    @Override
    public CachesOverlayItemImpl createItem(int index) {
        try {
            return items.get(index);
        } catch (Exception e) {
            Log.e("CachesOverlay.createItem", e);
        }

        return null;
    }

    @Override
    public int size() {
        try {
            return items.size();
        } catch (Exception e) {
            Log.e("CachesOverlay.size", e);
        }

        return 0;
    }

    private class RequestDetailsThread extends Thread {

        private final Geocache cache;

        public RequestDetailsThread(Geocache cache) {
            this.cache = cache;
        }

        public boolean requestRequired() {
            return CacheType.UNKNOWN == cache.getType() || cache.getDifficulty() == 0;
        }

        @Override
        public void run() {
            if (requestRequired()) {
                /* final SearchResult search = */GCMap.searchByGeocodes(Collections.singleton(cache.getGeocode()));
            }
            CGeoMap.markCacheAsDirty(cache.getGeocode());
            CachePopup.startActivity(context, cache.getGeocode());
            progress.dismiss();
        }
    }

}
