package cgeo.geocaching;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import android.util.Log;

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

    public static cgRating getRating(String guid, String geocode) {
        List<String> guids = null;
        List<String> geocodes = null;

        if (StringUtils.isNotBlank(guid)) {
            guids = new ArrayList<String>();
            guids.add(guid);
        } else if (StringUtils.isNotBlank(geocode)) {
            geocodes = new ArrayList<String>();
            geocodes.add(geocode);
        } else {
            return null;
        }

        final Map<String, cgRating> ratings = getRating(guids, geocodes);
        if (ratings != null) {
            for (Entry<String, cgRating> entry : ratings.entrySet()) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static Map<String, cgRating> getRating(List<String> guids, List<String> geocodes) {
        if (guids == null && geocodes == null) {
            return null;
        }

        final Map<String, cgRating> ratings = new HashMap<String, cgRating>();

        try {
            final Parameters params = new Parameters();
            if (Settings.isLogin()) {
                final ImmutablePair<String, String> login = Settings.getGCvoteLogin();
                if (login != null) {
                    params.put("userName", login.left, "password", login.right);
                }
            }
            if (CollectionUtils.isNotEmpty(guids)) {
                params.put("cacheIds", StringUtils.join(guids.toArray(), ','));
            } else {
                params.put("waypoints", StringUtils.join(geocodes.toArray(), ','));
            }
            params.put("version", "cgeo");
            final String votes = cgBase.getResponseData(cgBase.request("http://gcvote.com/getVotes.php", params, false, false, false));
            if (votes == null) {
                return null;
            }

            String voteData = null;
            final Matcher matcherVoteElement = patternVoteElement.matcher(votes);
            while (matcherVoteElement.find()) {
                if (matcherVoteElement.groupCount() > 0) {
                    voteData = matcherVoteElement.group(1);
                }

                if (voteData == null) {
                    continue;
                }

                String guid = null;
                cgRating rating = new cgRating();
                boolean loggedIn = false;

                try {
                    final Matcher matcherGuid = patternGuid.matcher(voteData);
                    if (matcherGuid.find()) {
                        if (matcherGuid.groupCount() > 0) {
                            guid = matcherGuid.group(1);
                        }
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgBase.getRating: Failed to parse guid");
                }

                try {
                    final Matcher matcherLoggedIn = patternLogIn.matcher(votes);
                    if (matcherLoggedIn.find()) {
                        if (matcherLoggedIn.groupCount() > 0) {
                            if (matcherLoggedIn.group(1).equalsIgnoreCase("true")) {
                                loggedIn = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgBase.getRating: Failed to parse loggedIn");
                }

                try {
                    final Matcher matcherRating = patternRating.matcher(voteData);
                    if (matcherRating.find()) {
                        if (matcherRating.groupCount() > 0) {
                            rating.rating = Float.parseFloat(matcherRating.group(1));
                        }
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgBase.getRating: Failed to parse rating");
                }

                try {
                    final Matcher matcherVotes = patternVotes.matcher(voteData);
                    if (matcherVotes.find()) {
                        if (matcherVotes.groupCount() > 0) {
                            rating.votes = Integer.parseInt(matcherVotes.group(1));
                        }
                    }
                } catch (Exception e) {
                    Log.w(Settings.tag, "cgBase.getRating: Failed to parse vote count");
                }

                if (loggedIn) {
                    try {
                        final Matcher matcherVote = patternVote.matcher(voteData);
                        if (matcherVote.find()) {
                            if (matcherVote.groupCount() > 0) {
                                rating.myVote = Float.parseFloat(matcherVote.group(1));
                            }
                        }
                    } catch (Exception e) {
                        Log.w(Settings.tag, "cgBase.getRating: Failed to parse user's vote");
                    }
                }

                if (StringUtils.isNotBlank(guid)) {
                    ratings.put(guid, rating);
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.getRating: " + e.toString());
        }

        return ratings;
    }

    public static boolean setRating(cgCache cache, double vote) {
        if (!cache.supportsGCVote()) {
            return false;
        }
        String guid = cache.guid;
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

        final String result = cgBase.getResponseData(cgBase.request("http://gcvote.com/setVote.php", params, false, false, false));

        return result.trim().equalsIgnoreCase("ok");
    }
}
