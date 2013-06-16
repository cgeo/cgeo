package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.utils.Log;

import java.util.regex.Pattern;

public class GeokretyConnector extends AbstractTrackableConnector {

    private static final Pattern PATTERN_GK_CODE = Pattern.compile("GK[0-9A-F]{4,}");

    @Override
    public boolean canHandleTrackable(String geocode) {
        return geocode != null && PATTERN_GK_CODE.matcher(geocode).matches();
    }

    @Override
    public String getUrl(Trackable trackable) {
        return "http://geokrety.org/konkret.php?id=" + getId(trackable);
    }

    private static int getId(Trackable trackable) {
        try {
            final String hex = trackable.getGeocode().substring(2);
            return Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            Log.e("Trackable.getUrl", e);
        }
        return -1;
    }

}
