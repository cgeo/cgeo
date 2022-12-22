package cgeo.geocaching.address;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.apache.commons.collections4.CollectionUtils;

public class AndroidGeocoder {
    private final Geocoder geocoder;

    public AndroidGeocoder(final Context context) {
        geocoder = new Geocoder(context, Locale.getDefault());
    }

    /**
     * Retrieve addresses from a textual location using Android geocoding API. The work happens on the network
     * scheduler.
     *
     * @param keyword the location
     * @return an observable containing zero or more locations
     * @see Geocoder#getFromLocationName(String, int)
     */
    public Observable<Address> getFromLocationName(@NonNull final String keyword) {
        if (!Geocoder.isPresent()) {
            return Observable.error(new RuntimeException("no Android geocoder"));
        }
        return Observable.defer(() -> {
            try {
                return addressesToObservable(geocoder.getFromLocationName(keyword, 20));
            } catch (final Exception e) {
                Log.i("Unable to use Android reverse geocoder: " + e.getMessage());
                return Observable.error(e);
            }
        }).subscribeOn(AndroidRxUtils.networkScheduler);
    }

    /**
     * Retrieve the physical address for coordinates. The work happens on the network scheduler.
     *
     * @param coords the coordinates
     * @return a single containing one location or an error
     */
    public Single<Address> getFromLocation(@NonNull final Geopoint coords) {
        if (!Geocoder.isPresent()) {
            return Single.error(new RuntimeException("no Android reverse geocoder"));
        }
        return Observable.defer(() -> {
            try {
                return addressesToObservable(geocoder.getFromLocation(coords.getLatitude(), coords.getLongitude(), 1));
            } catch (final Exception e) {
                Log.i("Unable to use Android reverse geocoder: " + e.getMessage());
                return Observable.error(e);
            }
        }).subscribeOn(AndroidRxUtils.networkScheduler).firstOrError();
    }

    private static Observable<Address> addressesToObservable(final List<Address> addresses) {
        return CollectionUtils.isEmpty(addresses) ?
                Observable.error(new RuntimeException("no result from Android geocoder")) :
                Observable.fromIterable(addresses);
    }

}
