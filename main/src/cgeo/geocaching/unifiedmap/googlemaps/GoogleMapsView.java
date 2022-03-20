package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.google.v2.GoogleGeoPoint;
import cgeo.geocaching.maps.google.v2.GoogleMapController;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.AbstractUnifiedMap;
import cgeo.geocaching.unifiedmap.UnifiedMapPosition;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractGoogleTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
public class GoogleMapsView extends AbstractUnifiedMap<LatLng> implements OnMapReadyCallback {

    private GoogleMap mMap;
    private View rootView;
    private final GoogleMapController mapController = new GoogleMapController();

    @Override
    public void init(final AppCompatActivity activity) {
        super.init(activity);
        activity.setContentView(R.layout.unifiedmap_googlemaps);
        rootView = activity.findViewById(R.id.unifiedmap_gm);
        final SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.mapViewGM);
        assert mapFragment != null;
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
        positionLayer = configPositionLayer(true);
    }

    @Override
    public void setTileSource(final AbstractTileProvider newSource) {
        super.setTileSource(newSource);
        ((AbstractGoogleTileProvider) newSource).setMapType(mMap);
    }

    /** keep track of rotation and zoom level changes **/
    protected void configMapChangeListener(final boolean enable) {
        if (mMap != null) {
            mMap.setOnCameraIdleListener(null);
            if (enable) {
                mMap.setOnCameraIdleListener(() -> {
                    if (activityMapChangeListener != null) {
                        final CameraPosition pos = mMap.getCameraPosition();
                        activityMapChangeListener.call(new UnifiedMapPosition(pos.target.latitude, pos.target.longitude, (int) pos.zoom, pos.bearing));
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
    };

    // ========================================================================
    // zoom & heading methods

    @Override
    public void zoomToBounds(final BoundingBox bounds) {
        if (mMap != null) {
            mapController.zoomToSpan((int) (bounds.getLatitudeSpan() * 1E6), (int) (bounds.getLongitudeSpan() * 1E6));
            mapController.animateTo(new GoogleGeoPoint(bounds.getCenterPoint()));
        }
    };

    @Override
    public int getCurrentZoom() {
        return 0; // @todo: return actual current zoom level
    }

    @Override
    public void setZoom(final int zoomLevel) {
        // @todo: actually set zoom level
    }

    @Override
    protected AbstractPositionLayer<LatLng> configPositionLayer(final boolean create) {
        if (create) {
            return positionLayer != null ? positionLayer : mMap == null ? null : new GoogleMapsPositionLayer(mMap, rootView);
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
