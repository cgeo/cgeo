package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Pattern;

public class GeokretyConnector extends AbstractTrackableConnector {

    private static final Pattern PATTERN_GK_CODE = Pattern.compile("GK[0-9A-F]{4,}");

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return geocode != null && PATTERN_GK_CODE.matcher(geocode).matches();
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        return "http://geokrety.org/konkret.php?id=" + getId(trackable.getGeocode());
    }

    @Override
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        final String page = Network.getResponseData(Network.getRequest("http://geokrety.org/export2.php?gkid=" + getId(geocode)));
        if (page == null) {
            return null;
        }
        return GeokretyParser.parse(page);
    }

    protected static int getId(final String geocode) {
        try {
            final String hex = geocode.substring(2);
            return Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            Log.e("Trackable.getUrl", e);
        }
        return -1;
    }

    @Override
    public @Nullable
    String getTrackableCodeFromUrl(@NonNull final String url) {
        // http://geokrety.org/konkret.php?id=38545
        final String id = StringUtils.substringAfterLast(url, "konkret.php?id=");
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

}
