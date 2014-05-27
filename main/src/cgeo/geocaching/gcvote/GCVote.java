package cgeo.geocaching.gcvote;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class GCVote {
    public static final float NO_RATING = 0;
    private static final Pattern PATTERN_LOG_IN = Pattern.compile("loggedIn='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_GUID = Pattern.compile("cacheId='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_WAYPOINT = Pattern.compile("waypoint='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_RATING = Pattern.compile("voteAvg='([0-9.]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_VOTES = Pattern.compile("voteCnt='([0-9]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_VOTE = Pattern.compile("voteUser='([0-9.]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_VOTE_ELEMENT = Pattern.compile("<vote ([^>]+)>", Pattern.CASE_INSENSITIVE);

    private static final int MAX_CACHED_RATINGS = 1000;
    private static final LeastRecentlyUsedMap<String, GCVoteRating> RATINGS_CACHE = new LeastRecentlyUsedMap.LruCache<String, GCVoteRating>(MAX_CACHED_RATINGS);
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
    private static Map<String, GCVoteRating> getRating(final List<String> guids, final List<String> geocodes) {
        if (guids == null && geocodes == null) {
            return null;
        }

        final Map<String, GCVoteRating> ratings = new HashMap<String, GCVoteRating>();

        try {
            final Parameters params = new Parameters();
            if (Settings.isLogin()) {
                final ImmutablePair<String, String> login = Settings.getGCvoteLogin();
                if (login != null) {
                    params.put("userName", login.left, "password", login.right);
                }
            }
            // use guid or gccode for lookup
            boolean requestByGuids = true;
            if (guids != null && !guids.isEmpty()) {
                params.put("cacheIds", StringUtils.join(guids.toArray(), ','));
            } else {
                params.put("waypoints", StringUtils.join(geocodes.toArray(), ','));
                requestByGuids = false;
            }
            params.put("version", "cgeo");
            final String page = Network.getResponseData(Network.getRequest("http://gcvote.com/getVotes.php", params));
            if (page == null) {
                return null;
            }

            final MatcherWrapper matcherVoteElement = new MatcherWrapper(PATTERN_VOTE_ELEMENT, page);
            while (matcherVoteElement.find()) {
                String voteData = matcherVoteElement.group(1);
                if (voteData == null) {
                    continue;
                }

                String id = null;
                String guid = null;
                final MatcherWrapper matcherGuid = new MatcherWrapper(PATTERN_GUID, voteData);
                if (matcherGuid.find()) {
                    if (matcherGuid.groupCount() > 0) {
                        guid = matcherGuid.group(1);
                        if (requestByGuids) {
                            id = guid;
                        }
                    }
                }
                if (!requestByGuids) {
                    final MatcherWrapper matcherWp = new MatcherWrapper(PATTERN_WAYPOINT, voteData);
                    if (matcherWp.find()) {
                        if (matcherWp.groupCount() > 0) {
                            id = matcherWp.group(1);
                        }
                    }
                }
                if (id == null) {
                    continue;
                }

                boolean loggedIn = false;
                final MatcherWrapper matcherLoggedIn = new MatcherWrapper(PATTERN_LOG_IN, page);
                if (matcherLoggedIn.find()) {
                    if (matcherLoggedIn.groupCount() > 0) {
                        if (matcherLoggedIn.group(1).equalsIgnoreCase("true")) {
                            loggedIn = true;
                        }
                    }
                }

                float rating = NO_RATING;
                try {
                    final MatcherWrapper matcherRating = new MatcherWrapper(PATTERN_RATING, voteData);
                    if (matcherRating.find()) {
                        rating = Float.parseFloat(matcherRating.group(1));
                    }
                } catch (NumberFormatException e) {
                    Log.w("GCVote.getRating: Failed to parse rating");
                }
                if (!isValidRating(rating)) {
                    continue;
                }

                int votes = -1;
                try {
                    final MatcherWrapper matcherVotes = new MatcherWrapper(PATTERN_VOTES, voteData);
                    if (matcherVotes.find()) {
                        votes = Integer.parseInt(matcherVotes.group(1));
                    }
                } catch (NumberFormatException e) {
                    Log.w("GCVote.getRating: Failed to parse vote count");
                }
                if (votes < 0) {
                    continue;
                }

                float myVote = NO_RATING;
                if (loggedIn) {
                    try {
                        final MatcherWrapper matcherVote = new MatcherWrapper(PATTERN_VOTE, voteData);
                        if (matcherVote.find()) {
                            myVote = Float.parseFloat(matcherVote.group(1));
                        }
                    } catch (NumberFormatException e) {
                        Log.w("GCVote.getRating: Failed to parse user's vote");
                    }
                }

                if (StringUtils.isNotBlank(id)) {
                    GCVoteRating gcvoteRating = new GCVoteRating(rating, votes, myVote);
                    ratings.put(id, gcvoteRating);
                    RATINGS_CACHE.put(guid, gcvoteRating);
                }
            }
        } catch (RuntimeException e) {
            Log.e("GCVote.getRating", e);
        }

        return ratings;
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

            if (MapUtils.isNotEmpty(ratings)) {
                // save found cache coordinates
                for (Geocache cache : caches) {
                    if (ratings.containsKey(cache.getGeocode())) {
                        GCVoteRating rating = ratings.get(cache.getGeocode());

                        cache.setRating(rating.getRating());
                        cache.setVotes(rating.getVotes());
                        cache.setMyVote(rating.getMyVote());
                    }
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
        final ArrayList<String> geocodes = new ArrayList<String>(caches.size());
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

    public static String getRatingText(final float rating) {
        return String.format(Locale.getDefault(), "%.1f", rating);
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
