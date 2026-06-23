package cgeo.geocaching.utils;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CacheUtilsTest {

    private static final long DAY_MS = 86_400_000L;

    private static LogEntry log(final LogType type, final long daysAgo) {
        return new LogEntry.Builder()
                .setLogType(type)
                .setDate(System.currentTimeMillis() - daysAgo * DAY_MS)
                .build();
    }

    @Test
    public void testCalculateHealthScoreEmptyLogsReturnsUnknown() {
        final Integer score = CacheUtils.calculateHealthScore(Collections.emptyList(), 0);
        assertThat(score).isEqualTo(Geocache.HEALTH_SCORE_UNKNOWN);
    }

    @Test
    public void testCalculateHealthScoreNeutralOnlyReturnsUnknown() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.NOTE, 1));
        logs.add(log(LogType.NOTE, 2));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(Geocache.HEALTH_SCORE_UNKNOWN);
    }

    @Test
    public void testCalculateHealthScoreAllGoodReturns100() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.FOUND_IT, 1));
        logs.add(log(LogType.FOUND_IT, 2));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(100);
    }

    @Test
    public void testCalculateHealthScoreAllBadReturns0() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.DIDNT_FIND_IT, 1));
        logs.add(log(LogType.DIDNT_FIND_IT, 2));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(0);
    }

    @Test
    public void testCalculateHealthScoreMixedLogs() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.FOUND_IT, 1));
        logs.add(log(LogType.DIDNT_FIND_IT, 2));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        // both logs should produce a score between 0 and 100
        assertThat(score).isGreaterThan(0).isLessThan(100);
    }

    @Test
    public void testCalculateHealthScoreResetByTemporarilyDisabled() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.DIDNT_FIND_IT, 1));
        logs.add(log(LogType.TEMP_DISABLE_LISTING, 2));
        logs.add(log(LogType.FOUND_IT, 3));
        // FOUND_IT is before the reset trigger, so only DNF counts
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(0);
    }

    @Test
    public void testCalculateHealthScoreResetTriggerAsFirstLogReturnsUnknown() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.ENABLE_LISTING, 1));
        logs.add(log(LogType.FOUND_IT, 2));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(Geocache.HEALTH_SCORE_UNKNOWN);
    }

    @Test
    public void testCalculateHealthScoreNullifyingTriggerAsFirstResetReturnsZero() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.FOUND_IT, 1));
        logs.add(log(LogType.TEMP_DISABLE_LISTING, 2));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(0);
    }


    @Test
    public void testCalculateHealthScoreOwnerMaintenanceAsFirstLogReturnsUnknown() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.OWNER_MAINTENANCE, 1));
        logs.add(log(LogType.DIDNT_FIND_IT, 2));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(Geocache.HEALTH_SCORE_UNKNOWN);
    }

    @Test
    public void testCalculateHealthScoreTruncatesAt20Logs() {
        final List<LogEntry> logs = new ArrayList<>();
        // 20 newest are DNFs, 5 older are found-its; truncation should exclude the found-its
        for (int i = 1; i <= 20; i++) {
            logs.add(log(LogType.DIDNT_FIND_IT, i));
        }
        for (int i = 21; i <= 25; i++) {
            logs.add(log(LogType.FOUND_IT, i));
        }
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        // Only the 20 DNFs are counted; the 5 FIs are truncated → score must be 0
        assertThat(score).isEqualTo(0);
    }

    @Test
    public void testCalculateHealthScoreSingleGoodLog() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.FOUND_IT, 5));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(100);
    }

    @Test
    public void testCalculateHealthScoreSingleBadLog() {
        final List<LogEntry> logs = new ArrayList<>();
        logs.add(log(LogType.DIDNT_FIND_IT, 5));
        final Integer score = CacheUtils.calculateHealthScore(logs, 0);
        assertThat(score).isEqualTo(0);
    }

    @Test
    public void testCalculateHealthScoreNeverReturnsNull() {
        final Integer score = CacheUtils.calculateHealthScore(Collections.emptyList(), 0);
        assertThat(score).isNotNull();
    }

    @Test
    public void testCalculateHealthScoreRecentFoundBoostsScore() {
        // A recent found should score higher than only an older found with a recent DNF
        final List<LogEntry> goodLogs = new ArrayList<>();
        goodLogs.add(log(LogType.FOUND_IT, 1));
        goodLogs.add(log(LogType.DIDNT_FIND_IT, 10));

        final List<LogEntry> badLogs = new ArrayList<>();
        badLogs.add(log(LogType.DIDNT_FIND_IT, 1));
        badLogs.add(log(LogType.FOUND_IT, 10));

        final Integer goodScore = CacheUtils.calculateHealthScore(goodLogs, 0);
        final Integer badScore = CacheUtils.calculateHealthScore(badLogs, 0);
        assertThat(goodScore).isGreaterThan(badScore);
    }
}
