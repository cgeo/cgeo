package cgeo.geocaching.address;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import android.location.Address;

import androidx.annotation.NonNull;

import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.lang3.StringUtils;

public class MapQuestGeocoder {

    private static final String MAPQUEST_KEY = "Fmjtd|luurn1u2n9,bs=o5-9wynua";

    private MapQuestGeocoder() {
        // Do not instantiate
    }

    /**
     * Retrieve addresses from a textual location using MapQuest geocoding API. The work happens on the network
     * scheduler.
     *
     * @param address the location
     * @return an observable containing zero or more locations
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
    public static Single<Address> getFromLocation(@NonNull final Geopoint coords) {
        return get("reverse", new Parameters("location", String.format(Locale.US, "%f,%f", coords.getLatitude(), coords.getLongitude()))).firstOrError();
    }

    private static Observable<Address> get(@NonNull final String method, @NonNull final Parameters parameters) {
        return Network.requestJSON("https://www.mapquestapi.com/geocoding/v1/" + method,
                parameters.put("key", MAPQUEST_KEY))
                .flatMapObservable((Function<ObjectNode, Observable<Address>>) response -> {
                    final int statusCode = response.path("info").path("statuscode").asInt(-1);
                    if (statusCode != 0) {
                        Log.w("MapQuest decoder error: statuscode is not 0");
                        throw new IllegalStateException("no correct answer from MapQuest geocoder");
                    }
                    return Observable.create(emitter -> {
                        try {
                            for (final JsonNode address : response.get("results").get(0).get("locations")) {
                                emitter.onNext(mapquestToAddress(address));
                            }
                            emitter.onComplete();
                        } catch (final Exception e) {
                            Log.e("Error decoding MapQuest address", e);
                            emitter.onError(e);
                        }
                    });
                });
    }

    private static Address mapquestToAddress(final JsonNode mapquestAddress) {
        final Address address = new Address(Locale.getDefault());
        for (int i = 1; i <= 6; i++) {
            final String adminAreaName = "adminArea" + i;
            setComponent(address, mapquestAddress, adminAreaName, mapquestAddress.path(adminAreaName + "Type").asText());
        }
        setComponent(address, mapquestAddress, "postalCode", "PostalCode");
        int index = 0;
        for (final String addressComponent : new String[]{mapquestAddress.path("street").asText(), address.getSubLocality(), address.getLocality(),
                address.getPostalCode(), address.getSubAdminArea(), address.getAdminArea(), address.getCountryCode()}) {
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
