package cgeo.geocaching.gcvote;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GCVote {
    private static final Pattern patternLogIn = Pattern.compile("loggedIn='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternGuid = Pattern.compile("cacheId='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternRating = Pattern.compile("voteAvg='([0-9.]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternVotes = Pattern.compile("voteCnt='([0-9]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternVote = Pattern.compile("voteUser='([0-9.]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternVoteElement = Pattern.compile("<vote ([^>]+)>", Pattern.CASE_INSENSITIVE);

    private static final int MAX_CACHED_RATINGS = 1000;
    private static LeastRecentlyUsedMap<String, GCVoteRating> ratingsCache = new LeastRecentlyUsedMap.LruCache<String, GCVoteRating>(MAX_CACHED_RATINGS);

    /**
     * Get user rating for a given guid or geocode. For a guid first the ratings cache is checked
     * before a request to gcvote.com is made.
     *
     * @param guid
     * @param geocode
     * @return
     */
    public static GCVoteRating getRating(String guid, String geocode) {
        List<String> guids = null;
        List<String> geocodes = null;

        if (StringUtils.isNotBlank(guid)) {

            GCVoteRating rating = ratingsCache.get(guid);
            if (rating != null) {
                return rating;
            }
            guids = new ArrayList<String>();
            guids.add(guid);
        } else if (StringUtils.isNotBlank(geocode)) {
            geocodes = new ArrayList<String>();
            geocodes.add(geocode);
        } else {
            return null;
        }

        final Map<String, GCVoteRating> ratings = getRating(guids, geocodes);
        if (ratings != null) {
            for (Entry<String, GCVoteRating> entry : ratings.entrySet()) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Get user ratings from gcvote.com
     *
     * @param guids
     * @param geocodes
     * @return
     */
    public static Map<String, GCVoteRating> getRating(List<String> guids, List<String> geocodes) {
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
            if (guids != null && !guids.isEmpty()) {
                params.put("cacheIds", StringUtils.join(guids.toArray(), ','));
            } else {
                params.put("waypoints", StringUtils.join(geocodes.toArray(), ','));
            }
            params.put("version", "cgeo");
            final String page = Network.getResponseData(Network.request("http://gcvote.com/getVotes.php", params, false, false, false));
            if (page == null) {
                return null;
            }

            String voteData = null;
            final Matcher matcherVoteElement = patternVoteElement.matcher(page);
            while (matcherVoteElement.find()) {
                voteData = matcherVoteElement.group(1);
                if (voteData == null) {
                    continue;
                }

                String guid = null;
                try {
                    final Matcher matcherGuid = patternGuid.matcher(voteData);
                    if (matcherGuid.find()) {
                        if (matcherGuid.groupCount() > 0) {
                            guid = matcherGuid.group(1);
                        }
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "GCVote.getRating: Failed to parse guid");
                }
                if (guid == null) {
                    continue;
                }

                boolean loggedIn = false;
                try {
                    final Matcher matcherLoggedIn = patternLogIn.matcher(page);
                    if (matcherLoggedIn.find()) {
                        if (matcherLoggedIn.groupCount() > 0) {
                            if (matcherLoggedIn.group(1).equalsIgnoreCase("true")) {
                                loggedIn = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "GCVote.getRating: Failed to parse loggedIn");
                }

                float rating = 0;
                try {
                    final Matcher matcherRating = patternRating.matcher(voteData);
                    if (matcherRating.find()) {
                        rating = Float.parseFloat(matcherRating.group(1));
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "GCVote.getRating: Failed to parse rating");
                }
                if (rating <= 0) {
                    continue;
                }

                int votes = -1;
                try {
                    final Matcher matcherVotes = patternVotes.matcher(voteData);
                    if (matcherVotes.find()) {
                        votes = Integer.parseInt(matcherVotes.group(1));
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "GCVote.getRating: Failed to parse vote count");
                }
                if (votes < 0) {
                    continue;
                }

                float myVote = 0;
                if (loggedIn) {
                    try {
                        final Matcher matcherVote = patternVote.matcher(voteData);
                        if (matcherVote.find()) {
                            myVote = Float.parseFloat(matcherVote.group(1));
                        }
                    } catch (Exception e) {
                        Log.w(Settings.tag, "GCVote.getRating: Failed to parse user's vote");
                    }
                }

                if (StringUtils.isNotBlank(guid)) {
                    GCVoteRating gcvoteRating = new GCVoteRating(rating, votes, myVote);
                    ratings.put(guid, gcvoteRating);
                    ratingsCache.put(guid, gcvoteRating);
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "GCVote.getRating: " + e.toString());
        }

        return ratings;
    }

    /**
     * Transmit user vote to gcvote.com
     *
     * @param cache
     * @param vote
     * @return
     */
    public static boolean setRating(cgCache cache, double vote) {
        if (!cache.supportsGCVote()) {
            return false;
        }
        String guid = cache.getGuid();
        if (StringUtils.isBlank(guid)) {
            return false;
        }
        if (vote <= 0.0 || vote > 5.0) {
            return false;
        }

        final ImmutablePair<String, String> login = Settings.getGCvoteLogin();
        if (login == null) {
            return false;
        }

        final Parameters params = new Parameters(
                "userName", login.left,
                "password", login.right,
                "cacheId", guid,
                "voteUser", String.format("%.1f", vote).replace(',', '.'),
                "version", "cgeo");

        final String result = Network.getResponseData(Network.request("http://gcvote.com/setVote.php", params, false, false, false));

        return result.trim().equalsIgnoreCase("ok");
    }

    public static void loadRatings(ArrayList<cgCache> caches) {
        if (!Settings.isRatingWanted()) {
            return;
        }

        final ArrayList<String> guids = new ArrayList<String>(caches.size());
        for (final cgCache cache : caches) {
            String guid = cache.getGuid();
            if (StringUtils.isNotBlank(guid)) {
                guids.add(guid);
            }
        }

        if (guids.isEmpty()) {
            return;
        }

        try {
            final Map<String, GCVoteRating> ratings = GCVote.getRating(guids, null);

            if (MapUtils.isNotEmpty(ratings)) {
                // save found cache coordinates
                for (cgCache cache : caches) {
                    if (ratings.containsKey(cache.getGuid())) {
                        GCVoteRating rating = ratings.get(cache.getGuid());

                        cache.setRating(rating.getRating());
                        cache.setVotes(rating.getVotes());
                        cache.setMyVote(rating.getMyVote());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "GCvote.loadRatings: " + e.toString());
        }
    }
}
