package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.google.v2.GoogleGeoPoint;
import cgeo.geocaching.maps.google.v2.GoogleMapController;
import cgeo.geocaching.unifiedmap.AbstractGeoitemLayer;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.AbstractUnifiedMapView;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.unifiedmap.UnifiedMapPosition;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractGoogleTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import org.oscim.core.BoundingBox;

/**
 * GoogleMapsView - Contains the view handling parts specific to Google Maps
 * To be called by UnifiedMapActivity (mostly)
 */
public class GoogleMapsView extends AbstractUnifiedMapView<LatLng> implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private View rootView;
    private final GoogleMapController mapController = new GoogleMapController();

    @Override
    public void init(final UnifiedMapActivity activity, final int delayedZoomTo, final Geopoint delayedCenterTo, final Runnable onMapReadyTasks) {
        super.init(activity, delayedZoomTo, delayedCenterTo, onMapReadyTasks);
        activity.setContentView(R.layout.unifiedmap_googlemaps);
        rootView = activity.findViewById(R.id.unifiedmap_gm);

        // add map fragment
        final SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.mapViewGM, mapFragment)
                .commit();

        // start map
        mapFragment.getMapAsync(this);
    }

    @Override
    public void prepareForTileSourceChange() {
        super.prepareForTileSourceChange();
    }


    /* retrieve fingerprint with getKeyHash(activity, "SHA")
    private String getKeyHash(final Activity activity, final String hashStrategy) {
        final char[] hexChars = "0123456789ABCDEF".toCharArray();
        final PackageInfo info;
        try {
            info = activity.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                final MessageDigest md = MessageDigest.getInstance(hashStrategy);
                md.update(signature.toByteArray());
                final StringBuilder sb = new StringBuilder();
                for (byte c : md.digest()) {
                    sb.append(hexChars[Byte.toUnsignedInt(c) / 16]).append(hexChars[c & 15]).append(' ');
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return e.toString();
        }
        return null;
    }
    */

    @Override
    public void onMapReady(@NonNull final GoogleMap googleMap) {
        mMap = googleMap;
        mapController.setGoogleMap(googleMap);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        configMapChangeListener(true);
        setMapRotation(mapRotation);
        positionLayer = configPositionLayer(true);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        onMapReadyTasks.run();
    }

    @Override
    public void onMapClick(final LatLng point) {
        onTapCallback(point.latitude, point.longitude, false);
    }

    @Override
    public void onMapLongClick(final LatLng point) {
        onTapCallback(point.latitude, point.longitude, true);
    }

    @Override
    protected AbstractGeoitemLayer createGeoitemLayers(final AbstractTileProvider tileProvider) {
        return new GoogleGeoitemLayer(mMap);
    }

    @Override
    public void setTileSource(final AbstractTileProvider newSource) {
        super.setTileSource(newSource);
        ((AbstractGoogleTileProvider) newSource).setMapType(mMap);
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

    /**
     * keep track of rotation and zoom level changes
     **/
    protected void configMapChangeListener(final boolean enable) {
        if (mMap != null) {
            mMap.setOnCameraIdleListener(null);
            mMap.setOnCameraMoveStartedListener(null);
            if (enable) {
                mMap.setOnCameraIdleListener(() -> {
                    if (activityMapChangeListener != null) {
                        final CameraPosition pos = mMap.getCameraPosition();
                        activityMapChangeListener.call(new UnifiedMapPosition(pos.target.latitude, pos.target.longitude, (int) pos.zoom, pos.bearing));
                    }
                });
                mMap.setOnCameraMoveStartedListener(reason -> {
                    if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && resetFollowMyLocationListener != null) {
                        resetFollowMyLocationListener.run();
                    }
                });
            }
        }
    }

    @Override
    public void applyTheme() {
        // @todo
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
        final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        return new BoundingBox(bounds.southwest.latitude, bounds.southwest.longitude, bounds.northeast.latitude, bounds.northeast.longitude);
    }

    // ========================================================================
    // zoom & heading methods

    @Override
    public void zoomToBounds(final Viewport bounds) {
        if (mMap != null) {
            mapController.zoomToSpan((int) (bounds.getLatitudeSpan() * 1E6), (int) (bounds.getLongitudeSpan() * 1E6));
            mapController.animateTo(new GoogleGeoPoint(bounds.getCenter().getLatitudeE6(), bounds.getCenter().getLongitudeE6()));
        }
    }

    /**
     * returns -1 if error while retrieving zoom level
     */
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
            delayedZoomTo = zoomLevel;
        }
    }

    @Override
    protected AbstractPositionLayer<LatLng> configPositionLayer(final boolean create) {
        if (create) {
            if (positionLayer == null) {
                positionLayer = mMap == null ? null : new GoogleMapsPositionLayer(mMap, rootView);
            }
            return positionLayer;
        } else {
            positionLayer = null;
        }
        return null;
    }

    // ========================================================================
    // Lifecycle methods

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
