package cgeo.geocaching.twitter;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.Parameters;

import org.apache.http.HttpResponse;

import android.util.Log;

public final class Twitter {
    public static final int MAX_TWEET_SIZE = 140;

    public static void postTweet(final cgeoapplication app, final String status, final Geopoint coords) {
        if (app == null) {
            return;
        }
        if (!Settings.isTwitterLoginValid()) {
            return;
        }

        try {
            Parameters parameters = new Parameters("status", status);
            if (coords != null) {
                parameters.put(
                        "lat", coords.format(Format.LAT_DECDEGREE_RAW),
                        "long", coords.format(Format.LON_DECDEGREE_RAW),
                        "display_coordinates", "true");
            }

            OAuth.signOAuth("api.twitter.com", "/1/statuses/update.json", "POST", false, parameters, Settings.getTokenPublic(), Settings.getTokenSecret());
            final HttpResponse httpResponse = cgBase.postRequest("http://api.twitter.com/1/statuses/update.json", parameters);
            if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
                Log.i(Settings.tag, "Tweet posted");
            } else {
                Log.e(Settings.tag, "Tweet could not be posted");
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.postTweet: " + e.toString());
        }
    }

    public static String appendHashTag(final String status, final String tag) {
        String result = status;
        if (result.length() + 2 + tag.length() <= 140) {
            result += " #" + tag;
        }
        return result;
    }
}
