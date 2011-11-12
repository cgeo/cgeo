package cgeo.geocaching.go4cache;

import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Go4CacheUser {
    private static final Pattern patternGeocode = Pattern.compile("^(GC[A-Z0-9]+)(\\: ?(.+))?$", Pattern.CASE_INSENSITIVE);

    private final Date located;
    private final String username;
    private final Geopoint coords;
    private final String action;
    private final String client;

    private String actionForDisplay;
    private String geocode;

    public Go4CacheUser(final String username, final Geopoint coords, final Date located, final String action, final String client) {
        this.username = username;
        this.coords = coords;
        this.located = located;
        this.action = action;
        this.client = client;
    }

    public Date getLocated() {
        return located;
    }

    public String getUsername() {
        return username;
    }

    public Geopoint getCoords() {
        return coords;
    }

    public int getIconId() {
        if (null == client) {
            return -1;
        }
        if (client.equalsIgnoreCase("c:geo")) {
            return R.drawable.client_cgeo;
        } else if (client.equalsIgnoreCase("preCaching")) {
            return R.drawable.client_precaching;
        } else if (client.equalsIgnoreCase("Handy Geocaching")) {
            return R.drawable.client_handygeocaching;
        }
        return -1;
    }

    private void getGeocodeAndAction() {
        final Matcher matcherGeocode = patternGeocode.matcher(action.trim());

        geocode = "";
        if (0 == action.length() || action.equalsIgnoreCase("pending")) {
            actionForDisplay = "Looking around";
        } else if (action.equalsIgnoreCase("tweeting")) {
            actionForDisplay = "Tweeting";
        } else if (matcherGeocode.find()) {
            if (null != matcherGeocode.group(1)) {
                geocode = matcherGeocode.group(1).trim().toUpperCase();
            }
            if (null != matcherGeocode.group(3)) {
                actionForDisplay = "Heading to " + geocode + " (" + matcherGeocode.group(3).trim() + ")";
            } else {
                actionForDisplay = "Heading to " + geocode;
            }
        } else {
            actionForDisplay = action;
        }
    }

    public String getAction() {
        if (null == actionForDisplay) {
            getGeocodeAndAction();
        }
        return actionForDisplay;
    }

    public String getGeocode() {
        if (null == geocode) {
            getGeocodeAndAction();
        }
        return geocode;
    }
}
