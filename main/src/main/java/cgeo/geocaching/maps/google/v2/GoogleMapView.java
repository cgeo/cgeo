package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.CGeoMap;
import cgeo.geocaching.maps.DistanceDrawer;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.ScaleDrawer;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapReadyCallback;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnCacheTapListener;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.mapsforge.AbstractMapsforgeMapSource;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.FilterUtils;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.maps.google.v2.GoogleMapUtils.isGoogleMapsAvailable;
import static cgeo.geocaching.storage.extension.OneTimeDialogs.DialogType.MAP_AUTOROTATION_DISABLE;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.VisibleRegion;

public class GoogleMapView extends MapView implements MapViewImpl<GoogleCacheOverlayItem>, OnMapReadyCallback {

    private OnMapDragListener onDragListener;
    private final GoogleMapController mapController = new GoogleMapController();
    private GoogleMap googleMap;
    private MapReadyCallback mapReadyCallback;

    private LatLng viewCenter;
    private float zoomLevel;
    private VisibleRegion visibleRegion;

    private GoogleCachesList cachesList;
    private GestureDetector gestureDetector;
    private Collection<GoogleCacheOverlayItem> cacheItems;

    private OnCacheTapListener onCacheTapListener;
    private boolean showCircles = false;
    private boolean canDisableAutoRotate = false;

    private final Lock lock = new ReentrantLock();

    private final ScaleDrawer scaleDrawer = new ScaleDrawer();
    private DistanceDrawer distanceDrawer;

    private WeakReference<AbstractBottomNavigationActivity> activityRef;
    private WeakReference<PositionAndHistory> positionAndHistoryRef;
    private View root = null;

    private int fromList = StoredList.TEMPORARY_LIST.id;

    public interface PostRealDistance {
        void postRealDistance(float realDistance);
    }

    public GoogleMapView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public GoogleMapView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }


    public void onMapReady(final GoogleMap googleMap) {
        if (this.googleMap != null) {
            if (this.googleMap == googleMap) {
                return;
            } else {
                throw new IllegalStateException("Could not set new google map - already set");
            }
        }
        this.googleMap = googleMap;
        mapController.setGoogleMap(googleMap);

        final GoogleMapsThemes theme = GoogleMapsThemes.getByName(Settings.getSelectedGoogleMapTheme());
        googleMap.setMapStyle(theme.getMapStyleOptions(getContext()));

        cachesList = new GoogleCachesList(googleMap);
        googleMap.setOnCameraMoveListener(this::recognizePositionChange);
        googleMap.setOnCameraIdleListener(this::recognizePositionChange);
        googleMap.setOnMarkerClickListener(marker -> {
            // onCacheTapListener will fire on onSingleTapUp event, not here, because this event
            // is fired 300 ms after map tap, which is too slow for UI

            // suppress default behaviour (yeah, true == suppress)
            // ("The default behavior is for the camera to move to the marker and an info window to appear.")
            return true;
        });
        googleMap.setOnMapLongClickListener(tapLatLong -> {
            if (Settings.isLongTapOnMapActivated()) {
                boolean hitWaypoint = false;
                final GoogleCacheOverlayItem closest = closest(new Geopoint(tapLatLong.latitude, tapLatLong.longitude));
                final Point tappedPoint = googleMap.getProjection().toScreenLocation(tapLatLong);
                if (closest != null) {
                    final Point waypointPoint = googleMap.getProjection().toScreenLocation(new LatLng(closest.getCoord().getCoords().getLatitude(), closest.getCoord().getCoords().getLongitude()));
                    if (insideCachePointDrawable(tappedPoint, waypointPoint, closest.getMarker(0).getDrawable())) {
                        hitWaypoint = true;
                        ((CGeoMap) onCacheTapListener).toggleRouteItem(closest.getCoord());
                    }
                }
                if (!hitWaypoint && null != positionAndHistoryRef) {
                    final PositionAndHistory positionAndHistory = positionAndHistoryRef.get();
                    if (null != positionAndHistory) {
                        positionAndHistory.setLongTapLatLng(tapLatLong);
                        ((CGeoMap) onCacheTapListener).triggerLongTapContextMenu(tappedPoint);
                    }
                }
            }
        });
        adaptLayoutForActionbar(true);
        googleMap.setOnCameraChangeListener(cameraPosition -> {
            // check for tap on compass rose, which resets bearing to 0.0
            // only active, if it has been not equal to 0.0 before
            final float bearing = cameraPosition.bearing;
            if (canDisableAutoRotate && bearing == 0.0f && Settings.getMapRotation() == Settings.MAPROTATION_AUTO) {
                canDisableAutoRotate = false;
                final Context context = getContext();
                Dialogs.advancedOneTimeMessage(context, MAP_AUTOROTATION_DISABLE, context.getString(MAP_AUTOROTATION_DISABLE.messageTitle), context.getString(MAP_AUTOROTATION_DISABLE.messageText), "", true, null, () -> {
                    Settings.setMapRotation(Settings.MAPROTATION_MANUAL);

                    // notify overlay
                    if (null != positionAndHistoryRef) {
                        final PositionAndHistory positionAndHistory = positionAndHistoryRef.get();
                        if (null != positionAndHistory) {
                            positionAndHistory.updateMapRotation();
                        }
                    }
                });
            } else if (bearing != 0.0f) {
                canDisableAutoRotate = true;
            }
            Log.d("bearing=" + cameraPosition.bearing + ", tilt=" + cameraPosition.tilt + ", canDisable=" + canDisableAutoRotate);
        });
        if (mapReadyCallback != null) {
            mapReadyCallback.mapReady();
            mapReadyCallback = null;
        }
        redraw();
    }

    @Override
    public void setListId(final int listId) {
        fromList = listId;
    }

    private void recognizePositionChange() {
        final CameraPosition cameraPosition = googleMap.getCameraPosition();
        // update all variable, which getters are available only in main thread
        viewCenter = cameraPosition.target;
        zoomLevel = cameraPosition.zoom;
        final VisibleRegion newVisibleRegion = googleMap.getProjection().getVisibleRegion();
        if (newVisibleRegion != null) {
            visibleRegion = newVisibleRegion;
        }
        invalidate(); // force redraw to draw scale
    }

    private void initialize(final Context context) {
        if (isInEditMode()) {
            return;
        }

        activityRef = new WeakReference<>((AbstractBottomNavigationActivity) context);

        if (!isGoogleMapsAvailable(context)) {
            // either play services are missing (should have been caught in MapProviderFactory) or Play Services version does not support this Google Maps API version
            SimpleDialog.of((Activity) context).setTitle(R.string.warn_gm_not_available).setMessage(R.string.switch_to_mf).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm((dialog, whichButton) -> {
                // switch to first Mapsforge mapsource found
                final Collection<MapSource> mapSources = MapProviderFactory.getMapSources();
                for (final MapSource mapSource : mapSources) {
                    if (mapSource instanceof AbstractMapsforgeMapSource) {
                        Settings.setMapSource(mapSource);
                        SimpleDialog.of((Activity) context).setTitle(R.string.warn_gm_not_available).setMessage(R.string.switched_to_mf).show((dialog2, whichButton2) -> ((Activity) context).finish());
                        break;
                    }
                }
            });
        }

        getMapAsync(this);
        gestureDetector = new GestureDetector(context, new GestureListener((AbstractBottomNavigationActivity) context));
    }


    @Override
    public void setBuiltInZoomControls(final boolean b) {
        if (googleMap == null) {
            return;
        }
        googleMap.getUiSettings().setZoomControlsEnabled(b);
    }

    @Override
    public void displayZoomControls(final boolean takeFocus) {
        // nothing to do here, TODO merge design with mapsforge zoom controls?
    }

    @Override
    public MapControllerImpl getMapController() {
        return mapController;
    }

    @Override
    public GeoPointImpl getMapViewCenter() {
        if (viewCenter == null) {
            return null;
        }
        return new GoogleGeoPoint(viewCenter);
    }

    @Override
    public int getLatitudeSpan() {
        if (visibleRegion == null) {
            return -1;
        }
        return (int) (Math.abs(visibleRegion.latLngBounds.northeast.latitude - visibleRegion.latLngBounds.southwest.latitude) * 1e6);
    }

    @Override
    public int getLongitudeSpan() {
        if (visibleRegion == null) {
            return -1;
        }
        return (int) (Math.abs(visibleRegion.latLngBounds.northeast.longitude - visibleRegion.latLngBounds.southwest.longitude) * 1e6);
    }

    @Override
    public Viewport getViewport() {
        if (visibleRegion == null) {
            return null;
        }
        return new Viewport(new GoogleGeoPoint(visibleRegion.farLeft), new GoogleGeoPoint(visibleRegion.nearRight));
    }

    @Override
    public void clearOverlays() {
        // do nothing, there are no overlays to be cleared
    }

    @Override
    public MapProjectionImpl getMapProjection() {
        if (googleMap == null) {
            return null;
        }
        return new GoogleMapProjection(googleMap.getProjection());
    }

    @Override
    public PositionAndHistory createAddPositionAndScaleOverlay(final View root, final Geopoint coords, final String geocode) {
        this.root = root;
        if (googleMap == null) {
            throw new IllegalStateException("Google map not initialized yet"); // TODO check
        }
        final GoogleOverlay ovl = new GoogleOverlay(googleMap, this, realDistance -> {
            if (distanceDrawer != null) {
                distanceDrawer.setRealDistance(realDistance);
                this.invalidate();
            }
        }, routeDistance -> {
            if (distanceDrawer != null) {
                distanceDrawer.setRouteDistance(routeDistance);
                this.invalidate();
            }
        });
        setDestinationCoords(coords);
        positionAndHistoryRef = new WeakReference<>(ovl.getBase());
        return positionAndHistoryRef.get();
    }

    @Override
    public int getMapZoomLevel() {
        return googleMap != null ? (int) zoomLevel : -1;
    }

    @Override
    public void zoomToBounds(final Viewport bounds, final GeoPointImpl center) {
        mapController.zoomToSpan(bounds.topRight.getLatitudeE6() - bounds.bottomLeft.getLatitudeE6(), bounds.topRight.getLongitudeE6() - bounds.bottomLeft.getLongitudeE6());
        mapController.animateTo(center);
    }

    @Override
    public void setMapSource() {
        if (googleMap == null) {
            return;
        }
        final GoogleMapProvider.AbstractGoogleMapSource mapSource = (GoogleMapProvider.AbstractGoogleMapSource) Settings.getMapSource();
        googleMap.setMapType(mapSource.mapType);
        googleMap.setIndoorEnabled(mapSource.indoorEnabled);
    }

    @Override
    public void repaintRequired(final GeneralOverlay overlay) {
        // FIXME add recheck/readd markers and overlay
        if (null != positionAndHistoryRef) {
            final PositionAndHistory positionAndHistory = positionAndHistoryRef.get();
            if (null != positionAndHistory) {
                positionAndHistory.repaintRequired();
            }
        }
    }

    @Override
    public void setOnDragListener(final OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        // onTouchEvent is not working for Google's MapView
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void setDestinationCoords(final Geopoint destCoords) {
        setDistanceDrawer(destCoords);
    }

    /**
     * needed to provide current coordinates for distanceDrawer
     * called only in GooglePositionAndHistory
     */
    @Override
    public void setCoordinates(final Location coordinates) {
        if (distanceDrawer != null) {
            distanceDrawer.setCoordinates(coordinates);
        }
    }

    public Geopoint getDestinationCoords() {
        if (distanceDrawer != null) {
            return distanceDrawer.getDestinationCoords();
        } else {
            return null;
        }
    }

    public float getBearing() {
        // even thought google map support rotation, if the marker for current position is set as
        // flat (.flat(true)), the rotation is relative to map north, not view top, so the correct
        // value to be returned in this method is 0. TODO?
        return 0;
    }

    /**
     * can be made static if nonstatic inner clases could have static methods
     */
    private boolean insideCachePointDrawable(final Point p, final Point drawP, final Drawable d) {
        final int width = d.getIntrinsicWidth();
        final int height = d.getIntrinsicHeight();
        final int diffX = p.x - drawP.x;
        final int diffY = p.y - drawP.y;
        // assume drawable is drawn above drawP
        return
                Math.abs(diffX) < width / 2 &&
                        diffY > -height && diffY < 0;

    }

    private class GestureListener extends SimpleOnGestureListener {

        private final WeakReference<AbstractBottomNavigationActivity> activityRef;

        GestureListener(final AbstractBottomNavigationActivity activity) {
            super();
            this.activityRef = new WeakReference<>(activity);
        }
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            // no need to move to new location, google maps will do it for us
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return false;
        }

        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            // is map already initialized?
            if (googleMap != null) {
                final Point p = new Point((int) e.getX(), (int) e.getY());
                final LatLng latLng = googleMap.getProjection().fromScreenLocation(p);
                if (latLng != null && onCacheTapListener != null) {
                    final GoogleCacheOverlayItem closest = closest(new Geopoint(latLng.latitude, latLng.longitude));
                    if (closest != null) {
                        final Point waypointPoint = googleMap.getProjection().toScreenLocation(new LatLng(closest.getCoord().getCoords().getLatitude(), closest.getCoord().getCoords().getLongitude()));
                        if (insideCachePointDrawable(p, waypointPoint, closest.getMarker(0).getDrawable())) {
                            onCacheTapListener.onCacheTap(closest.getCoord());
                        } else {
                            toggleActionBar();
                        }
                    } else {
                        toggleActionBar();
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                                final float distanceX, final float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return false;
        }
    }

    private void toggleActionBar() {
        final AbstractBottomNavigationActivity activity = activityRef.get();
        if (activity != null) {
            adaptLayoutForActionbar(FilterUtils.toggleActionBar(activity));
        }
    }

    private void adaptLayoutForActionbar(final boolean actionBarShowing) {
        if (googleMap != null) {
            try {
                final View mapView = findViewById(R.id.map);
                final View compass = mapView.findViewWithTag("GoogleMapCompass");
                compass.animate().translationY((actionBarShowing ? mapView.getRootView().findViewById(R.id.actionBarSpacer).getHeight() : 0) + ViewUtils.dpToPixel(25)).start();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public boolean needsInvertedColors() {
        return false;
    }

    @Override
    public void onMapReady(final MapReadyCallback callback) {
        if (callback == null) {
            return;
        }
        if (googleMap == null) {
            if (mapReadyCallback != null) {
                Log.e("Can not register more than one mapReadyCallback, overriding the previous one");
            }
            mapReadyCallback = callback;
        } else {
            callback.mapReady();
        }
    }

    @Override
    public void updateItems(final Collection<GoogleCacheOverlayItem> itemsPre) {
        try {
            lock.lock();
            if (itemsPre != null) {
                this.cacheItems = itemsPre;
            }
            redraw();
        } finally {
            lock.unlock();
        }
    }

    public void setDistanceDrawer(final Geopoint destCoords) {
        this.distanceDrawer = new DistanceDrawer(root, destCoords, Settings.isBrouterShowBothDistances());
    }

    public GoogleCacheOverlayItem closest(final Geopoint geopoint) {
        if (cacheItems == null) {
            return null;
        }
        final int size = cacheItems.size();
        if (size == 0) {
            return null;
        }
        final Iterator<GoogleCacheOverlayItem> it = cacheItems.iterator();
        GoogleCacheOverlayItem closest = it.next();
        float closestDist = closest.getCoord().getCoords().distanceTo(geopoint);
        while (it.hasNext()) {
            final GoogleCacheOverlayItem next = it.next();
            final float dist = next.getCoord().getCoords().distanceTo(geopoint);
            if (dist < closestDist) {
                closest = next;
                closestDist = dist;
            }
        }
        return closest;
    }

    @Override
    protected void dispatchDraw(final Canvas canvas) {
        canvas.save();
        super.dispatchDraw(canvas);
        canvas.restore();
        // cannot be in draw(), would not work
        scaleDrawer.drawScale(canvas, this);
        if (distanceDrawer != null) {
            distanceDrawer.drawDistance(canvas);
        }
    }


    public void redraw() {
        if (cachesList == null || cacheItems == null) {
            return;
        }
        cachesList.redraw(cacheItems, showCircles);
    }


    @Override
    public boolean getCircles() {
        return showCircles;
    }

    @Override
    public void setCircles(final boolean showCircles) {
        this.showCircles = showCircles;
        redraw();
    }

    @Override
    public void setOnTapListener(final OnCacheTapListener listener) {
        onCacheTapListener = listener;
    }

    @Override
    public void selectMapTheme(final AppCompatActivity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(R.string.map_theme_select);

        final int selectedItem = GoogleMapsThemes.getByName(Settings.getSelectedGoogleMapTheme()).ordinal();

        builder.setSingleChoiceItems(GoogleMapsThemes.getLabels(activity).toArray(new String[0]), selectedItem, (dialog, selection) -> {
            final GoogleMapsThemes theme = GoogleMapsThemes.values()[selection];
            Settings.setSelectedGoogleMapTheme(theme.name());
            googleMap.setMapStyle(theme.getMapStyleOptions(activity));
            dialog.cancel();
        });

        builder.show();
    }

    public enum GoogleMapsThemes {
        DEFAULT(R.string.google_maps_style_default, 0),
        NIGHT(R.string.google_maps_style_night, R.raw.googlemap_style_night),
        AUTO(R.string.google_maps_style_auto, 0),
        RETRO(R.string.google_maps_style_retro, R.raw.googlemap_style_retro),
        CONTRAST(R.string.google_maps_style_contrast, R.raw.googlemap_style_contrast);

        final int labelRes;
        final int jsonRes;

        GoogleMapsThemes(final int labelRes, final int jsonRes) {
            this.labelRes = labelRes;
            this.jsonRes = jsonRes;
        }

        public MapStyleOptions getMapStyleOptions(final Context context) {
            final int jsonResId;
            if (this == AUTO) {
                jsonResId = Settings.isLightSkin(context) ? DEFAULT.jsonRes : NIGHT.jsonRes;
            } else {
                jsonResId = this.jsonRes;
            }
            if (jsonResId != 0) {
                return MapStyleOptions.loadRawResourceStyle(context, jsonResId);
            }
            return null;
        }

        public static List<String> getLabels(final Context context) {
            final List<String> themeLabels = new ArrayList<>();
            for (GoogleMapsThemes theme : GoogleMapsThemes.values()) {
                themeLabels.add(context.getResources().getString(theme.labelRes));
            }
            return themeLabels;
        }

        public static GoogleMapsThemes getByName(final String themeName) {
            for (GoogleMapsThemes theme : GoogleMapsThemes.values()) {
                if (theme.name().equals(themeName)) {
                    return theme;
                }
            }
            return DEFAULT;
        }
    }
}
