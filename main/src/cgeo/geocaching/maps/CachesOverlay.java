package cgeo.geocaching.maps;

import cgeo.geocaching.CachePopup;
import cgeo.geocaching.R;
import cgeo.geocaching.WaypointPopup;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.connector.gc.GCMap;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

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
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CachesOverlay extends AbstractItemizedOverlay {

    private List<CachesOverlayItemImpl> items = new ArrayList<>();
    private Context context = null;
    private boolean displayCircles = false;
    private final Progress progress = new Progress();
    private Paint blockedCircle = null;
    private PaintFlagsDrawFilter setFilter = null;
    private PaintFlagsDrawFilter removeFilter = null;
    private MapItemFactory mapItemFactory = null;

    public CachesOverlay(final ItemizedOverlayImpl ovlImpl, final Context contextIn) {
        super(ovlImpl);

        populate();

        context = contextIn;

        final MapProvider mapProvider = Settings.getMapProvider();
        mapItemFactory = mapProvider.getMapItemFactory();
    }

    void updateItems(final CachesOverlayItemImpl item) {
        final List<CachesOverlayItemImpl> itemsPre = new ArrayList<>();
        itemsPre.add(item);

        updateItems(itemsPre);
    }

    void updateItems(final List<CachesOverlayItemImpl> itemsPre) {
        if (itemsPre == null) {
            return;
        }

        for (final CachesOverlayItemImpl item : itemsPre) {
            item.setMarker(boundCenterBottom(item.getMarker(0)));
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

    boolean getCircles() {
        return displayCircles;
    }

    void switchCircles() {
        displayCircles = !displayCircles;
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

        try {
            if (items.size() <= index) {
                return false;
            }

            progress.show(context, context.getString(R.string.map_live), context.getString(R.string.cache_dialog_loading_details), true, null);

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

            final IWaypoint coordinate = item.getCoord();
            final CoordinatesType coordType = coordinate.getCoordType();

            if (coordType == CoordinatesType.CACHE && StringUtils.isNotBlank(coordinate.getGeocode())) {
                final Geocache cache = DataStore.loadCache(coordinate.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    final RequestDetailsThread requestDetailsThread = new RequestDetailsThread(cache);
                    if (!requestDetailsThread.requestRequired()) {
                        // don't show popup if we have enough details
                        progress.dismiss();
                    }
                    requestDetailsThread.start();
                    return true;
                }
                progress.dismiss();
                return false;
            }

            if (coordType == CoordinatesType.WAYPOINT && coordinate.getId() >= 0) {
                CGeoMap.markCacheAsDirty(coordinate.getGeocode());
                WaypointPopup.startActivity(context, coordinate.getId(), coordinate.getGeocode());
            } else {
                progress.dismiss();
                return false;
            }

            progress.dismiss();
        } catch (final NotFoundException e) {
            Log.e("CachesOverlay.onTap", e);
            progress.dismiss();
        }

        return true;
    }

    @Override
    public CachesOverlayItemImpl createItem(final int index) {
        try {
            return items.get(index);
        } catch (final Exception e) {
            Log.e("CachesOverlay.createItem", e);
        }

        return null;
    }

    @Override
    public int size() {
        return items.size();
    }

    private class RequestDetailsThread extends Thread {

        @NonNull private final Geocache cache;

        RequestDetailsThread(@NonNull final Geocache cache) {
            this.cache = cache;
        }

        public boolean requestRequired() {
            return cache.getType() == CacheType.UNKNOWN || cache.getDifficulty() == 0;
        }

        @Override
        public void run() {
            if (requestRequired()) {
                try {
                /* final SearchResult search = */GCMap.searchByGeocodes(Collections.singleton(cache.getGeocode()));
                } catch (final Exception ex) {
                    Log.w("Error requesting cache popup info", ex);
                    ActivityMixin.showToast((Activity) context, R.string.err_request_popup_info);
                }
            }
            CGeoMap.markCacheAsDirty(cache.getGeocode());
            CachePopup.startActivity(context, cache.getGeocode());
            progress.dismiss();
        }
    }

}
