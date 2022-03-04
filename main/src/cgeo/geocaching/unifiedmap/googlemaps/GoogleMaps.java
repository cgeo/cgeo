package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.google.v2.GoogleGeoPoint;
import cgeo.geocaching.maps.google.v2.GoogleMapController;
import cgeo.geocaching.unifiedmap.AbstractUnifiedMap;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractGoogleTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import org.oscim.core.BoundingBox;

public class GoogleMaps extends AbstractUnifiedMap implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final GoogleMapController mapController = new GoogleMapController();

    @Override
    public void init(final AppCompatActivity activity) {
        activity.setContentView(R.layout.unifiedmap_googlemaps);
        final SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.mapViewGM);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
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
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center.getLatitudeE6(), center.getLongitudeE6())));
        mapController.setGoogleMap(googleMap);
    }

    @Override
    public void setTileSource(final AbstractTileProvider newSource) {
        super.setTileSource(newSource);
        ((AbstractGoogleTileProvider) newSource).setMapType(mMap);
    }

    @Override
    public void applyTheme() {
        // @todo
    }

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


    // lifecycle methods

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
