package cgeo.geocaching.twitter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogTemplateProvider;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.OAuthTokens;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.apache.commons.lang3.StringUtils;

public final class Twitter {

    private static final int MAX_TWEET_SIZE = 140;

    private Twitter() {
        // Utility class
    }

    @WorkerThread
    public static void postTweetCache(final String geocode, @Nullable final LogEntry logEntry) {
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache != null) {
            postTweet(getStatusMessage(cache, logEntry));
        }
    }

    @WorkerThread
    public static void postTweetTrackable(final String geocode, @Nullable final LogEntry logEntry) {
        final Trackable trackable = DataStore.loadTrackable(geocode);
        if (trackable != null) {
            postTweet(getStatusMessage(trackable, logEntry));
        }
    }

    @WorkerThread
    private static void postTweet(final String status) {
        if (!Settings.isUseTwitter() || !Settings.isTwitterLoginValid()) {
            return;
        }

        try {
            final String trimmed = StringUtils.trim(status);
            final String truncated = StringUtils.length(trimmed) <= MAX_TWEET_SIZE ? trimmed : StringUtils.substring(trimmed, 0, MAX_TWEET_SIZE - 1) + CgeoApplication.getInstance().getString(R.string.ellipsis);
            final Parameters parameters = new Parameters("status", truncated);

            OAuth.signOAuth("api.twitter.com", "/1.1/statuses/update.json", "POST", true, parameters,
                    new OAuthTokens(Settings.getTokenPublic(), Settings.getTokenSecret()), Settings.getTwitterKeyConsumerPublic(), Settings.getTwitterKeyConsumerSecret());
            try {
                Network.completeWithSuccess(Network.postRequest("https://api.twitter.com/1.1/statuses/update.json", parameters)).blockingAwait();
                Log.i("Tweet posted");
            } catch (final Exception ignored) {
                Log.e("Tweet could not be posted");
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
    static String getStatusMessage(@NonNull final Geocache cache, @Nullable final LogEntry logEntry) {
        return appendHashTags(LogTemplateProvider.applyTemplatesNoIncrement(Settings.getCacheTwitterMessage(), new LogContext(cache, logEntry)));
    }

    @NonNull
    static String getStatusMessage(@NonNull final Trackable trackable, @Nullable final LogEntry logEntry) {
        return appendHashTags(LogTemplateProvider.applyTemplates(Settings.getTrackableTwitterMessage(), new LogContext(trackable, logEntry)));
    }

    private static String appendHashTags(final String status) {
        final StringBuilder builder = new StringBuilder(status);
        appendHashTag(builder, "#cgeo");
        appendHashTag(builder, "#geocaching");
        return builder.toString();
    }
}
