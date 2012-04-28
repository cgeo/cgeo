package cgeo.geocaching.go4cache;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.res.Resources;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Go4CacheUser {
    private static final Pattern patternGeocode = Pattern.compile("^(GC[A-Z0-9]+)(\\: ?(.+))?$", Pattern.CASE_INSENSITIVE);

    private final Date date;
    private final String username;
    private final Geopoint coords;
    private final String action;
    private final String client;

    private String actionForDisplay;
    private String geocode;
    private Resources res;

    public Go4CacheUser(final String username, final Geopoint coords, final Date date, final String action, final String client) {
        this.username = username;
        this.coords = coords;
        this.date = new Date(date.getTime());
        this.action = action;
        this.client = client;
    }

    public Date getDate() {
        return date;
    }

    public String getUsername() {
        return username;
    }

    public Geopoint getCoords() {
        return coords;
    }

    public int getIconId() {
        if (client == null) {
            return -1;
        }
        if (client.equalsIgnoreCase("c:geo")) {
            return R.drawable.client_cgeo;
        }
        if (client.equalsIgnoreCase("preCaching")) {
            return R.drawable.client_precaching;
        }
        if (client.equalsIgnoreCase("Handy Geocaching")) {
            return R.drawable.client_handygeocaching;
        }
        return -1;
    }

    private void getGeocodeAndAction() {
        final Matcher matcherGeocode = patternGeocode.matcher(action.trim());
        res = cgeoapplication.getInstance().getResources();

        geocode = "";
        if (0 == action.length() || action.equalsIgnoreCase("pending")) {
            actionForDisplay = res.getString(R.string.go4cache_looking_around);
        } else if (action.equalsIgnoreCase("tweeting")) {
            actionForDisplay = res.getString(R.string.go4cache_tweeting);
        } else if (matcherGeocode.find()) {
            if (null != matcherGeocode.group(1)) {
                geocode = matcherGeocode.group(1).trim().toUpperCase();
            }
            if (null != matcherGeocode.group(3)) {
                actionForDisplay = res.getString(R.string.go4cache_heading_to) + " " + geocode + " (" + matcherGeocode.group(3).trim() + ")";
            } else {
                actionForDisplay = res.getString(R.string.go4cache_heading_to) + " " + geocode;
            }
        } else {
            actionForDisplay = action;
        }
    }

    public String getAction() {
        if (null == actionForDisplay) {
            getGeocodeAndAction();
        }
        return actionForDisplay + getTime() + ".";
    }

    private String getTime() {
        int minutes = (int) ((System.currentTimeMillis() - date.getTime()) / 60000);
        if (minutes < 0) {
            minutes = 0;
        }
        return " (" + res.getQuantityString(R.plurals.go4cache_time_minutes, minutes, minutes) + ")";
    }

    public String getGeocode() {
        if (null == geocode) {
            getGeocodeAndAction();
        }
        return geocode;
    }
}