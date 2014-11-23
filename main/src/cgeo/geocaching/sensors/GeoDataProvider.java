package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils.LooperCallbacks;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class GeoDataProvider extends LooperCallbacks<GeoData> {

    private final Context context;
    private final LocationManager geoManager;
    private Location latestGPSLocation = null;
    private final Listener networkListener = new Listener();
    private final Listener gpsListener = new Listener();

    /**
     * Build a new geo data provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     */
    protected GeoDataProvider(final Context context) {
        super(2500, TimeUnit.MILLISECONDS);
        this.context = context.getApplicationContext();
        geoManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static Observable<GeoData> create(final Context context) {
        return Observable.create(new GeoDataProvider(context));
    }

    @Override
    public void onStart() {
        final GeoData initialLocation = GeoData.getInitialLocation(context);
        if (initialLocation != null) {
            subject.onNext(initialLocation);
        }
        Log.d("GeoDataProvider: starting the GPS and network listeners");
        try {
            geoManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
        } catch (final Exception e) {
            Log.w("Unable to create GPS location provider: " + e.getMessage());
        }
        try {
            geoManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
        } catch (final Exception e) {
            Log.w("Unable to create network location provider: " + e.getMessage());
        }
    }

    @Override
    protected void onStop() {
        Log.d("GeoDataProvider: stopping the GPS and network listeners");
        geoManager.removeUpdates(networkListener);
        geoManager.removeUpdates(gpsListener);
    }

    private class Listener implements LocationListener {

        @Override
        public void onLocationChanged(final Location location) {
            if (StringUtils.equals(location.getProvider(), LocationManager.GPS_PROVIDER)) {
                latestGPSLocation = location;
                assign(latestGPSLocation);
            } else {
                assign(GeoData.best(latestGPSLocation, location));
            }
        }

        @Override
        public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            // nothing
        }

        @Override
        public void onProviderDisabled(final String provider) {
            // nothing
        }

        @Override
        public void onProviderEnabled(final String provider) {
            // nothing
        }
    }

    private void assign(final Location location) {
        // We do not necessarily get signalled when satellites go to 0/0.
        final GeoData current = new GeoData(location);
        subject.onNext(current);
    }

}
