package cgeo.geocaching.gcvote;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GCVote {
    public static final float NO_RATING = 0;

    private static final int MAX_CACHED_RATINGS = 1000;
    private static final LeastRecentlyUsedMap<String, GCVoteRating> RATINGS_CACHE = new LeastRecentlyUsedMap.LruCache<>(MAX_CACHED_RATINGS);
    private static final float MIN_RATING = 1;
    private static final float MAX_RATING = 5;

    private GCVote() {
        // utility class
    }

    /**
     * Get user rating for a given guid or geocode. For a guid first the ratings cache is checked
     * before a request to gcvote.com is made.
     *
     * @param guid
     * @param geocode
     * @return
     */
    public static GCVoteRating getRating(final String guid, final String geocode) {
        if (StringUtils.isNotBlank(guid) && RATINGS_CACHE.containsKey(guid)) {
            return RATINGS_CACHE.get(guid);
        }

        final Map<String, GCVoteRating> ratings = getRating(singletonOrNull(guid), singletonOrNull(geocode));
        return MapUtils.isNotEmpty(ratings) ? ratings.values().iterator().next() : null;
    }

    private static List<String> singletonOrNull(final String item) {
        return StringUtils.isNotBlank(item) ? Collections.singletonList(item) : null;
    }

    /**
     * Get user ratings from gcvote.com
     *
     * @param guids
     * @param geocodes
     * @return
     */
    @NonNull
    private static Map<String, GCVoteRating> getRating(final List<String> guids, final List<String> geocodes) {
        if (guids == null && geocodes == null) {
            return Collections.emptyMap();
        }

        final Parameters params = new Parameters("version", "cgeo");
        final ImmutablePair<String, String> login = Settings.getGCvoteLogin();
        if (login != null) {
            params.put("userName", login.left, "password", login.right);
        }

        // use guid or gccode for lookup
        final boolean requestByGuids = CollectionUtils.isNotEmpty(guids);
        if (requestByGuids) {
            params.put("cacheIds", StringUtils.join(guids, ','));
        } else {
            params.put("waypoints", StringUtils.join(geocodes, ','));
        }
        final InputStream response = Network.getResponseStream(Network.getRequest("http://gcvote.com/getVotes.php", params));
        if (response == null) {
            return Collections.emptyMap();
        }
        return getRatingsFromXMLResponse(response, requestByGuids);
    }

    static Map<String, GCVoteRating> getRatingsFromXMLResponse(@NonNull final InputStream response, final boolean requestByGuids) {
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(response, Charsets.UTF_8.name());
            boolean loggedIn = false;
            final Map<String, GCVoteRating> ratings = new HashMap<>();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    final String tagName = xpp.getName();
                    if (StringUtils.equals(tagName, "vote")) {
                        final String guid = xpp.getAttributeValue(null, "cacheId");
                        final String id = requestByGuids ? guid : xpp.getAttributeValue(null, "waypoint");
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
        } catch (final Exception e) {
            Log.e("Cannot parse GC vote result", e);
            return Collections.emptyMap();

        }
    }

    /**
     * Transmit user vote to gcvote.com
     *
     * @param cache
     * @param vote
     * @return {@code true} if the rating was submitted successfully
     */
    public static boolean setRating(final Geocache cache, final float vote) {
        if (!isVotingPossible(cache)) {
            return false;
        }
        if (!isValidRating(vote)) {
            return false;
        }

        final ImmutablePair<String, String> login = Settings.getGCvoteLogin();
        if (login == null) {
            return false;
        }

        final Parameters params = new Parameters(
                "userName", login.left,
                "password", login.right,
                "cacheId", cache.getGuid(),
                "voteUser", String.format("%.1f", vote).replace(',', '.'),
                "version", "cgeo");

        final String result = Network.getResponseData(Network.getRequest("http://gcvote.com/setVote.php", params));

        return result != null && result.trim().equalsIgnoreCase("ok");
    }

    public static void loadRatings(final @NonNull ArrayList<Geocache> caches) {
        if (!Settings.isRatingWanted()) {
            return;
        }

        final ArrayList<String> geocodes = getVotableGeocodes(caches);
        if (geocodes.isEmpty()) {
            return;
        }

        try {
            final Map<String, GCVoteRating> ratings = GCVote.getRating(null, geocodes);

            // save found cache coordinates
            for (Geocache cache : caches) {
                if (ratings.containsKey(cache.getGeocode())) {
                    GCVoteRating rating = ratings.get(cache.getGeocode());

                    cache.setRating(rating.getRating());
                    cache.setVotes(rating.getVotes());
                    cache.setMyVote(rating.getMyVote());
                }
            }
        } catch (Exception e) {
            Log.e("GCvote.loadRatings", e);
        }
    }

    /**
     * Get geocodes of all the caches, which can be used with GCVote. Non-GC caches will be filtered out.
     *
     * @param caches
     * @return
     */
    private static @NonNull
    ArrayList<String> getVotableGeocodes(final @NonNull Collection<Geocache> caches) {
        final ArrayList<String> geocodes = new ArrayList<>(caches.size());
        for (final Geocache cache : caches) {
            String geocode = cache.getGeocode();
            if (StringUtils.isNotBlank(geocode) && cache.supportsGCVote()) {
                geocodes.add(geocode);
            }
        }
        return geocodes;
    }

    public static boolean isValidRating(final float rating) {
        return rating >= MIN_RATING && rating <= MAX_RATING;
    }

    public static boolean isVotingPossible(final Geocache cache) {
        return Settings.isGCvoteLogin() && StringUtils.isNotBlank(cache.getGuid()) && cache.supportsGCVote();
    }

    public static String getDescription(final float rating) {
        switch (Math.round(rating * 2f)) {
            case 2:
                return getString(R.string.log_stars_1_description);
            case 3:
                return getString(R.string.log_stars_15_description);
            case 4:
                return getString(R.string.log_stars_2_description);
            case 5:
                return getString(R.string.log_stars_25_description);
            case 6:
                return getString(R.string.log_stars_3_description);
            case 7:
                return getString(R.string.log_stars_35_description);
            case 8:
                return getString(R.string.log_stars_4_description);
            case 9:
                return getString(R.string.log_stars_45_description);
            case 10:
                return getString(R.string.log_stars_5_description);
            default:
                return getString(R.string.log_no_rating);
        }
    }

    private static String getString(int resId) {
        return CgeoApplication.getInstance().getString(resId);
    }

}
