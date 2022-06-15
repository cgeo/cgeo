package cgeo.geocaching.gcvote;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public final class GCVote implements ICredentials {

    // gcvote.com does not have a https certificate. However, Guido (the owner of gcvote.com) told
    // us on 2017-03-21 that the site is accessible through its provider https endpoint at
    // https://ssl.webpack.de/gcvote.com

    public static final float NO_RATING = 0;

    private static final int MAX_CACHED_RATINGS = 1000;
    private static final LeastRecentlyUsedMap<String, GCVoteRating> RATINGS_CACHE = new LeastRecentlyUsedMap.LruCache<>(MAX_CACHED_RATINGS);

    private GCVote() {
        // utility class
    }

    @NonNull
    public static GCVote getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Get user rating for a given guid or geocode. For a guid first the ratings cache is checked
     * before a request to gcvote.com is made.
     */
    @Nullable
    @WorkerThread
    public static GCVoteRating getRating(final String guid, final String geocode) {
        if (StringUtils.isNotBlank(guid) && RATINGS_CACHE.containsKey(guid)) {
            return RATINGS_CACHE.get(guid);
        }

        final Map<String, GCVoteRating> ratings = getRating(singletonOrNull(guid), singletonOrNull(geocode));
        return MapUtils.isNotEmpty(ratings) ? ratings.values().iterator().next() : null;
    }

    @Nullable
    private static List<String> singletonOrNull(final String item) {
        return StringUtils.isNotBlank(item) ? Collections.singletonList(item) : null;
    }

    /**
     * Get user ratings from gcvote.com
     */
    @NonNull
    @WorkerThread
    private static Map<String, GCVoteRating> getRating(final List<String> guids, final List<String> geocodes) {
        if (guids == null && geocodes == null) {
            return Collections.emptyMap();
        }

        final Parameters params = new Parameters("version", "cgeo");
        final Credentials login = Settings.getGCVoteLogin();
        if (login.isValid()) {
            params.put("userName", login.getUserName(), "password", login.getPassword());
        }

        // use guid or gccode for lookup
        final boolean requestByGuids = CollectionUtils.isNotEmpty(guids);
        if (requestByGuids) {
            params.put("cacheIds", StringUtils.join(guids, ','));
        } else {
            params.put("waypoints", StringUtils.join(geocodes, ','));
        }
        final InputStream response = Network.getResponseStream(Network.getRequest("https://ssl.webpack.de/gcvote.com/getVotes.php", params));
        if (response == null) {
            return Collections.emptyMap();
        }
        try {
            return getRatingsFromXMLResponse(response, requestByGuids);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    @NonNull
    @WorkerThread
    static Map<String, GCVoteRating> getRatingsFromXMLResponse(@NonNull final InputStream response, final boolean requestByGuids) {
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(response, StandardCharsets.UTF_8.name());
            boolean loggedIn = false;
            final Map<String, GCVoteRating> ratings = new HashMap<>();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    final String tagName = xpp.getName();
                    if (StringUtils.equals(tagName, "vote")) {
                        final String id = xpp.getAttributeValue(null, requestByGuids ? "cacheId" : "waypoint");
                        final float myVote = loggedIn ? Float.parseFloat(xpp.getAttributeValue(null, "voteUser")) : 0;
                        final GCVoteRating voteRating = new GCVoteRating(Float.parseFloat(xpp.getAttributeValue(null, "voteAvg")),
                                Integer.parseInt(xpp.getAttributeValue(null, "voteCnt")),
                                myVote);
                        ratings.put(id, voteRating);
                    } else if (StringUtils.equals(tagName, "votes")) {
                        loggedIn = StringUtils.equals(xpp.getAttributeValue(null, "loggedIn"), "true");
                    }
                }
                eventType = xpp.next();
            }
            RATINGS_CACHE.putAll(ratings);
            return ratings;
        } catch (final NumberFormatException | XmlPullParserException | IOException e) {
            Log.e("Cannot parse GCVote result", e);
            return Collections.emptyMap();

        }
    }

    /**
     * Transmit user vote to gcvote.com
     *
     * @param cache  the geocache (supported by GCVote)
     * @param rating the rating
     * @return {@code true} if the rating was submitted successfully
     */
    @WorkerThread
    public static boolean setRating(@NonNull final Geocache cache, final float rating) {
        final IConnector connector = ConnectorFactory.getConnector(cache);
        if (!(connector instanceof IVotingCapability)) {
            throw new IllegalArgumentException("Service does not support voting" + cache);
        }
        final IVotingCapability votingConnector = (IVotingCapability) connector;
        if (!votingConnector.supportsVoting(cache)) {
            throw new IllegalArgumentException("voting is not possible for " + cache);
        }
        if (!votingConnector.isValidRating(rating)) {
            throw new IllegalArgumentException("invalid rating " + rating);
        }

        final Credentials login = Settings.getGCVoteLogin();
        if (login.isInvalid()) {
            Log.e("GCVote.setRating: cannot find credentials");
            return false;
        }
        final Parameters params = new Parameters(
                "userName", login.getUserName(),
                "password", login.getPassword(),
                "cacheId", cache.getGuid(),
                "waypoint", cache.getGeocode(),
                "voteUser", String.format(Locale.US, "%.1f", rating),
                "version", "cgeo");

        final String result = StringUtils.trim(Network.getResponseData(Network.getRequest("https://ssl.webpack.de/gcvote.com/setVote.php", params)));
        if (!StringUtils.equalsIgnoreCase(result, "ok")) {
            Log.e("GCVote.setRating: could not post rating, answer was " + result);
            return false;
        }
        return true;
    }

    @WorkerThread
    public static void loadRatings(@NonNull final List<Geocache> caches) {
        if (!Settings.isRatingWanted()) {
            return;
        }

        final List<String> geocodes = getVotableGeocodes(caches);
        if (geocodes.isEmpty()) {
            return;
        }

        try {
            final Map<String, GCVoteRating> ratings = getRating(null, geocodes);

            // save found cache coordinates
            for (final Geocache cache : caches) {
                if (ratings.containsKey(cache.getGeocode())) {
                    final GCVoteRating rating = ratings.get(cache.getGeocode());

                    cache.setRating(rating.getRating());
                    cache.setVotes(rating.getVotes());
                    cache.setMyVote(rating.getMyVote());
                }
            }
        } catch (final Exception e) {
            Log.e("GCVote.loadRatings", e);
        }
    }

    /**
     * Get geocodes of all the caches, which can be used with GCVote. Non-GC caches will be filtered out.
     */
    @NonNull
    private static List<String> getVotableGeocodes(@NonNull final Collection<Geocache> caches) {
        final List<String> geocodes = new ArrayList<>(caches.size());
        for (final Geocache cache : caches) {
            final String geocode = cache.getGeocode();

            final IConnector connector = ConnectorFactory.getConnector(cache);

            if (StringUtils.isNotBlank(geocode) && connector instanceof IVotingCapability && ((IVotingCapability) connector).supportsVoting(cache)) {
                geocodes.add(geocode);
            }
        }
        return geocodes;
    }

    @NonNull
    public static String getWebsite() {
        return "http://gcvote.com";
    }

    @NonNull
    public static String getCreateAccountUrl() {
        return "http://gcvote.com/help_en.php";
    }

    @Override
    public int getUsernamePreferenceKey() {
        return R.string.pref_user_vote;
    }

    @Override
    public int getPasswordPreferenceKey() {
        return R.string.pref_pass_vote;
    }

    @Override
    public Credentials getCredentials() {
        return Settings.getCredentials(R.string.pref_user_vote, R.string.pref_pass_vote);
    }

    private static class SingletonHolder {
        @NonNull
        private static final GCVote INSTANCE = new GCVote();
    }
}
