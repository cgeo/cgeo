package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;

public class GeokretyConnector extends AbstractTrackableConnector {

    private static final Pattern PATTERN_GK_CODE = Pattern.compile("GK[0-9A-F]{4,}");

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return geocode != null && PATTERN_GK_CODE.matcher(geocode).matches();
    }

    @Override
    public String getUrl(final Trackable trackable) {
        return "http://geokrety.org/konkret.php?id=" + getId(trackable.getGeocode());
    }

    @Override
    public final Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return searchTrackable(geocode);
    }

    public static Trackable searchTrackable(final String geocode) {
        Log.d("GeokretyConnector.searchTrackable: gkid=" + getId(geocode));
        try {
            final InputStream response = Network.getResponseStream(Network.getRequest("http://geokrety.org/export2.php?gkid=" + getId(geocode)));
            final InputSource is = new InputSource(response);
            return GeokretyParser.parse(is).get(0);
        } catch (Exception e) {
            Log.w("GeokretyConnector.searchTrackable", e);
            return null;
        }
    }

    public static List<Trackable> searchTrackables(final String geocode) {
        Log.d("GeokretyConnector.searchTrackables: wpt=" + geocode);
        try {
            final InputStream response = Network.getResponseStream(Network.getRequest("http://geokrety.org/export2.php?wpt=" + geocode));
            final InputSource is = new InputSource(response);
            return GeokretyParser.parse(is);
        } catch (final Exception e) {
            Log.w("GeokretyConnector.searchTrackables", e);
            return null;
        }
    }

    protected static int getId(String geocode) {
        try {
            final String hex = geocode.substring(2);
            return Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            Log.e("Trackable.getId", e);
        }
        return -1;
    }

    @Override
    public @Nullable
    String getTrackableCodeFromUrl(@NonNull String url) {
        // http://geokrety.org/konkret.php?id=38545
        String id = StringUtils.substringAfterLast(url, "konkret.php?id=");
        if (StringUtils.isNumeric(id)) {
            return geocode(Integer.parseInt(id));
        }
        return null;
    }

    /**
     * Get geocode from geokrety id
     *
     * @param id
     * @return
     */
    public static String geocode(final int id) {
        return String.format("GK%04X", id);
    }

    @Override
    public boolean isLoggable() {
        return false;
    }
}
