package cgeo.geocaching.filters.core;

import cgeo.geocaching.SearchCacheData;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogEntryGeocacheFilterTest {

    private static final String AUTHOR = "Alice";
    private static final String LOG_TEXT = "needle";

    @Test
    public void onlineAuthorHintWithoutLogTextIsConclusive() {
        assertThat(createFilter(false, null).filter(createOnlineCache(false, Collections.emptyList()))).isTrue();
        assertThat(createFilter(true, null).filter(createOnlineCache(true, Collections.emptyList()))).isTrue();
    }

    @Test
    public void onlineAuthorHintWithLogTextRequiresMatchingLocalLog() {
        final LogEntryGeocacheFilter filter = createFilter(false, LOG_TEXT);

        assertThat(filter.filter(createOnlineCache(false, Collections.emptyList()))).isNull();
        assertThat(filter.filter(createOnlineCache(false, Collections.singletonList(log(AUTHOR, "other text"))))).isNull();
        assertThat(filter.filter(createOnlineCache(false, Collections.singletonList(log(AUTHOR, "contains needle here"))))).isTrue();
    }

    @Test
    public void includeInconclusiveControlsUnknownOnlineLogTextResult() {
        final LogEntryGeocacheFilter filter = createFilter(false, LOG_TEXT);
        final TestGeocache cache = createOnlineCache(false, Collections.emptyList());

        assertThat(GeocacheFilter.create(false, false, filter).filter(cache)).isFalse();
        assertThat(GeocacheFilter.create(false, true, filter).filter(cache)).isTrue();
    }

    @Test
    public void inverseOnlineAuthorHintWithLogTextRequiresLocalLogEvidence() {
        final LogEntryGeocacheFilter filter = createFilter(true, LOG_TEXT);

        assertThat(filter.filter(createOnlineCache(true, Collections.emptyList()))).isNull();
        assertThat(filter.filter(createOnlineCache(true, Collections.singletonList(log(AUTHOR, "other text"))))).isNull();
        assertThat(filter.filter(createOnlineCache(true,
                Collections.singletonList(log(AUTHOR, "contains needle here", LogType.DIDNT_FIND_IT))))).isFalse();
        assertThat(filter.filter(createOnlineCache(true,
                Collections.singletonList(log(AUTHOR, "contains needle here", LogType.NOTE))))).isFalse();
    }

    @Test
    public void locallyStoredLogsKeepExistingMatchSemantics() {
        final TestGeocache matchingCache = new TestGeocache(true, Arrays.asList(
                log("Other", LOG_TEXT), log(AUTHOR, "contains needle here")));
        final TestGeocache nonMatchingCache = new TestGeocache(true, Collections.singletonList(log(AUTHOR, "other text")));

        assertThat(createFilter(false, LOG_TEXT).filter(matchingCache)).isTrue();
        assertThat(createFilter(true, LOG_TEXT).filter(matchingCache)).isFalse();
        assertThat(createFilter(false, LOG_TEXT).filter(nonMatchingCache)).isFalse();
        assertThat(createFilter(true, LOG_TEXT).filter(nonMatchingCache)).isTrue();
    }

    @Test
    public void inverseAuthorOnlyFilterKeepsCompatibilityWithoutSearchMetadata() {
        final TestGeocache cache = new TestGeocache(false, Collections.emptyList());

        assertThat(createFilter(false, null).filter(cache)).isNull();
        assertThat(createFilter(true, null).filter(cache)).isTrue();
    }

    @Test
    public void missingLogsWithLogTextAreInconclusive() {
        final TestGeocache cache = new TestGeocache(false, Collections.emptyList());

        assertThat(createFilter(false, LOG_TEXT).filter(cache)).isNull();
        assertThat(createFilter(true, LOG_TEXT).filter(cache)).isNull();
    }

    private static LogEntryGeocacheFilter createFilter(final boolean inverse, final String logText) {
        final LogEntryGeocacheFilter filter = new LogEntryGeocacheFilter();
        filter.setFoundByUser(AUTHOR);
        filter.setLogText(logText);
        filter.setInverse(inverse);
        return filter;
    }

    private static TestGeocache createOnlineCache(final boolean inverse, final List<LogEntry> logs) {
        final SearchCacheData searchData = new SearchCacheData();
        if (inverse) {
            searchData.addNotFoundBy(AUTHOR);
        } else {
            searchData.addFoundBy(AUTHOR);
        }
        final TestGeocache cache = new TestGeocache(false, logs);
        cache.setSearchData(searchData);
        return cache;
    }

    private static LogEntry log(final String author, final String text) {
        return log(author, text, LogType.FOUND_IT);
    }

    private static LogEntry log(final String author, final String text, final LogType logType) {
        return new LogEntry.Builder().setAuthor(author).setLog(text).setLogType(logType).build();
    }

    private static class TestGeocache extends Geocache {

        private final boolean stored;
        private final List<LogEntry> logs;

        TestGeocache(final boolean stored, final List<LogEntry> logs) {
            this.stored = stored;
            this.logs = logs;
        }

        @Override
        public boolean inDatabase() {
            return stored;
        }

        @Override
        public List<LogEntry> getLogs() {
            return logs;
        }
    }
}
