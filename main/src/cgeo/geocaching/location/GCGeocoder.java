package cgeo.geocaching.location;

import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;
import rx.functions.Func0;

import android.location.Address;

import java.util.Locale;

public class GCGeocoder {

    private GCGeocoder() {
        // Do not instantiate
    }

    /**
     * Retrieve addresses from a textual location using geocaching.com geocoding API. The works happens on the network scheduler.
     *
     * @param address the location
     * @return an observable containing zero or more locations
     *
     * @see android.location.Geocoder#getFromLocationName(String, int)
     */
    public static Observable<Address> getFromLocationName(@NonNull final String address) {
        return Observable.defer(new Func0<Observable<Address>>() {
            @Override
            public Observable<Address> call() {
                if (!Settings.isGCConnectorActive()) {
                    return Observable.error(new RuntimeException("geocaching.com connector is not active"));
                }
                final ObjectNode response = Network.requestJSON("https://www.geocaching.com/api/geocode", new Parameters("q", address));
                if (response == null || !StringUtils.equalsIgnoreCase(response.path("status").asText(), "success")) {
                    return Observable.error(new RuntimeException("unable to use geocaching.com geocoder"));
                }

                final JsonNode data = response.path("data");
                final Address geocodedAddress = new Address(Locale.getDefault());
                try {
                    geocodedAddress.setLatitude(data.get("lat").asDouble());
                    geocodedAddress.setLongitude(data.get("lng").asDouble());
                    geocodedAddress.setAddressLine(0, address);
                    return Observable.just(geocodedAddress);
                } catch (final Exception e) {
                    Log.e("unable to decode answer from geocaching.com geocoder", e);
                    return Observable.error(e);
                }
            }
        }).subscribeOn(RxUtils.networkScheduler);
    }

}
