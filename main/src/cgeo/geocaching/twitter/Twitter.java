package cgeo.geocaching.twitter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.Settings;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;

public final class Twitter {
    private static final String HASH_PREFIX_WITH_BLANK = " #";
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
            Log.e("cgBase.postTweet", e);
        }
    }

    public static void appendHashTag(final StringBuilder status, final String tag) {
        if (status.length() + HASH_PREFIX_WITH_BLANK.length() + tag.length() <= MAX_TWEET_SIZE) {
            status.append(HASH_PREFIX_WITH_BLANK).append(tag);
        }
    }

    public static void postTweetCache(String geocode) {
        if (!Settings.isUseTwitter()) {
            return;
        }
        if (!Settings.isTwitterLoginValid()) {
            return;
        }
        final Geocache cache = cgData.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        postTweet(cgeoapplication.getInstance(), getStatusMessage(cache), null);
    }

    static String getStatusMessage(Geocache cache) {
        String name = cache.getName();
        if (name.length() > 100) {
            name = name.substring(0, 100) + '…';
            }
        final String url = StringUtils.defaultString(cache.getUrl());
        String status = "I found [NAME] ([URL])";
        return fillTemplate(status, name, url);
    }

    private static String fillTemplate(String template, String name, final String url) {
        String result = StringUtils.replace(template, "[NAME]", name);
        result = StringUtils.replace(result, "[URL]", url);
        StringBuilder builder = new StringBuilder(result);
        appendHashTag(builder, "cgeo");
        appendHashTag(builder, "geocaching");
        return builder.toString();
    }

    public static void postTweetTrackable(String geocode) {
        final Trackable trackable = cgData.loadTrackable(geocode);
        postTweet(cgeoapplication.getInstance(), getStatusMessage(trackable), null);
    }

    static String getStatusMessage(Trackable trackable) {
        String name = trackable.getName();
        if (name.length() > 82) {
            name = name.substring(0, 81) + '…';
        }
        String url = StringUtils.defaultString(trackable.getUrl());
        String status = "I touched [NAME] ([URL])!";
        return fillTemplate(status, name, url);
    }
}
