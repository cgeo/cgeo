package cgeo.geocaching.twitter;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgTrackable;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import org.apache.http.HttpResponse;


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
            final HttpResponse httpResponse = Network.postRequest("http://api.twitter.com/1/statuses/update.json", parameters);
            if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
                Log.i("Tweet posted");
            } else {
                Log.e("Tweet could not be posted");
            }
        } catch (Exception e) {
            Log.e("cgBase.postTweet: " + e.toString());
        }
    }

    public static String appendHashTag(final String status, final String tag) {
        String result = status;
        if (result.length() + 2 + tag.length() <= 140) {
            result += " #" + tag;
        }
        return result;
    }

    public static void postTweetCache(String geocode) {
        final cgCache cache = cgeoapplication.getInstance().loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        String status;
        final String url = cache.getUrl();
        if (url.length() >= 100) {
            status = "I found " + url;
        }
        else {
            String name = cache.getName();
            status = "I found " + name + " (" + url + ")";
            if (status.length() > MAX_TWEET_SIZE) {
                name = name.substring(0, name.length() - (status.length() - MAX_TWEET_SIZE) - 3) + "...";
            }
            status = "I found " + name + " (" + url + ")";
            status = appendHashTag(status, "cgeo");
            status = appendHashTag(status, "geocaching");
        }
    
        postTweet(cgeoapplication.getInstance(), status, null);
    }

    public static void postTweetTrackable(String geocode) {
        final cgTrackable trackable = cgeoapplication.getInstance().getTrackableByGeocode(geocode);
        String name = trackable.getName();
        if (name.length() > 82) {
            name = name.substring(0, 79) + "...";
        }
        StringBuilder builder = new StringBuilder("I touched ");
        builder.append(name);
        if (trackable.getUrl() != null) {
            builder.append(" (").append(trackable.getUrl()).append(')');
        }
        builder.append('!');
        String status = appendHashTag(builder.toString(), "cgeo");
        status = appendHashTag(status, "geocaching");
        postTweet(cgeoapplication.getInstance(), status, null);
    }
}
