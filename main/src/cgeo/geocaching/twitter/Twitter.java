package cgeo.geocaching.twitter;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.OAuthTokens;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public final class Twitter {

    private static final int MAX_TWEET_SIZE = 140;

    private Twitter() {
        // Utility class
    }

    public static void postTweetCache(final String geocode, final @Nullable LogEntry logEntry) {
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache != null) {
            postTweet(getStatusMessage(cache, logEntry));
        }
    }

    public static void postTweetTrackable(final String geocode, final @Nullable LogEntry logEntry) {
        final Trackable trackable = DataStore.loadTrackable(geocode);
        if (trackable != null) {
            postTweet(getStatusMessage(trackable, logEntry));
        }
    }

    private static void postTweet(final String status) {
        if (!Settings.isUseTwitter() || !Settings.isTwitterLoginValid()) {
            return;
        }

        try {
            final String trimmed = StringUtils.trim(status);
            final String truncated = StringUtils.length(trimmed) <= MAX_TWEET_SIZE ? trimmed : StringUtils.substring(trimmed, 0, MAX_TWEET_SIZE - 1) + '…';
            final Parameters parameters = new Parameters("status", truncated);

            OAuth.signOAuth("api.twitter.com", "/1.1/statuses/update.json", "POST", true, parameters,
                    new OAuthTokens(Settings.getTokenPublic(), Settings.getTokenSecret()), Settings.getTwitterKeyConsumerPublic(), Settings.getTwitterKeyConsumerSecret());
            final HttpResponse httpResponse = Network.postRequest("https://api.twitter.com/1.1/statuses/update.json", parameters);
            if (Network.isSuccess(httpResponse)) {
                Log.i("Tweet posted");
            } else {
                Log.e("Tweet could not be posted. Reason: " + httpResponse);
            }
        } catch (final Exception e) {
            Log.e("Twitter.postTweet", e);
        }
    }

    private static void appendHashTag(final StringBuilder status, final String tag) {
        if (status.length() + 1 + tag.length() <= MAX_TWEET_SIZE && !StringUtils.contains(status, tag)) {
            status.append(' ').append(tag);
        }
    }

    @NonNull
    static String getStatusMessage(final @NonNull Geocache cache, final @Nullable LogEntry logEntry) {
        return appendHashTags(LogTemplateProvider.applyTemplates(Settings.getCacheTwitterMessage(), new LogContext(cache, logEntry)));
    }

    @NonNull
    static String getStatusMessage(final @NonNull Trackable trackable, final @Nullable LogEntry logEntry) {
        return appendHashTags(LogTemplateProvider.applyTemplates(Settings.getTrackableTwitterMessage(), new LogContext(trackable, logEntry)));
    }

    private static String appendHashTags(final String status) {
        final StringBuilder builder = new StringBuilder(status);
        appendHashTag(builder, "#cgeo");
        appendHashTag(builder, "#geocaching");
        return builder.toString();
    }
}
