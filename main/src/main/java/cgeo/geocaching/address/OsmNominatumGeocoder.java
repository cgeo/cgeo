package cgeo.geocaching.address;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.location.Address;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.lang3.StringUtils;

public class OsmNominatumGeocoder {

    private static final String PROXY_BASE = "https://api.c-geo.de/geocode.php";

    private OsmNominatumGeocoder() {
        // Do not instantiate
    }

    /**
     * Retrieve addresses from a textual location using OSM Nominatum geocoding API using c:geo proxy.
     * The work happens on the network scheduler.
     *
     * @param search the location
     * @return an observable containing zero or more locations
     * @see android.location.Geocoder#getFromLocationName(String, int)
     */
    public static Observable<Address> getFromLocationName(@NonNull final String search) {
        final String method = getMethod(search, 0, 0);
        final Parameters param = new Parameters("q", search, "t", method);

        return Network.requestJSONArray(PROXY_BASE, param)
                .flatMapObservable((Function<ArrayNode, Observable<Address>>) response -> Observable.create(emitter -> {
                    try {
                        for (final JsonNode address : response) {
                            emitter.onNext(osmToAddress(address));
                        }
                        emitter.onComplete();
                    } catch (final Exception e) {
                        Log.e("Error decoding OSM Nominatum address", e);
                        emitter.onError(e);
                    }
                }));
    }

    /**
     * Retrieve the physical address for coordinates. The work happens on the network scheduler.
     *
     * @param coords the coordinates
     * @return an observable containing one location or an error
     */
    public static Single<Address> getFromLocation(@NonNull final Geopoint coords) {
        final String method = getMethod("", coords.getLatitudeE6(), coords.getLongitudeE6());
        final Parameters param = new Parameters("lat", String.valueOf(coords.getLatitude()), "lon", String.valueOf(coords.getLongitude()), "t", method);
        return Network.requestJSON(PROXY_BASE, param)
                .flatMapObservable((Function<ObjectNode, Observable<Address>>) response -> Observable.create(emitter -> {
                    try {
                        emitter.onNext(osmToAddress(response));
                        emitter.onComplete();
                    } catch (final Exception e) {
                        Log.e("Error decoding OSM Nominatum address", e);
                        emitter.onError(e);
                    }
                })).firstOrError();
    }

    public static String getMethod(final String search, final int latE6, final int lonE6) {
        try {
            final String info = "q=" + search.toLowerCase() + ",lat=" + latE6 + ",lon=" + lonE6 + ",ua=" + Network.USER_AGENT + ",api=1";
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] temp = digest.digest(info.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder(2 * temp.length);
            for (byte b : temp) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ignore) {
            return "";
        }
    }

    private static Address osmToAddress(final JsonNode osmAddress) {
        final Address address = new Address(Locale.getDefault());
        final JsonNode add = osmAddress.get("address");
        set(add, address::setLocality, "city", "village", "town");
        set(add, address::setPostalCode, "postcode");
        set(add, address::setAdminArea, "state");
        set(add, address::setSubAdminArea, "county");
        set(add, data -> address.setCountryCode(data.toUpperCase(Locale.ROOT)), "country_code");
        set(add, address::setCountryName, "country");
        set(add, address::setSubLocality, "neighbourhood");

        address.setLatitude(osmAddress.get("lat").asDouble());
        address.setLongitude(osmAddress.get("lon").asDouble());

        int index = 0;
        for (final String addressComponent : new String[]{add.path("road").asText(), address.getSubLocality(), address.getLocality(),
                address.getPostalCode(), address.getSubAdminArea(), address.getAdminArea(), address.getCountryName()}) {
            if (StringUtils.isNotBlank(addressComponent)) {
                address.setAddressLine(index++, addressComponent);
            }
        }
        return address;
    }

    /** uses the first available field content to call the setter method */
    private static void set(final JsonNode parent, final Action1<String> setter, final String... fields) {
        for (String field : fields) {
            if (parent.findValue(field) != null) {
                setter.call(parent.get(field).asText());
            }
        }
    }

}
