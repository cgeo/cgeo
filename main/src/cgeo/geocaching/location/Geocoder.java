package cgeo.geocaching.location;

import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.location.Address;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulation of the Android {@link android.location.Geocoder} with default error handling. All methods of this class
 * are blocking and will do network lookups.
 *
 */
public class Geocoder {
    private final android.location.Geocoder geocoder;

    public Geocoder(final Context context) {
        geocoder = new android.location.Geocoder(context, Locale.getDefault());
    }

    /**
     * @param keyword
     * @return
     *
     * @see android.location.Geocoder#getFromLocationName(String, int)
     */
    public @NonNull List<Address> getFromLocationName(final String keyword) {
        try {
            return geocoder.getFromLocationName(keyword, 20);
        } catch (final Exception e) {
            handleException(e);
            return Collections.emptyList();
        }
    }

    public @NonNull List<Address> getFromLocation(final Geopoint coords) {
        try {
            return geocoder.getFromLocation(coords.getLatitude(), coords.getLongitude(), 20);
        } catch (final IOException e) {
            handleException(e);
            return Collections.emptyList();
        }
    }

    private static void handleException(final Exception e) {
        // non Google devices come without the geocoder
        if (StringUtils.containsIgnoreCase(e.getMessage(), "Service not Available")) {
            Log.i("No geocoder available");
        }
        else {
            Log.e("Geocoder", e);
        }
    }
}
