package cgeo.geocaching.location;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;
import rx.functions.Func0;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.util.List;
import java.util.Locale;

public class AndroidGeocoder {
    private final Geocoder geocoder;

    public AndroidGeocoder(final Context context) {
        geocoder = new Geocoder(context, Locale.getDefault());
    }

    /**
     * Retrieve addresses from a textual location using Android geocoding API. The work happens on the network
     * scheduler.
     *
     * @param keyword
     *            the location
     * @return an observable containing zero or more locations
     *
     * @see Geocoder#getFromLocationName(String, int)
     */
    public Observable<Address> getFromLocationName(@NonNull final String keyword) {
        if (!Geocoder.isPresent()) {
            return Observable.error(new RuntimeException("no Android geocoder"));
        }
        return Observable.defer(new Func0<Observable<Address>>() {
            @Override
            public Observable<Address> call() {
                try {
                    return addressesToObservable(geocoder.getFromLocationName(keyword, 20));
                } catch (final Exception e) {
                    Log.i("Unable to use Android reverse geocoder: " + e.getMessage());
                    return Observable.error(e);
                }
            }
        }).subscribeOn(RxUtils.networkScheduler);
    }

    /**
     * Retrieve the physical address for coordinates. The work happens on the network scheduler.
     *
     * @param coords the coordinates
     * @return an observable containing one location or an error
     */
    public Observable<Address> getFromLocation(@NonNull final Geopoint coords) {
        if (!Geocoder.isPresent()) {
            return Observable.error(new RuntimeException("no Android reverse geocoder"));
        }
        return Observable.defer(new Func0<Observable<Address>>() {
            @Override
            public Observable<Address> call() {
                try {
                    return addressesToObservable(geocoder.getFromLocation(coords.getLatitude(), coords.getLongitude(), 1));
                } catch (final Exception e) {
                    Log.i("Unable to use Android reverse geocoder: " + e.getMessage());
                    return Observable.error(e);
                }
            }
        }).subscribeOn(RxUtils.networkScheduler).first();
    }

    private static Observable<Address> addressesToObservable(final List<Address> addresses) {
        return CollectionUtils.isEmpty(addresses) ?
                Observable.<Address>error(new RuntimeException("no result from Android geocoder")) :
                Observable.from(addresses);
    }

}
