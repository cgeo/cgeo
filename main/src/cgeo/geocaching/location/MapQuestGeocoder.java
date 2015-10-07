package cgeo.geocaching.location;

import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func0;

import android.location.Address;

import java.util.Locale;

public class MapQuestGeocoder {

    private static final String MAPQUEST_KEY = "Fmjtd|luurn1u2n9,bs=o5-9wynua";

    private MapQuestGeocoder() {
        // Do not instantiate
    }

    /**
     * Retrieve addresses from a textual location using MapQuest geocoding API. The work happens on the network
     * scheduler.
     *
     * @param address
     *            the location
     * @return an observable containing zero or more locations
     *
     * @see android.location.Geocoder#getFromLocationName(String, int)
     */
    public static Observable<Address> getFromLocationName(@NonNull final String address) {
        return get("address", new Parameters("location", address, "maxResults", "20", "thumbMaps", "false"));
    }

    /**
     * Retrieve the physical address for coordinates. The work happens on the network scheduler.
     *
     * @param coords the coordinates
     * @return an observable containing one location or an error
     */
    public static Observable<Address> getFromLocation(@NonNull final Geopoint coords) {
        return get("reverse", new Parameters("location", String.format(Locale.US, "%f,%f", coords.getLatitude(), coords.getLongitude()))).first();
    }

    private static Observable<Address> get(@NonNull final String method, @NonNull final Parameters parameters) {
        return Observable.defer(new Func0<Observable<Address>>() {
            @Override
            public Observable<Address> call() {
                final ObjectNode response = Network.requestJSON("https://open.mapquestapi.com/geocoding/v1/" + method,
                        parameters.put("key", MAPQUEST_KEY));
                if (response == null) {
                    Log.w("MapQuest decoder error: no response");
                    return Observable.error(new RuntimeException("no answer from MapQuest geocoder"));
                }
                final int statusCode = response.path("info").path("statuscode").asInt(-1);
                if (statusCode != 0) {
                    Log.w("MapQuest decoder error: statuscode is not 0");
                    return Observable.error(new RuntimeException("no correct answer from MapQuest geocoder"));
                }
                return Observable.create(new OnSubscribe<Address>() {
                    @Override
                    public void call(final Subscriber<? super Address> subscriber) {
                        try {
                            for (final JsonNode address: response.get("results").get(0).get("locations")) {
                                subscriber.onNext(mapquestToAddress(address));
                            }
                            subscriber.onCompleted();
                        } catch (final Exception e) {
                            Log.e("Error decoding MapQuest address", e);
                            subscriber.onError(e);
                        }
                    }
                });
            }
        }).subscribeOn(RxUtils.networkScheduler);
    }

    private static Address mapquestToAddress(final JsonNode mapquestAddress) {
        final Address address = new Address(Locale.getDefault());
        for (int i = 1; i <= 6; i++) {
            final String adminAreaName = "adminArea" + i;
            setComponent(address, mapquestAddress, adminAreaName, mapquestAddress.path(adminAreaName + "Type").asText());
        }
        setComponent(address, mapquestAddress, "postalCode", "PostalCode");
        int index = 0;
        for (final String addressComponent: new String[]{ mapquestAddress.path("street").asText(), address.getSubLocality(), address.getLocality(),
                address.getPostalCode(), address.getSubAdminArea(), address.getAdminArea(), address.getCountryCode() }) {
            if (StringUtils.isNotBlank(addressComponent)) {
                address.setAddressLine(index++, addressComponent);
            }
        }
        final JsonNode latLng = mapquestAddress.get("latLng");
        address.setLatitude(latLng.get("lat").asDouble());
        address.setLongitude(latLng.get("lng").asDouble());
        return address;
    }

    private static void setComponent(final Address address, final JsonNode mapquestAddress, final String adminArea, final String adminAreaType) {
        final String content = StringUtils.trimToNull(mapquestAddress.path(adminArea).asText());
        switch (adminAreaType) {
            case "City":
                address.setLocality(content);
                break;
            case "Neighborhood":
                address.setSubLocality(content);
                break;
            case "PostalCode":
                address.setPostalCode(content);
                break;
            case "State":
                address.setAdminArea(content);
                break;
            case "County":
                address.setSubAdminArea(content);
                break;
            case "Country":
                address.setCountryCode(content);
                address.setCountryName(new Locale("", content).getDisplayCountry());
                break;
            // Make checkers happy
            default:
                break;
        }
    }

}
