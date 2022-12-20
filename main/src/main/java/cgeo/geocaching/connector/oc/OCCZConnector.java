package cgeo.geocaching.connector.oc;

import cgeo.geocaching.utils.Log;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class OCCZConnector extends OCConnector {

    private static final String GEOCODE_PREFIX = "OZ";

    public OCCZConnector() {
        super("OpenCaching.CZ", "www.opencaching.cz", true, GEOCODE_PREFIX, "OC.CZ");
    }

    @Override
    @Nullable
    public String getGeocodeFromUrl(@NonNull final String url) {
        final Uri uri = Uri.parse(url);
        if (!StringUtils.containsIgnoreCase(uri.getHost(), getShortHost())) {
            return null;
        }

        // host.tld?cacheid=cacheid
        final String id = uri.getQueryParameter("cacheid");
        if (StringUtils.isNotBlank(id)) {
            try {
                final String geocode = GEOCODE_PREFIX + StringUtils.leftPad(Integer.toHexString(Integer.parseInt(id)), 4, '0');
                if (canHandle(geocode)) {
                    return geocode;
                }
            } catch (final NumberFormatException e) {
                Log.e("Unexpected URL for opencaching.cz " + url);
            }
        }
        return super.getGeocodeFromUrl(url);
    }
}
