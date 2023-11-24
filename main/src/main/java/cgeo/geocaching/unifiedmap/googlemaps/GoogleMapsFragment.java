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
import org.oscim.core.BoundingBox;

public class GoogleMapsFragment extends AbstractMapFragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private final GoogleMapController mapController = new GoogleMapController();
    private GestureDetector gestureDetector;

    private LatLngBounds lastBounds;

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
        googleMap.getUiSettings().setZoomControlsEnabled(true);
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
        GoogleMapsThemeHelper.setTheme(requireActivity(), mMap);
        if (position != null) {
            setCenter(position);
        }
        setZoom(zoomLevel);
        mMap.setOnMarkerClickListener(marker -> true); // suppress default behavior (too slow & unwanted popup)
        ((TouchableWrapper) (requireView().findViewById(R.id.mapViewGMWrapper))).setOnTouch(gestureDetector::onTouchEvent);

//        googleMap.setOnMapClickListener(latLng -> {
//            if (activityRef.get() != null) {
//                final ActionBar actionBar = activityRef.get().getSupportActionBar();
//                if (actionBar != null) {
//                    adaptLayoutForActionbar(activityRef.get(), googleMap, actionBar.isShowing());
//                }
//            }
//        });
//        adaptLayoutForActionbar(activityRef.get(), googleMap, true);

        lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        mMap.setOnCameraMoveStartedListener(reason -> {
            lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds;

            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
                viewModel.followMyLocation.setValue(false);
            }
        });
        mMap.setOnCameraIdleListener(() -> {
            lastBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            viewModel.mapCenter.setValue(getCenter());
//            if (activityMapChangeListener != null) {
//                final CameraPosition pos = mMap.getCameraPosition();
//                activityMapChangeListener.call(new UnifiedMapPosition(pos.target.latitude, pos.target.longitude, (int) pos.zoom, pos.bearing));
//            }
        });

        mMap.setOnMapLongClickListener(latLng -> {
            final Point point = mMap.getProjection().toScreenLocation(latLng);
            onTapCallback((int) (latLng.latitude * 1E6), (int) (latLng.longitude * 1E6), point.x, point.y, true);
        });

        adaptLayoutForActionbar(true);


        initLayers();
        onMapReadyTasks.run();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMap != null) {
            initLayers();
        }
    }

    @Override
    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource instanceof AbstractGoogleTileProvider;
    }

    @Override
    public void setTileSource(final AbstractTileProvider newSource) {
        super.setTileSource(newSource);
        ((AbstractGoogleTileProvider) newSource).setMapType(mMap);
    }

    @Override
    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return new GoogleV2GeoItemLayer(mMap);
    }

    @Override
    public void setCenter(final Geopoint geopoint) {
        if (mMap != null) {
            mapController.animateTo(new GoogleGeoPoint(geopoint.getLatitudeE6(), geopoint.getLongitudeE6()));
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
    public BoundingBox getBoundingBox() {
        if (lastBounds == null) {
            return new BoundingBox(0, 0, 0, 0);
        }
        // mMap.getProjection() needs to be called on UI thread
        return new BoundingBox(lastBounds.southwest.latitude, lastBounds.southwest.longitude, lastBounds.northeast.latitude, lastBounds.northeast.longitude);
    }

    // ========================================================================
    // theme & language related methods

    @Override
    public void selectTheme(final Activity activity) {
        GoogleMapsThemeHelper.selectTheme(activity, mMap);
    }

    @Override
    public void applyTheme() {
        GoogleMapsThemeHelper.setTheme(requireActivity(), mMap);
    }

    // ========================================================================
    // zoom, bearing & heading methods

//    /** keep track of rotation and zoom level changes */
//    @Override
//    protected void configMapChangeListener(final boolean enable) {
//        if (mMap != null) {
//            mMap.setOnCameraIdleListener(null);
//            mMap.setOnCameraMoveStartedListener(null);
//            if (enable) {
//                mMap.setOnCameraIdleListener(() -> {
//                    if (activityMapChangeListener != null) {
//                        final CameraPosition pos = mMap.getCameraPosition();
//                        activityMapChangeListener.call(new UnifiedMapPosition(pos.target.latitude, pos.target.longitude, (int) pos.zoom, pos.bearing));
//                    }
//                });
//                mMap.setOnCameraMoveStartedListener(reason -> {
//                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && resetFollowMyLocationListener != null) {
//                        resetFollowMyLocationListener.run();
//                    }
//                });
//            }
//        }
//    }

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
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
        } else {
            this.zoomLevel = zoomLevel;
        }
    }

    @Override
    public void setMapRotation(final int mapRotation) {
        mMap.getUiSettings().setRotateGesturesEnabled(mapRotation == MAPROTATION_MANUAL);
        super.setMapRotation(mapRotation);
    }

    @Override
    public float getCurrentBearing() {
        return mMap.getCameraPosition().bearing;
    }

    @Override
    public void setBearing(final float bearing) {
        // @todo: it looks like we need to take current heading into account, otherwise the map is rotated into heading arrows direction when called with bearing=0
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(mMap.getCameraPosition()).bearing(AngleUtils.normalize(bearing)).build()));

    }

    @Override
    protected void adaptLayoutForActionbar(final boolean actionBarShowing) {
        if (mMap == null) {
            return;
        }

        final View compass = requireView().findViewWithTag("GoogleMapCompass");
        compass.animate().translationY((actionBarShowing ? requireActivity().findViewById(R.id.actionBarSpacer).getHeight() : 0) + ViewUtils.dpToPixel(25)).start();
    }

}
