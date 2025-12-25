// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractNavigationBarMapActivity
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.CGeoMap
import cgeo.geocaching.maps.DistanceDrawer
import cgeo.geocaching.maps.MapProviderFactory
import cgeo.geocaching.maps.ScaleDrawer
import cgeo.geocaching.maps.interfaces.GeneralOverlay
import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.maps.interfaces.MapControllerImpl
import cgeo.geocaching.maps.interfaces.MapProjectionImpl
import cgeo.geocaching.maps.interfaces.MapReadyCallback
import cgeo.geocaching.maps.interfaces.MapSource
import cgeo.geocaching.maps.interfaces.MapViewImpl
import cgeo.geocaching.maps.interfaces.OnCacheTapListener
import cgeo.geocaching.maps.interfaces.OnMapDragListener
import cgeo.geocaching.maps.interfaces.PositionAndHistory
import cgeo.geocaching.maps.mapsforge.AbstractMapsforgeMapSource
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.ActionBarUtils
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.maps.google.v2.GoogleMapUtils.isGoogleMapsAvailable
import cgeo.geocaching.storage.extension.OneTimeDialogs.DialogType.MAP_AUTOROTATION_DISABLE

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View

import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collection
import java.util.Iterator
import java.util.List
import java.util.Objects
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import javax.annotation.Nullable

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.VisibleRegion

class GoogleMapView : MapView() : MapViewImpl<GoogleCacheOverlayItem>, OnMapReadyCallback {

    private OnMapDragListener onDragListener
    private val mapController: GoogleMapController = GoogleMapController()
    private GoogleMap googleMap
    private MapReadyCallback mapReadyCallback

    private LatLng viewCenter
    private Float zoomLevel
    private VisibleRegion visibleRegion

    private GoogleCachesList cachesList
    private GestureDetector gestureDetector
    private Collection<GoogleCacheOverlayItem> cacheItems

    private OnCacheTapListener onCacheTapListener
    private var showCircles: Boolean = false
    private var canDisableAutoRotate: Boolean = false

    private val lock: Lock = ReentrantLock()

    private val scaleDrawer: ScaleDrawer = ScaleDrawer()
    private DistanceDrawer distanceDrawer

    private WeakReference<AbstractNavigationBarMapActivity> activityRef
    private WeakReference<PositionAndHistory> positionAndHistoryRef
    private var root: View = null
    private Marker coordsMarker

    private var fromList: Int = StoredList.TEMPORARY_LIST.id

    interface PostRealDistance {
        Unit postRealDistance(Float realDistance)
    }

    public GoogleMapView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        initialize(context)
    }

    public GoogleMapView(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        initialize(context)
    }

    override     // splitting-up method would not improve readability
    @SuppressWarnings("PMD.NPathComplexity")
    public Unit onMapReady(final GoogleMap googleMap) {
        if (this.googleMap != null) {
            if (this.googleMap == googleMap) {
                return
            } else {
                throw IllegalStateException("Could not set google map - already set")
            }
        }
        this.googleMap = googleMap
        mapController.setGoogleMap(googleMap)

        val theme: GoogleMapsThemes = GoogleMapsThemes.getByName(Settings.getSelectedGoogleMapTheme())
        if (theme.isInternalColorScheme) {
            googleMap.setMapColorScheme(theme.jsonRes)
        } else {
            googleMap.setMapStyle(theme.getMapStyleOptions(getContext()))
        }

        cachesList = GoogleCachesList(googleMap)
        googleMap.setOnCameraMoveListener(this::recognizePositionChange)
        googleMap.setOnCameraIdleListener(this::recognizePositionChange)
        googleMap.setOnMapClickListener(latLng -> {
            if (activityRef.get() != null) {
                if (activityRef.get().sheetRemoveFragment()) {
                    return
                }
                adaptLayoutForActionbar(ActionBarUtils.toggleActionBar(activityRef.get()))
            }
        })
        googleMap.setOnMarkerClickListener(marker -> {
            // onCacheTapListener will fire on onSingleTapUp event, not here, because this event
            // is fired 300 ms after map tap, which is too slow for UI

            // suppress default behaviour (yeah, true == suppress)
            // ("The default behavior is for the camera to move to the marker and an info window to appear.")
            return true
        })
        if (Settings.isLongTapOnMapActivated()) {
            googleMap.setOnMapLongClickListener(tapLatLong -> {
                val tappedPoint: Point = googleMap.getProjection().toScreenLocation(tapLatLong)
                if (!isLongTappedOnGeoItem(tapLatLong, null) && null != positionAndHistoryRef) {
                    val positionAndHistory: GooglePositionAndHistory = (GooglePositionAndHistory) positionAndHistoryRef.get()
                    if (null != positionAndHistory) {
                        for (RouteItem item : positionAndHistory.individualRoutePoints) {
                            val itemPoint: Point = googleMap.getProjection().toScreenLocation(LatLng(item.getPoint().getLatitude(), item.getPoint().getLongitude()))
                            if (Math.abs(itemPoint.x - tappedPoint.x) < ViewUtils.dpToPixel(8) && Math.abs(itemPoint.y - tappedPoint.y) < ViewUtils.dpToPixel(8)) {
                                ((CGeoMap) onCacheTapListener).toggleRouteItem(item.getPoint())
                                return
                            }
                        }
                        positionAndHistory.setLongTapLatLng(tapLatLong)
                        ((CGeoMap) onCacheTapListener).triggerLongTapContextMenu(tappedPoint)
                    }
                }
            })
            // GM renderer needs to catch Long tap by catching drag events
            googleMap.setOnMarkerDragListener(GoogleMap.OnMarkerDragListener() {
                override                 public Unit onMarkerDrag(final Marker marker) {
                    restorePosition(marker)
                }

                override                 public Unit onMarkerDragEnd(final Marker marker) {
                    restorePosition(marker)
                }

                override                 public Unit onMarkerDragStart(final Marker marker) {
                    isLongTappedOnGeoItem(marker.getPosition(), marker)
                    restorePosition(marker)
                }

                private Unit restorePosition(final Marker marker) {
                    // keep original position
                    val oldPosition: INamedGeoCoordinate = (INamedGeoCoordinate) marker.getTag()
                    if (oldPosition != null) {
                        marker.setPosition(LatLng(oldPosition.getCoords().getLatitude(), oldPosition.getCoords().getLongitude()))
                    }
                }
            })
        }
        adaptLayoutForActionbar(true)
        googleMap.setOnCameraChangeListener(cameraPosition -> {
            // check for tap on compass rose, which resets bearing to 0.0
            // only active, if it has been not equal to 0.0 before
            val bearing: Float = cameraPosition.bearing
            if (canDisableAutoRotate && bearing == 0.0f && (Settings.getMapRotation() == Settings.MAPROTATION_AUTO_LOWPOWER || Settings.getMapRotation() == Settings.MAPROTATION_AUTO_PRECISE)) {
                canDisableAutoRotate = false
                val context: Context = getContext()
                Dialogs.advancedOneTimeMessage(context, MAP_AUTOROTATION_DISABLE, context.getString(MAP_AUTOROTATION_DISABLE.messageTitle), context.getString(MAP_AUTOROTATION_DISABLE.messageText), "", true, null, () -> {
                    Settings.setMapRotation(Settings.MAPROTATION_MANUAL)

                    // notify overlay
                    if (null != positionAndHistoryRef) {
                        val positionAndHistory: PositionAndHistory = positionAndHistoryRef.get()
                        if (null != positionAndHistory) {
                            positionAndHistory.updateMapRotation()
                        }
                    }
                })
            } else if (bearing != 0.0f) {
                canDisableAutoRotate = true
            }
            Log.d("bearing=" + cameraPosition.bearing + ", tilt=" + cameraPosition.tilt + ", canDisable=" + canDisableAutoRotate)
        })
        if (mapReadyCallback != null) {
            mapReadyCallback.mapReady()
            mapReadyCallback = null
        }

        redraw()
    }

    private Boolean isLongTappedOnGeoItem(final LatLng tapLocation, final Marker marker) {
        val closest: GoogleCacheOverlayItem = closest(Geopoint(tapLocation.latitude, tapLocation.longitude))
        val tappedPoint: Point = googleMap.getProjection().toScreenLocation(tapLocation)
        if (closest != null) {
            if (marker != null) {
                marker.setTag(closest.getCoord())
            }
            val waypointPoint: Point = googleMap.getProjection().toScreenLocation(LatLng(closest.getCoord().getCoords().getLatitude(), closest.getCoord().getCoords().getLongitude()))
            if (insideCachePointDrawable(tappedPoint, waypointPoint, closest.getMarker(0).getDrawable())) {
                ((CGeoMap) onCacheTapListener).handleCacheWaypointLongTap(closest.getCoord(), waypointPoint.x, waypointPoint.y)
                return true
            }
        }
        return false
    }

    private Unit adaptLayoutForActionbar(final Boolean actionBarShowing) {
        val activity: AppCompatActivity = activityRef.get()
        if (activity != null && googleMap != null) {
            try {
                val mapView: View = findViewById(R.id.map)
                val compass: View = mapView.findViewWithTag("GoogleMapCompass")
                adaptLayoutForActionBarHelper(activity, actionBarShowing, compass)
            } catch (Exception ignore) {
            }
        }
    }

    private static Unit adaptLayoutForActionBarHelper(final AppCompatActivity activity, @androidx.annotation.Nullable final Boolean actionBarShowing, @androidx.annotation.Nullable final View compassRose) {
        if (compassRose == null) {
            return
        }

        Int minHeight = 0

        Boolean abs = actionBarShowing
        if (actionBarShowing == null) {
            val actionBar: ActionBar = activity.getSupportActionBar()
            abs = actionBar != null && actionBar.isShowing()
        }
        if (abs) {
            minHeight = activity.findViewById(R.id.actionBarSpacer).getHeight()
        }

        val filterbar: View = activity.findViewById(R.id.filter_bar)
        if (filterbar != null) {
            minHeight += filterbar.getHeight()
        }

        View v = activity.findViewById(R.id.distanceinfo)
        if (v.getVisibility() != View.VISIBLE) {
            v = activity.findViewById(R.id.target)
        }
        if (v.getVisibility() == View.VISIBLE) {
            minHeight += v.getHeight()
        }

        val finalMinHeight: Int = minHeight
        activity.runOnUiThread(() -> compassRose.animate().translationY(finalMinHeight).start())
    }


    override     public Unit setCoordsMarker(final Geopoint coords) {
        if (coordsMarker != null) {
            coordsMarker.remove()
            coordsMarker = null
        }
        if (coords != null && coords.isValid()) {
            coordsMarker = googleMap.addMarker(MarkerOptions()
                    .position(LatLng(coords.getLatitude(), coords.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromBitmap(Objects.requireNonNull(ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(getResources(), R.drawable.coords_indicator, null)))))
            )
        }
    }

    override     public Unit setListId(final Int listId) {
        fromList = listId
    }

    private Unit recognizePositionChange() {
        val cameraPosition: CameraPosition = googleMap.getCameraPosition()
        // update all variable, which getters are available only in main thread
        viewCenter = cameraPosition.target
        zoomLevel = cameraPosition.zoom
        val newVisibleRegion: VisibleRegion = googleMap.getProjection().getVisibleRegion()
        if (newVisibleRegion != null) {
            visibleRegion = newVisibleRegion
        }
        invalidate(); // force redraw to draw scale
    }

    private Unit initialize(final Context context) {
        if (isInEditMode()) {
            return
        }

        activityRef = WeakReference<>((AbstractNavigationBarMapActivity) context)

        if (!isGoogleMapsAvailable(context)) {
            // either play services are missing (should have been caught in MapProviderFactory) or Play Services version does not support this Google Maps API version
            SimpleDialog.of((Activity) context).setTitle(R.string.warn_gm_not_available).setMessage(R.string.switch_to_mf).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(() -> {
                // switch to first Mapsforge mapsource found
                val mapSources: Collection<MapSource> = MapProviderFactory.getMapSources()
                for (final MapSource mapSource : mapSources) {
                    if (mapSource is AbstractMapsforgeMapSource) {
                        Settings.setMapSource(mapSource)
                        SimpleDialog.of((Activity) context).setTitle(R.string.warn_gm_not_available).setMessage(R.string.switched_to_mf).show(() -> ((Activity) context).finish())
                        break
                    }
                }
            })
        }

        getMapAsync(this)
        gestureDetector = GestureDetector(context, GestureListener((AbstractNavigationBarMapActivity) context))
    }


    override     public Unit setBuiltInZoomControls(final Boolean b) {
        if (googleMap == null) {
            return
        }
        googleMap.getUiSettings().setZoomControlsEnabled(b)
    }

    override     public Unit zoomInOut(final Boolean zoomIn) {
        googleMap.animateCamera(zoomIn ? CameraUpdateFactory.zoomIn() : CameraUpdateFactory.zoomOut())
    }

    override     public MapControllerImpl getMapController() {
        return mapController
    }

    override     public GeoPointImpl getMapViewCenter() {
        if (viewCenter == null) {
            return null
        }
        return GoogleGeoPoint(viewCenter)
    }

    override     public Int getLatitudeSpan() {
        if (visibleRegion == null) {
            return -1
        }
        return (Int) (Math.abs(visibleRegion.latLngBounds.northeast.latitude - visibleRegion.latLngBounds.southwest.latitude) * 1e6)
    }

    override     public Int getLongitudeSpan() {
        if (visibleRegion == null) {
            return -1
        }
        return (Int) (Math.abs(visibleRegion.latLngBounds.northeast.longitude - visibleRegion.latLngBounds.southwest.longitude) * 1e6)
    }

    override     public Viewport getViewport() {
        if (visibleRegion == null) {
            return null
        }
        return Viewport(GoogleGeoPoint(visibleRegion.farLeft), GoogleGeoPoint(visibleRegion.nearRight))
    }

    override     public Unit clearOverlays() {
        // do nothing, there are no overlays to be cleared
    }

    override     public MapProjectionImpl getMapProjection() {
        if (googleMap == null) {
            return null
        }
        return GoogleMapProjection(googleMap.getProjection())
    }

    override     public PositionAndHistory createAddPositionAndScaleOverlay(final View root, final Geopoint coords, final String geocode) {
        this.root = root
        if (googleMap == null) {
            throw IllegalStateException("Google map not initialized yet"); // TODO check
        }
        val ovl: GoogleOverlay = GoogleOverlay(googleMap, this, realDistance -> {
            if (distanceDrawer != null) {
                distanceDrawer.setRealDistance(realDistance)
                this.invalidate()
            }
        }, routeDistance -> {
            if (distanceDrawer != null) {
                distanceDrawer.setRouteDistance(routeDistance)
                this.invalidate()
            }
        })
        setDestinationCoords(coords)
        positionAndHistoryRef = WeakReference<>(ovl.getBase())
        return positionAndHistoryRef.get()
    }

    override     public Int getMapZoomLevel() {
        return googleMap != null ? (Int) zoomLevel : -1
    }

    override     public Unit zoomToBounds(final Viewport bounds, final GeoPointImpl center) {
        mapController.zoomToSpan(bounds.topRight.getLatitudeE6() - bounds.bottomLeft.getLatitudeE6(), bounds.topRight.getLongitudeE6() - bounds.bottomLeft.getLongitudeE6())
        mapController.animateTo(center)
    }

    override     public Unit setMapSource() {
        if (googleMap == null) {
            return
        }
        val mapSource: MapSource = Settings.getMapSource()
        if (mapSource is GoogleMapProvider.AbstractGoogleMapSource) {
            final GoogleMapProvider.AbstractGoogleMapSource gMapSource = (GoogleMapProvider.AbstractGoogleMapSource) mapSource
            googleMap.setMapType(gMapSource.mapType)
            googleMap.setIndoorEnabled(gMapSource.indoorEnabled)
        } else {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL)
            googleMap.setIndoorEnabled(true)
        }
    }

    override     public Unit repaintRequired(final GeneralOverlay overlay) {
        // FIXME add recheck/readd markers and overlay
        if (null != positionAndHistoryRef) {
            val positionAndHistory: PositionAndHistory = positionAndHistoryRef.get()
            if (null != positionAndHistory) {
                positionAndHistory.repaintRequired()
            }
        }
    }

    override     public Unit setOnDragListener(final OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener
    }

    override     public Boolean dispatchTouchEvent(final MotionEvent ev) {
        // onTouchEvent is not working for Google's MapView
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override     public Unit setDestinationCoords(final Geopoint destCoords) {
        setDistanceDrawer(destCoords)
    }

    /**
     * needed to provide current coordinates for distanceDrawer
     * called only in GooglePositionAndHistory
     */
    override     public Unit setCoordinates(final Location coordinates) {
        if (distanceDrawer != null) {
            distanceDrawer.setCoordinates(coordinates)
        }
    }

    public Geopoint getDestinationCoords() {
        if (distanceDrawer != null) {
            return distanceDrawer.getDestinationCoords()
        } else {
            return null
        }
    }

    public Float getBearing() {
        // even thought google map support rotation, if the marker for current position is set as
        // flat (.flat(true)), the rotation is relative to map north, not view top, so the correct
        // value to be returned in this method is 0. TODO?
        return 0
    }

    /**
     * can be made static if nonstatic inner clases could have static methods
     */
    private Boolean insideCachePointDrawable(final Point p, final Point drawP, final Drawable d) {
        val width: Int = d.getIntrinsicWidth()
        val height: Int = d.getIntrinsicHeight()
        val diffX: Int = p.x - drawP.x
        val diffY: Int = p.y - drawP.y
        // assume drawable is drawn above drawP
        return
                Math.abs(diffX) < width / 2 &&
                        diffY > -height && diffY < 0

    }

    private class GestureListener : SimpleOnGestureListener() {

        private static val GOOGLEMAP_ZOOMIN_BUTTON: String = "GoogleMapZoomInButton"
        private static val GOOGLEMAP_ZOOMOUT_BUTTON: String = "GoogleMapZoomOutButton"
        private static val GOOGLEMAP_COMPASS: String = "GoogleMapCompass"

        private final WeakReference<AbstractNavigationBarMapActivity> activityRef

        GestureListener(final AbstractNavigationBarMapActivity activity) {
            super()
            this.activityRef = WeakReference<>(activity)
        }

        override         public Boolean onDoubleTap(final MotionEvent e) {
            // no need to move to location, google maps will do it for us
            if (onDragListener != null) {
                onDragListener.onDrag()
            }
            return false
        }

        override         public Boolean onSingleTapUp(final MotionEvent e) {
            // is map already initialized?
            if (googleMap != null) {
                val p: Point = Point((Int) e.getX(), (Int) e.getY())

                // check for zoom controls and compass rose
                final Int[] mapLocation = Int[2]
                val vMap: View = findViewById(R.id.map)
                vMap.getLocationOnScreen(mapLocation)

                if (isHit(p.x + mapLocation[0], p.y + mapLocation[1], findViewWithTag(GOOGLEMAP_ZOOMIN_BUTTON))
                 || isHit(p.x + mapLocation[0], p.y + mapLocation[1], findViewWithTag(GOOGLEMAP_ZOOMOUT_BUTTON))
                 || isHit(p.x + mapLocation[0], p.y + mapLocation[1], findViewWithTag(GOOGLEMAP_COMPASS))
                ) {
                    return false
                }

                // hit something else
                val latLng: LatLng = googleMap.getProjection().fromScreenLocation(p)
                if (latLng != null && onCacheTapListener != null) {
                    val closest: GoogleCacheOverlayItem = closest(Geopoint(latLng.latitude, latLng.longitude))
                    if (closest != null) {
                        val waypointPoint: Point = googleMap.getProjection().toScreenLocation(LatLng(closest.getCoord().getCoords().getLatitude(), closest.getCoord().getCoords().getLongitude()))
                        if (insideCachePointDrawable(p, waypointPoint, closest.getMarker(0).getDrawable())) {
                            onCacheTapListener.onCacheTap(closest.getCoord())
                        }
                    }
                }
            }
            return false
        }

        private Boolean isHit(final Int x, final Int y, final View v) {
            if (v == null) {
                return false
            }
            final Int[] location = Int[2]
            v.getLocationOnScreen(location)
            return (x >= location[0]) && (x <= location[0] + v.getWidth()) && (y >= location[1]) && (y <= location[1] + v.getHeight())
        }

        override         public Boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                                final Float distanceX, final Float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag()
            }
            return false
        }
    }

    override     public Boolean needsInvertedColors() {
        return false
    }

    override     public Unit onMapReady(final MapReadyCallback callback) {
        if (callback == null) {
            return
        }
        if (googleMap == null) {
            if (mapReadyCallback != null) {
                Log.e("Can not register more than one mapReadyCallback, overriding the previous one")
            }
            mapReadyCallback = callback
        } else {
            callback.mapReady()
        }
    }

    override     public Unit updateItems(final Collection<GoogleCacheOverlayItem> itemsPre) {
        try {
            lock.lock()
            if (itemsPre != null) {
                this.cacheItems = itemsPre
            }
            redraw()
        } finally {
            lock.unlock()
        }
    }

    public Unit setDistanceDrawer(final Geopoint destCoords) {
        this.distanceDrawer = DistanceDrawer(root, destCoords, Settings.isBrouterShowBothDistances(), () -> adaptLayoutForActionbar(null))
    }

    public GoogleCacheOverlayItem closest(final Geopoint geopoint) {
        if (cacheItems == null) {
            return null
        }
        val size: Int = cacheItems.size()
        if (size == 0) {
            return null
        }
        val it: Iterator<GoogleCacheOverlayItem> = cacheItems.iterator()
        GoogleCacheOverlayItem closest = it.next()
        Float closestDist = closest.getCoord().getCoords().distanceTo(geopoint)
        while (it.hasNext()) {
            val next: GoogleCacheOverlayItem = it.next()
            val dist: Float = next.getCoord().getCoords().distanceTo(geopoint)
            if (dist < closestDist) {
                closest = next
                closestDist = dist
            }
        }
        return closest
    }

    override     protected Unit dispatchDraw(final Canvas canvas) {
        canvas.save()
        super.dispatchDraw(canvas)
        canvas.restore()
        // cannot be in draw(), would not work
        scaleDrawer.drawScale(canvas, this)
        if (distanceDrawer != null) {
            distanceDrawer.drawDistance()
        }
    }


    public Unit redraw() {
        if (cachesList == null || cacheItems == null) {
            return
        }
        cachesList.redraw(cacheItems, showCircles)
    }


    override     public Boolean getCircles() {
        return showCircles
    }

    override     public Unit setCircles(final Boolean showCircles) {
        this.showCircles = showCircles
        redraw()
    }

    override     public Unit setOnTapListener(final OnCacheTapListener listener) {
        onCacheTapListener = listener
    }

    override     public Unit selectMapTheme(final AppCompatActivity activity) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(R.string.map_theme_select)

        val selectedItem: Int = GoogleMapsThemes.getByName(Settings.getSelectedGoogleMapTheme()).ordinal()

        builder.setSingleChoiceItems(GoogleMapsThemes.getLabels(activity).toArray(String[0]), selectedItem, (dialog, selection) -> {
            val theme: GoogleMapsThemes = GoogleMapsThemes.values()[selection]
            Settings.setSelectedGoogleMapTheme(theme.name())
            if (theme.isInternalColorScheme) {
                googleMap.setMapColorScheme(theme.jsonRes)
            } else {
                googleMap.setMapStyle(theme.getMapStyleOptions(activity))
            }
            dialog.cancel()
        })

        builder.show()
    }

    enum class class GoogleMapsThemes {
        DEFAULT(R.string.google_maps_style_default, MapColorScheme.LIGHT, true),
        NIGHT(R.string.google_maps_style_night, MapColorScheme.DARK, true),
        AUTO(R.string.google_maps_style_auto, MapColorScheme.FOLLOW_SYSTEM, true),
        CLASSIC(R.string.google_maps_style_classic, R.raw.googlemap_style_classic, false),
        RETRO(R.string.google_maps_style_retro, R.raw.googlemap_style_retro, false),
        CONTRAST(R.string.google_maps_style_contrast, R.raw.googlemap_style_contrast, false)

        final Int labelRes
        final Int jsonRes
        final Boolean isInternalColorScheme

        GoogleMapsThemes(final Int labelRes, final Int jsonRes, final Boolean isInternalColorScheme) {
            this.labelRes = labelRes
            this.jsonRes = jsonRes
            this.isInternalColorScheme = isInternalColorScheme
        }

        public MapStyleOptions getMapStyleOptions(final Context context) {
            return MapStyleOptions.loadRawResourceStyle(context, this.jsonRes)
        }

        public static List<String> getLabels(final Context context) {
            val themeLabels: List<String> = ArrayList<>()
            for (GoogleMapsThemes theme : GoogleMapsThemes.values()) {
                themeLabels.add(context.getResources().getString(theme.labelRes))
            }
            return themeLabels
        }

        public static GoogleMapsThemes getByName(final String themeName) {
            for (GoogleMapsThemes theme : GoogleMapsThemes.values()) {
                if (theme.name() == (themeName)) {
                    return theme
                }
            }
            return DEFAULT
        }
    }
}
