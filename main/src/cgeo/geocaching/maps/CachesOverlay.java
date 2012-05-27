package cgeo.geocaching.maps;

import cgeo.geocaching.CachePopup;
import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
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
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
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

    public CachesOverlay(ItemizedOverlayImpl ovlImpl, Context contextIn) {
        super(ovlImpl);

        populate();

        context = contextIn;

        final MapProvider mapProvider = Settings.getMapProvider();
        mapItemFactory = mapProvider.getMapItemFactory();
    }

    public void updateItems(CachesOverlayItemImpl item) {
        List<CachesOverlayItemImpl> itemsPre = new ArrayList<CachesOverlayItemImpl>();
        itemsPre.add(item);

        updateItems(itemsPre);
    }

    public void updateItems(List<CachesOverlayItemImpl> itemsPre) {
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

    public boolean getCircles() {
        return displayCircles;
    }

    public void switchCircles() {
        displayCircles = !displayCircles;
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {

        drawInternal(canvas, mapView.getMapProjection());

        super.draw(canvas, mapView, false);
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {

        drawInternal(canvas, projection);

        super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
    }

    private void drawInternal(Canvas canvas, MapProjectionImpl projection) {
        if (!displayCircles || items.isEmpty()) {
            return;
        }

        // prevent content changes
        getOverlayImpl().lock();
        try {
            lazyInitializeDrawingObjects();
            canvas.setDrawFilter(setFilter);

            final int radius = calculateDrawingRadius(projection);
            final Point center = new Point();

            for (CachesOverlayItemImpl item : items) {
                final Geopoint itemCoord = item.getCoord().getCoords();
                final GeoPointImpl itemGeo = mapItemFactory.getGeoPointBase(itemCoord);
                projection.toPixels(itemGeo, center);

                final CacheType type = item.getType();
                if (type == null || type == CacheType.MULTI || type == CacheType.MYSTERY || type == CacheType.VIRTUAL || type.isEvent()) {
                    blockedCircle.setColor(0x66000000);
                    blockedCircle.setStyle(Style.STROKE);
                    canvas.drawCircle(center.x, center.y, radius, blockedCircle);
                } else {
                    blockedCircle.setColor(0x66BB0000);
                    blockedCircle.setStyle(Style.STROKE);
                    canvas.drawCircle(center.x, center.y, radius, blockedCircle);

                    blockedCircle.setColor(0x44BB0000);
                    blockedCircle.setStyle(Style.FILL);
                    canvas.drawCircle(center.x, center.y, radius, blockedCircle);
                }
            }
            canvas.setDrawFilter(removeFilter);
        } finally {
            getOverlayImpl().unlock();
        }
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
                cgCache cache = cgeoapplication.getInstance().loadCache(coordinate.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                RequestDetailsThread requestDetailsThread = new RequestDetailsThread(cache);
                if (!requestDetailsThread.requestRequired()) {
                    // don't show popup if we have enough details
                    progress.dismiss();
                }
                requestDetailsThread.start();
                return true;
            }

            if (coordinate.getCoordType() != null && coordinate.getCoordType().equalsIgnoreCase("waypoint") && coordinate.getId() > 0) {
                CGeoMap.markCacheAsDirty(coordinate.getGeocode());
                WaypointPopup.startActivity(context, coordinate.getId(), coordinate.getGeocode());
            } else {
                progress.dismiss();
                return false;
            }

            progress.dismiss();
        } catch (Exception e) {
            Log.e("cgMapOverlay.onTap: " + e.toString());
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
            Log.e("cgMapOverlay.createItem: " + e.toString());
        }

        return null;
    }

    @Override
    public int size() {
        try {
            return items.size();
        } catch (Exception e) {
            Log.e("cgMapOverlay.size: " + e.toString());
        }

        return 0;
    }

    private class RequestDetailsThread extends Thread {

        private final cgCache cache;

        public RequestDetailsThread(cgCache cache) {
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
