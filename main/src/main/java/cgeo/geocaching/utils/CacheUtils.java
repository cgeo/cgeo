package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogUtils;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class CacheUtils {

    private static final int HEALTH_SCORE_MAX_LOGS_TO_CONSIDER = 20;

    /** Log types that contribute positively to the health score. */
    private static final Set<LogType> HEALTH_SCORE_GOOD_LOG_TYPES = EnumSet.of(
            LogType.FOUND_IT, LogType.ATTENDED, LogType.WEBCAM_PHOTO_TAKEN);

    /** Log types that contribute negatively to the health score. */
    private static final Set<LogType> HEALTH_SCORE_BAD_LOG_TYPES = EnumSet.of(
            LogType.DIDNT_FIND_IT, LogType.NEEDS_MAINTENANCE, LogType.NEEDS_ARCHIVE);

    /**
     * Log types that act as a "fresh start": the algorithm stops consuming older logs when it
     * encounters one of these types.
     */
    private static final Set<LogType> HEALTH_SCORE_RESET_LOG_TYPES = EnumSet.of(
            LogType.OWNER_MAINTENANCE, LogType.ENABLE_LISTING, LogType.UNARCHIVE, LogType.ARCHIVE,
            LogType.TEMP_DISABLE_LISTING);

    /**
     * Log types that act as a reset "nullifier": if found before any other RESET type, health is 0%.
     */
    private static final Set<LogType> HEALTH_SCORE_RESET_NULLIFY_LOG_TYPES = EnumSet.of(
            LogType.ARCHIVE, LogType.TEMP_DISABLE_LISTING);

    private CacheUtils() {
        // utility class
    }

    /**
     * Calculates the log health score for a cache.
     *
     * @param logs           Log entries for the cache (may be in any order; at most 20 are used).
     * @param detailedUpdate Timestamp (ms since epoch) when log data was last fetched from server.
     *                       Not used for computation — kept for future use / API consistency.
     * @return Integer in [0, 100] representing the health score in percent, or
     *         {@link Geocache#HEALTH_SCORE_UNKNOWN} (−1) when the candidate set is empty or
     *         contains no good/bad logs.  Never returns {@code null}.
     */
    @NonNull
    public static Integer calculateHealthScore(@NonNull final List<LogEntry> logs, final long detailedUpdate) {
        // Step 1: sort descending by date, keep at most 20
        final List<LogEntry> sorted = new ArrayList<>(logs);
        sorted.sort(LogUtils.LOG_ENTRY_DATE_COMPARATOR);

        // Walk from newest to oldest, stop before the first reset trigger
        int candidateEnd = Math.min(sorted.size(), HEALTH_SCORE_MAX_LOGS_TO_CONSIDER);
        for (int i = 0; i < candidateEnd; i++) {
            if (HEALTH_SCORE_RESET_LOG_TYPES.contains(sorted.get(i).logType)) {

                if (HEALTH_SCORE_RESET_NULLIFY_LOG_TYPES.contains(sorted.get(i).logType)) {
                    return 0; // nullifying reset trigger found, no need to look further
                }

                candidateEnd = i; // exclusive: stop before the reset trigger
                break;
            }
        }

        if (candidateEnd == 0) {
            return Geocache.HEALTH_SCORE_UNKNOWN;
        }


        // Step 2: find oldest relevant date
        long oldestDay = Long.MAX_VALUE;
        for (int i = candidateEnd - 1; i >= 0; i--) {
            final LogEntry log = sorted.get(i);
            if (HEALTH_SCORE_GOOD_LOG_TYPES.contains(log.logType) || HEALTH_SCORE_BAD_LOG_TYPES.contains(log.logType)) {
                oldestDay = log.getLogAgeInDaysSinceEpochZoneCorrected();
                break;
            }
        }

        if (oldestDay == Long.MAX_VALUE) {
            return Geocache.HEALTH_SCORE_UNKNOWN; // no good/bad logs
        }

        // Step 3: compute weighted sums
        double wGood = 0;
        double wBad = 0;
        for (int i = 0; i < candidateEnd; i++) {
            final LogEntry log = sorted.get(i);
            final boolean isGood = HEALTH_SCORE_GOOD_LOG_TYPES.contains(log.logType);
            final boolean isBad = HEALTH_SCORE_BAD_LOG_TYPES.contains(log.logType);
            if (!isGood && !isBad) {
                continue;
            }
            final long day = log.getLogAgeInDaysSinceEpochZoneCorrected();
            final double age = day - oldestDay; // >= 0
            final double weight = Math.sqrt(age + 1); // >= 1
            if (isGood) {
                wGood += weight;
            } else {
                wBad += weight;
            }
        }

        //Step 4: calculate score as percentage of good weight over total weight
        final double wTotal = wGood + wBad;
        if (wTotal == 0) {
            return Geocache.HEALTH_SCORE_UNKNOWN;
        }

        return (int) Math.round(wGood / wTotal * 100);
    }

    public static String getLogHealthScoreExplanationAsMarkDown() {
        return LocalizationUtils.getString(R.string.log_health_score_explanation,
            HEALTH_SCORE_MAX_LOGS_TO_CONSIDER,
            TextUtils.join(HEALTH_SCORE_RESET_LOG_TYPES, LogType::getL10n, ", "),
            TextUtils.join(HEALTH_SCORE_GOOD_LOG_TYPES, LogType::getL10n, ", "),
            TextUtils.join(HEALTH_SCORE_BAD_LOG_TYPES, LogType::getL10n, ", "));
    }

    public static boolean isLabAdventure(@NonNull final Geocache cache) {
        return cache.getType() == CacheType.ADVLAB && StringUtils.isNotEmpty(cache.getUrl());
    }

    public static boolean isLabPlayerInstalled(@NonNull final Activity activity) {
        return null != ProcessUtils.getLaunchIntent(LocalizationUtils.getPlainString(R.string.package_alc));
    }

    public static void setLabLink(@NonNull final Activity activity, @NonNull final View view, @Nullable final String url) {
        view.setOnClickListener(v -> {
            // re-check installation state, might have changed since creating the view
            if (isLabPlayerInstalled(activity) && StringUtils.isNotBlank(url)) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } else {
                ProcessUtils.openMarket(activity, LocalizationUtils.getPlainString(R.string.package_alc));
            }
        });
    }

    /**
     * Find links to Adventure Labs in Listing of a cache. Returns URL if exactly 1 link target is found, else null.
     * 3 types of URLs possible: https://adventurelab.page.link/Cw3L, https://labs.geocaching.com/goto/Theater, https://labs.geocaching.com/goto/a4b45b7b-fa76-4387-a54f-045875ffee0c
     */
    @Nullable
    public static String findAdvLabUrl(final Geocache cache) {
        final Pattern patternAdvLabUrl = Pattern.compile("(https?://labs.geocaching.com/goto/[a-zA-Z0-9-_]{1,36}|https?://adventurelab.page.link/[a-zA-Z0-9]{4})");
        final Matcher matcher = patternAdvLabUrl.matcher(cache.getShortDescription() + " " + cache.getDescription());
        final Set<String> urls = new HashSet<>();
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        if (urls.size() == 1) {
            return urls.iterator().next();
        }
        return null;
    }

    /**
     * Returns a Pair of (title, message) for the hint/personal-note dialog.
     * title combines "Hint" and/or "Personal note" labels separated by " / ".
     * message combines hint and/or personal note text separated by a divider line.
     * Message may be empty if neither hint nor personal note is set.
     */
    @NonNull
    public static Pair<CharSequence, CharSequence> getHintTitleAndMessage(@Nullable final Geocache cache) {
        if (cache == null) {
            return Pair.create("", "");
        }

        final List<String> titleList = new ArrayList<>();
        final List<String> messageList = new ArrayList<>();

        final String hint = cache.getHint();
        final boolean hasHint = StringUtils.isNotEmpty(hint);
        if (hasHint) {
            titleList.add(LocalizationUtils.getString(R.string.cache_hint));
            messageList.add(hint);
        }

        final String personalNote = cache.getPersonalNote();
        final boolean hasPersonalNote = StringUtils.isNotEmpty(personalNote);
        if (hasPersonalNote) {
            titleList.add(LocalizationUtils.getString(R.string.cache_personal_note));
            messageList.add(personalNote);
        }

        final CharSequence title = TextUtils.join(titleList, s -> s, " / ");
        final CharSequence message = TextUtils.join(messageList, s -> s, "\r\n──────────\r\n");

        return Pair.create(title, message);
    }
}
