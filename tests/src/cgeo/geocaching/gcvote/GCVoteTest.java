package cgeo.geocaching.gcvote;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class GCVoteTest extends AbstractResourceInstrumentationTestCase {

    private String response;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        response = getFileContent(R.raw.gcvote);
    }

    public void testGetRatingsByGeocode() {
        final Map<String, GCVoteRating> ratings = GCVote.getRatingsFromXMLResponse(response, false);
        assertThat(ratings).hasSize(10);
        assertThat(ratings).containsKey("GCKF13");
        assertThat(ratings.get("GC1WEVZ")).isEqualToComparingFieldByField(new GCVoteRating(3.75f, 2, 0));
    }

    public void testGetRatingsByGuid() {
        final Map<String, GCVoteRating> ratings = GCVote.getRatingsFromXMLResponse(response, true);
        assertThat(ratings).hasSize(10);
        assertThat(ratings).containsKey("a02894bb-4a08-4c09-a73c-25939894ba15");
        assertThat(ratings.get("5520c33b-3941-45ca-9056-ea655dbaadf7")).isEqualToComparingFieldByField(new GCVoteRating(3.75f, 2, 0));
    }

    public void testBenchmark() {
        benchmarkXML(1000);
        benchmarkRegex(1000);
    }

    public void testCompareResults() {
        for (int i = 0; i < 2; i++) {
            final boolean requestByGuids = i == 0;
            final Map<String, GCVoteRating> xmlRatings = GCVote.getRatingsFromXMLResponse(response, requestByGuids);
            final Map<String, GCVoteRating> regexRatings = getRatingsRegex(response, requestByGuids);
            assertThat(xmlRatings.keySet()).containsExactlyElementsOf(regexRatings.keySet());
            for (final Entry<String, GCVoteRating> entry: xmlRatings.entrySet()) {
                assertThat(entry.getValue()).isEqualToComparingFieldByField(regexRatings.get(entry.getKey()));
            }
        }
    }

    private void benchmarkXML(final int occurrences) {
        final long start = System.currentTimeMillis();
        for (int i = 0; i < occurrences; i++) {
            GCVote.getRatingsFromXMLResponse(response, false);
        }
        Log.d("XML GCVote parsing (current) in ms (" + occurrences + " times): " + (System.currentTimeMillis() - start));
    }

    private void benchmarkRegex(final int occurrences) {
        final long start = System.currentTimeMillis();
        for (int i = 0; i < occurrences; i++) {
            getRatingsRegex(response, false);
        }
        Log.d("Regex GCVote parsing (old) in ms (" + occurrences + " times): " + (System.currentTimeMillis() - start));
    }

    public static final float NO_RATING = 0;
    private static final Pattern PATTERN_LOG_IN = Pattern.compile("loggedIn='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_GUID = Pattern.compile("cacheId='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_WAYPOINT = Pattern.compile("waypoint='([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_RATING = Pattern.compile("voteAvg='([0-9.]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_VOTES = Pattern.compile("voteCnt='([0-9]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_VOTE = Pattern.compile("voteUser='([0-9.]+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_VOTE_ELEMENT = Pattern.compile("<vote ([^>]+)>", Pattern.CASE_INSENSITIVE);

    private static final int MAX_CACHED_RATINGS = 1000;
    private static final LeastRecentlyUsedMap<String, GCVoteRating> RATINGS_CACHE = new LeastRecentlyUsedMap.LruCache<>(MAX_CACHED_RATINGS);

    private static Map<String, GCVoteRating> getRatingsRegex(final String page, final boolean requestByGuids) {
        final Map<String, GCVoteRating> ratings = new HashMap<>();

        try {
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
                    Log.w("GCVote.getRating: Failed to parse rating", e);
                }
                if (!GCVote.isValidRating(rating)) {
                    continue;
                }

                int votes = -1;
                try {
                    final MatcherWrapper matcherVotes = new MatcherWrapper(PATTERN_VOTES, voteData);
                    if (matcherVotes.find()) {
                        votes = Integer.parseInt(matcherVotes.group(1));
                    }
                } catch (NumberFormatException e) {
                    Log.w("GCVote.getRating: Failed to parse vote count", e);
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
                        Log.w("GCVote.getRating: Failed to parse user's vote", e);
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
}
