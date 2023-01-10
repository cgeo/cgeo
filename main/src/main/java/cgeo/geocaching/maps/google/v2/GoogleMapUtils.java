package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.location.GeoObject;
import cgeo.geocaching.location.GeoObjectList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.pm.PackageInfoCompat;

import java.util.ArrayList;
import java.util.Collection;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.model.LatLng;

class GoogleMapUtils {
    private GoogleMapUtils() {
        // utility class
    }

    public static boolean isGoogleMapsAvailable(final Context context) {
        // check if Google Play Services support the current Google Maps API version
        try {
            final long version = PackageInfoCompat.getLongVersionCode(context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0));
            if (version < GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE) {
                throw new PackageManager.NameNotFoundException("found version " + version);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("play services version too old / " + e.getMessage() + " / at least version " + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE + " required");
            return false;
        }
        return true;
    }

    public static ArrayList<ArrayList<LatLng>> toLatLng(final GeoObjectList gg) {
        final ArrayList<ArrayList<LatLng>> list = new ArrayList<>();
        for (GeoObject go : gg.getGeodata()) {
            list.add(toLatLng(go.getPoints()));
        }
        return list;
    }

    public static ArrayList<LatLng> toLatLng(final Collection<Geopoint> gps) {
        final ArrayList<LatLng> list = new ArrayList<>();
        for (Geopoint gp : gps) {
            list.add(toLatLng(gp));
        }
        return list;
    }

    public static LatLng toLatLng(final Geopoint gp) {
        return new LatLng(gp.getLatitude(), gp.getLongitude());
    }

}
