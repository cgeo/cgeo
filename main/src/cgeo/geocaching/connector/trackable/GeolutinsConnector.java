package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;

public final class GeolutinsConnector extends AbstractTrackableConnector {

    private static final Pattern PATTERN_GL_CODE = Pattern.compile("GL[0-9A-F]{5}|[0-9]{7,8}-[0-9]{3,4}");
    private static final String HOST = "www.geolutins.com";
    private static final String URL = "http://" + HOST;

    /**
     * Get geocode from Geolutins id
     *
     */
    public static String geocode(final int id) {
        return String.format("GL%05X", id);
    }

    public static int getId(final String geocode) {
        try {
            final String hex = geocode.substring(2);
            return Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            Log.e("Trackable.getId", e);
        }
        return -1;
    }

    @Override
    public boolean canHandleTrackable(@Nullable final String geocode) {
        return geocode != null && PATTERN_GL_CODE.matcher(geocode).matches();
    }

    @NonNull
    @Override
    public String getServiceTitle() {
        throw new IllegalStateException("this connector does not have a corresponding name.");
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        return getUrl(trackable.getGeocode());
    }

    private static String getUrl(final String geocode) {
        return URL + "/profil_geolutin.php?ID_Geolutin_Selectionne=" + getId(geocode);
    }

    @Override
    @Nullable
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        final String glid;

        if (StringUtils.startsWithIgnoreCase(geocode, "GL")) {
            glid = geocode;
        } else {
            // This probably a Tracking Code
            Log.d("GeolutinsConnector.searchTrackable: geocode=" + geocode);

            final String geocodeFound = getGeocodeFromTrackingCode(geocode);
            if (geocodeFound == null) {
                return null;
            }
            glid = geocodeFound;
        }

        Log.i("GeolutinsConnector.searchTrackable: glid=" + glid);
        try {
            final String urlDetails = URL + "/xml/api.php?G=" + StringUtils.upperCase(glid);
            Log.i("GeolutinsConnector.searchTrackable URL: " + urlDetails);

            final InputStream response = Network.getResponseStream(Network.getRequest(urlDetails));
            if (response == null) {
                Log.e("GeolutinsConnector.searchTrackable: No data from server");
                return null;
            }
            try {
                final InputSource is = new InputSource(response);
                final List<Trackable> trackables = GeolutinsParser.parse(is);

                if (CollectionUtils.isNotEmpty(trackables)) {
                    DataStore.saveTrackable(trackables.get(0));
                    return trackables.get(0);
                }
            } finally {
                IOUtils.closeQuietly(response);
            }
        } catch (final Exception e) {
            Log.w("GeolutinsConnector.searchTrackable", e);
        }
        return null;
    }

    @Override
    @Nullable
    public String getTrackableCodeFromUrl(@NonNull final String url) {
        final String glid = StringUtils.upperCase(StringUtils.substringAfterLast(url, "ID_Geolutin_Selectionne="));
        if (StringUtils.isNumeric(glid)) {
            return geocode(Integer.parseInt(glid));
        }
        return null;
    }

    @Override
    @NonNull
    public TrackableBrand getBrand() {
        return TrackableBrand.GEOLUTINS;
    }

    @Override
    @NonNull
    public String getHost() {
        return HOST;
    }

    @Override
    @NonNull
    public String getHostUrl() {
        return URL;
    }

    /**
     * Lookup Trackable Geocode from Tracking Code.
     *
     * @param trackingCode
     *          the Trackable Tracking Code to lookup
     * @return
     *          the Trackable Geocode
     */
    @Nullable
    public static String getGeocodeFromTrackingCode(final String trackingCode) {

        final Parameters params = new Parameters("G", trackingCode);
        final InputStream response = Network.getResponseStream(Network.getRequest(URL + "/xml/decode.php", params));
        try {
            final List<Trackable> trackables = GeolutinsParser.parse(new InputSource(response));

            if (CollectionUtils.isNotEmpty(trackables)) {
                return trackables.get(0).getGeocode();
            }
            return null;
        } finally {
            IOUtils.closeQuietly(response);
        }
    }
}
