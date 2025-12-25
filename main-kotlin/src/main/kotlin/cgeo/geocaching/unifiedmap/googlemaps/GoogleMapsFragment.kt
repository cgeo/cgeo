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

package cgeo.geocaching.unifiedmap.googlemaps

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.google.v2.GoogleGeoPoint
import cgeo.geocaching.maps.google.v2.GoogleMapController
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.TouchableWrapper
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.unifiedmap.AbstractMapFragment
import cgeo.geocaching.unifiedmap.UnifiedMapActivity
import cgeo.geocaching.unifiedmap.geoitemlayer.GoogleV2GeoItemLayer
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer
import cgeo.geocaching.unifiedmap.tileproviders.AbstractGoogleTileProvider
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL

import android.app.Activity
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.FragmentActivity

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

class GoogleMapsFragment : AbstractMapFragment() : OnMapReadyCallback {
    private GoogleMap mMap
    private val mapController: GoogleMapController = GoogleMapController()
    private GestureDetector gestureDetector

    private val scaleDrawer: ScaleDrawer = ScaleDrawer()
    private var lastBounds: LatLngBounds = null

    private Boolean mapIsCurrentlyMoving

    public GoogleMapsFragment() {
        super(R.layout.unifiedmap_googlemaps_fragment)
    }

    override     public Unit onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState)

        gestureDetector = GestureDetector(this.getContext(), GestureDetector.SimpleOnGestureListener() {
            override             public Boolean onSingleTapUp(final MotionEvent event) {
                val latLng: LatLng = mMap.getProjection().fromScreenLocation(Point((Int) event.getX(), (Int) event.getY()))
                onTapCallback((Int) (latLng.latitude * 1E6), (Int) (latLng.longitude * 1E6), (Int) event.getRawX(), (Int) event.getRawY(), false)
                return true
            }

            override             public Unit onLongPress(final MotionEvent event) {
                if (!mapIsCurrentlyMoving) {
                    val latLng: LatLng = mMap.getProjection().fromScreenLocation(Point((Int) event.getX(), (Int) event.getY()))
                    onTapCallback((Int) (latLng.latitude * 1E6), (Int) (latLng.longitude * 1E6), (Int) event.getRawX(), (Int) event.getRawY(), true)
                }
            }
        })

        // add map fragment
        val mapFragment: SupportMapFragment = SupportMapFragment.newInstance()
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.mapViewGM, mapFragment)
                .commit()

        // start map
        mapFragment.getMapAsync(this)
    }

    override     public Unit onMapReady(final GoogleMap googleMap) {
        mMap = googleMap

        mapController.setGoogleMap(googleMap)
        googleMap.getUiSettings().setZoomControlsEnabled(false)
        googleMap.getUiSettings().setCompassEnabled(false)
        setMapRotation(Settings.getMapRotation())
        onMapReadyCheckForActivity()
    }

    private Unit onMapReadyCheckForActivity() {
        val activity: FragmentActivity = getActivity()
        if (activity != null) {
            onMapAndActivityReady()
        } else {
            // wait a bit until fragment is attached and activity becomes available
            Handler(Looper.getMainLooper()).postDelayed(this::onMapReadyCheckForActivity, 100)
        }
    }

    private Unit onMapAndActivityReady() {
        applyTheme()
        if (position != null) {
            ViewUtils.runOnUiThread(true, () -> setCenter(position))
        }
        setZoom(zoomLevel)
        mMap.setOnMarkerClickListener(marker -> true); // suppress default behavior (too slow & unwanted popup)
        ((TouchableWrapper) (requireView().findViewById(R.id.mapViewGMWrapper))).setOnTouch(gestureDetector::onTouchEvent)

        lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds
        scaleDrawer.setImageView(requireActivity().findViewById(R.id.scale))

        mMap.setOnCameraMoveStartedListener(reason -> {
            mapIsCurrentlyMoving = true
            lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds
            scaleDrawer.drawScale(lastBounds)
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && Boolean.TRUE == (viewModel.followMyLocation.getValue())) {
                viewModel.followMyLocation.setValue(false)
            }
        })
        mMap.setOnCameraMoveListener(() -> {
            repaintRotationIndicator(getCurrentBearing())
            ActivityMixin.requireActivity(getActivity(), activity -> ((UnifiedMapActivity) activity).notifyZoomLevel(mMap.getCameraPosition().zoom))
        })
        mMap.setOnCameraIdleListener(() -> {
            mapIsCurrentlyMoving = false
            lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds
            scaleDrawer.drawScale(lastBounds)
        })

        initLayers()
        onMapReadyTasks.run()
    }


    // ========================================================================
    // lifecycle methods

    override     public Unit onStart() {
        super.onStart()
        if (mMap != null) {
            initLayers()
        }
    }


    // ========================================================================
    // tilesource handling

    override     public Boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource is AbstractGoogleTileProvider
    }

    override     public Boolean setTileSource(final AbstractTileProvider newSource, final Boolean force) {
        val needsUpdate: Boolean = super.setTileSource(newSource, force)
        if (needsUpdate) {
            ((AbstractGoogleTileProvider) newSource).setMapType(mMap)
        }
        return needsUpdate
    }


    // ========================================================================
    // layer handling

    override     public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return GoogleV2GeoItemLayer(mMap)
    }


    // ========================================================================
    // position related methods

    override     public Unit setCenter(final Geopoint geopoint) {
        this.position = geopoint
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(geopoint.getLatitude(), geopoint.getLongitude())))
        }
    }

    override     public Geopoint getCenter() {
        if (mMap != null) {
            val pos: LatLng = mMap.getCameraPosition().target
            return Geopoint(pos.latitude, pos.longitude)
        }
        return Geopoint(0.0d, 0.0d)
    }

    override     public Viewport getViewport() {
        if (lastBounds == null) {
            return null
        }
        // mMap.getProjection() needs to be called on UI thread
        return Viewport(lastBounds.southwest.latitude, lastBounds.southwest.longitude, lastBounds.northeast.latitude, lastBounds.northeast.longitude)
    }


    // ========================================================================
    // zoom, bearing & heading methods

    override     public Unit zoomToBounds(final Viewport bounds) {
        if (mMap != null) {
            mapController.zoomToSpan((Int) (bounds.getLatitudeSpan() * 1E6), (Int) (bounds.getLongitudeSpan() * 1E6))
            mapController.animateTo(GoogleGeoPoint(bounds.getCenter().getLatitudeE6(), bounds.getCenter().getLongitudeE6()))
        }
    }

    override     public Int getCurrentZoom() {
        try {
            return (Int) mMap.getCameraPosition().zoom
        } catch (Exception ignore) {
            return -1
        }
    }

    override     public Unit setZoom(final Int zoomLevel) {
        this.zoomLevel = zoomLevel
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel))
        }
    }

    override     public Unit zoomInOut(final Boolean zoomIn) {
        mMap.animateCamera(zoomIn ? CameraUpdateFactory.zoomIn() : CameraUpdateFactory.zoomOut())
    }

    override     public Unit setMapRotation(final Int mapRotation) {
        super.setMapRotation(mapRotation)
        mMap.getUiSettings().setRotateGesturesEnabled(mapRotation == MAPROTATION_MANUAL)
    }

    override     public Float getCurrentBearing() {
        return mMap.getCameraPosition().bearing
    }

    override     public Unit setBearing(final Float bearing) {
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(mMap.getCameraPosition()).bearing(AngleUtils.normalize(bearing)).build()))
        }
    }


    // ========================================================================
    // theme & language related methods

    override     public Unit selectThemeOptions(final Activity activity) {
        val mapType: Int = ((AbstractGoogleTileProvider) currentTileProvider).getMapType()
        GoogleMapsThemeHelper.selectThemeOptions(activity, mapType, mMap, scaleDrawer)
    }

    override     public Unit selectTheme(final Activity activity) {
        GoogleMapsThemeHelper.selectTheme(activity, mMap, scaleDrawer)
    }

    override     public Unit applyTheme() {
        GoogleMapsThemeHelper.setCurrentThemeOnMap(mMap, scaleDrawer)
    }


    // ========================================================================
    // additional menu entries

    // override     // public Boolean onOptionsItemSelected(final MenuItem item) {
    // }


    // ========================================================================
    // Tap handling methods

}
