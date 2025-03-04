package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.google.v2.GoogleGeoPoint;
import cgeo.geocaching.maps.google.v2.GoogleMapController;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.TouchableWrapper;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.unifiedmap.geoitemlayer.GoogleV2GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractGoogleTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class GoogleMapsFragment extends AbstractMapFragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private final GoogleMapController mapController = new GoogleMapController();
    private GestureDetector gestureDetector;

    private final ScaleDrawer scaleDrawer = new ScaleDrawer();
    private LatLngBounds lastBounds = null;

    private boolean mapIsCurrentlyMoving;

    public GoogleMapsFragment() {
        super(R.layout.unifiedmap_googlemaps_fragment);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gestureDetector = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(final @NonNull MotionEvent event) {
                final LatLng latLng = mMap.getProjection().fromScreenLocation(new Point((int) event.getX(), (int) event.getY()));
                onTapCallback((int) (latLng.latitude * 1E6), (int) (latLng.longitude * 1E6), (int) event.getX(), (int) event.getY(), false);
                return true;
            }

            @Override
            public void onLongPress(final @NonNull MotionEvent event) {
                if (!mapIsCurrentlyMoving) {
                    final LatLng latLng = mMap.getProjection().fromScreenLocation(new Point((int) event.getX(), (int) event.getY()));
                    onTapCallback((int) (latLng.latitude * 1E6), (int) (latLng.longitude * 1E6), (int) event.getX(), (int) event.getY(), true);
                }
            }
        });

        // add map fragment
        final SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.mapViewGM, mapFragment)
                .commit();

        // start map
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final @NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mapController.setGoogleMap(googleMap);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        setMapRotation(Settings.getMapRotation());
        onMapReadyCheckForActivity();
    }

    private void onMapReadyCheckForActivity() {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            onMapAndActivityReady();
        } else {
            // wait a bit until fragment is attached and activity becomes available
            new Handler(Looper.getMainLooper()).postDelayed(this::onMapReadyCheckForActivity, 100);
        }
    }

    private void onMapAndActivityReady() {
        applyTheme();
        if (position != null) {
            ViewUtils.runOnUiThread(true, () -> setCenter(position));
        }
        setZoom(zoomLevel);
        mMap.setOnMarkerClickListener(marker -> true); // suppress default behavior (too slow & unwanted popup)
        ((TouchableWrapper) (requireView().findViewById(R.id.mapViewGMWrapper))).setOnTouch(gestureDetector::onTouchEvent);

        lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        scaleDrawer.setImageView(requireActivity().findViewById(R.id.scale));

        mMap.setOnCameraMoveStartedListener(reason -> {
            mapIsCurrentlyMoving = true;
            lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            scaleDrawer.drawScale(lastBounds);
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
                viewModel.followMyLocation.setValue(false);
            }
        });
        mMap.setOnCameraMoveListener(() -> {
            repaintRotationIndicator(getCurrentBearing());
            ((UnifiedMapActivity) requireActivity()).notifyZoomLevel(mMap.getCameraPosition().zoom);
        });
        mMap.setOnCameraIdleListener(() -> {
            mapIsCurrentlyMoving = false;
            lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            scaleDrawer.drawScale(lastBounds);
        });

        adaptLayoutForActionBar(true);

        initLayers();
        onMapReadyTasks.run();
    }


    // ========================================================================
    // lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        if (mMap != null) {
            initLayers();
        }
    }


    // ========================================================================
    // tilesource handling

    @Override
    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource instanceof AbstractGoogleTileProvider;
    }

    @Override
    public boolean setTileSource(final AbstractTileProvider newSource, final boolean force) {
        final boolean needsUpdate = super.setTileSource(newSource, force);
        if (needsUpdate) {
            ((AbstractGoogleTileProvider) newSource).setMapType(mMap);
        }
        return needsUpdate;
    }


    // ========================================================================
    // layer handling

    @Override
    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return new GoogleV2GeoItemLayer(mMap);
    }


    // ========================================================================
    // position related methods

    @Override
    public void setCenter(final Geopoint geopoint) {
        this.position = geopoint;
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(geopoint.getLatitude(), geopoint.getLongitude())));
        }
    }

    @Override
    public Geopoint getCenter() {
        if (mMap != null) {
            final LatLng pos = mMap.getCameraPosition().target;
            return new Geopoint(pos.latitude, pos.longitude);
        }
        return new Geopoint(0.0d, 0.0d);
    }

    @Override
    @Nullable
    public Viewport getViewport() {
        if (lastBounds == null) {
            return null;
        }
        // mMap.getProjection() needs to be called on UI thread
        return new Viewport(lastBounds.southwest.latitude, lastBounds.southwest.longitude, lastBounds.northeast.latitude, lastBounds.northeast.longitude);
    }


    // ========================================================================
    // zoom, bearing & heading methods

    @Override
    public void zoomToBounds(final Viewport bounds) {
        if (mMap != null) {
            mapController.zoomToSpan((int) (bounds.getLatitudeSpan() * 1E6), (int) (bounds.getLongitudeSpan() * 1E6));
            mapController.animateTo(new GoogleGeoPoint(bounds.getCenter().getLatitudeE6(), bounds.getCenter().getLongitudeE6()));
        }
    }

    @Override
    public int getCurrentZoom() {
        try {
            return (int) mMap.getCameraPosition().zoom;
        } catch (Exception ignore) {
            return -1;
        }
    }

    @Override
    public void setZoom(final int zoomLevel) {
        this.zoomLevel = zoomLevel;
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
        }
    }

    @Override
    public void zoomInOut(final boolean zoomIn) {
        mMap.animateCamera(zoomIn ? CameraUpdateFactory.zoomIn() : CameraUpdateFactory.zoomOut());
    }

    @Override
    public void setMapRotation(final int mapRotation) {
        super.setMapRotation(mapRotation);
        mMap.getUiSettings().setRotateGesturesEnabled(mapRotation == MAPROTATION_MANUAL);
    }

    @Override
    public float getCurrentBearing() {
        return mMap.getCameraPosition().bearing;
    }

    @Override
    public void setBearing(final float bearing) {
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(mMap.getCameraPosition()).bearing(AngleUtils.normalize(bearing)).build()));
        }
    }


    // ========================================================================
    // theme & language related methods

    @Override
    public void selectThemeOptions(final Activity activity) {
        final int mapType = ((AbstractGoogleTileProvider) currentTileProvider).getMapType();
        GoogleMapsThemeHelper.selectThemeOptions(activity, mapType, mMap, scaleDrawer);
    }

    @Override
    public void selectTheme(final Activity activity) {
        GoogleMapsThemeHelper.selectTheme(activity, mMap, scaleDrawer);
    }

    @Override
    public void applyTheme() {
        GoogleMapsThemeHelper.setCurrentThemeOnMap(mMap, scaleDrawer);
    }


    // ========================================================================
    // additional menu entries

    // @Override
    // public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
    // }


    // ========================================================================
    // Tap handling methods

}
