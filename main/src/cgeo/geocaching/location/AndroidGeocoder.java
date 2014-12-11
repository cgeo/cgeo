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

/**
 * Encapsulation of the Android {@link Geocoder} with default error handling. All methods of this class
 * are blocking and will do network lookups.
 *
 */
public class AndroidGeocoder {
    private final Geocoder geocoder;

    public AndroidGeocoder(final Context context) {
        geocoder = new Geocoder(context, Locale.getDefault());
    }

    /**
     * Retrieve addresses from a textual location using Android geocoding API. The works happens on the network scheduler.
     *
     * @param keyword the location
     * @return an observable containing zero or more locations
     *
     * @see Geocoder#getFromLocationName(String, int)
     */
    public Observable<Address> getFromLocationName(@NonNull final String keyword) {
        return Observable.defer(new Func0<Observable<Address>>() {
            @Override
            public Observable<Address> call() {
                try {
                    final List<Address> addresses = geocoder.getFromLocationName(keyword, 20);
                    if (CollectionUtils.isEmpty(addresses)) {
                        return Observable.error(new RuntimeException("no result from Android geocoder"));
                    }
                    return Observable.from(addresses);
                } catch (final Exception e) {
                    Log.i("Unable to use Android geocoder: " + e.getMessage());
                    return Observable.error(e);
                }
            }
        }).subscribeOn(RxUtils.networkScheduler);
    }

}
