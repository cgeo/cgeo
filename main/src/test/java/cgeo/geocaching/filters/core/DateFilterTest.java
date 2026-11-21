package cgeo.geocaching.filters.core;

import cgeo.geocaching.storage.SqlBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Tests the shared calendar-date semantics of in-memory and SQL date filters. */
public class DateFilterTest {

    private TimeZone originalTimeZone;

    @Before
    public void rememberTimeZone() {
        originalTimeZone = TimeZone.getDefault();
    }

    @After
    public void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void absoluteDayUsesTokyoBoundariesInMemoryAndSql() {
        assertAbsoluteDayBoundaries("Asia/Tokyo", LocalDate.of(2025, 1, 1));
    }

    @Test
    public void absoluteDayUsesLosAngelesBoundariesInMemoryAndSql() {
        assertAbsoluteDayBoundaries("America/Los_Angeles", LocalDate.of(2025, 1, 1));
    }

    @Test
    public void absoluteDayUsesDstAwareBoundaries() {
        final ZoneId zone = setTimeZone("America/New_York");

        final DateFilter springFilter = filterForDay(zone, LocalDate.of(2024, 3, 10));
        assertThat(springFilter.getMaxDate().getTime() - springFilter.getMinDate().getTime() + 1)
                .isEqualTo(Duration.ofHours(23).toMillis());
        assertRangeBoundaries(springFilter);

        final DateFilter autumnFilter = filterForDay(zone, LocalDate.of(2024, 11, 3));
        assertThat(autumnFilter.getMaxDate().getTime() - autumnFilter.getMinDate().getTime() + 1)
                .isEqualTo(Duration.ofHours(25).toMillis());
        assertRangeBoundaries(autumnFilter);
    }

    @Test
    public void absoluteOneSidedRangesUseSameInMemoryAndSqlBoundaries() {
        final ZoneId zone = setTimeZone("America/Los_Angeles");
        final LocalDate selectedDay = LocalDate.of(2025, 2, 12);
        final Date selected = atLocalTime(zone, selectedDay, LocalTime.NOON);

        final DateFilter minOnly = new DateFilter();
        minOnly.setMinMaxDate(selected, null);
        assertThat(minOnly.matches(new Date(minOnly.getMinDate().getTime() - 1))).isFalse();
        assertThat(minOnly.matches(minOnly.getMinDate())).isTrue();
        assertThat(sqlFor(minOnly)).contains("t.hidden >= " + minOnly.getMinDate().getTime()).doesNotContain("t.hidden <=");

        final DateFilter maxOnly = new DateFilter();
        maxOnly.setMinMaxDate(null, selected);
        assertThat(maxOnly.matches(maxOnly.getMaxDate())).isTrue();
        assertThat(maxOnly.matches(new Date(maxOnly.getMaxDate().getTime() + 1))).isFalse();
        assertThat(sqlFor(maxOnly)).contains("t.hidden <= " + maxOnly.getMaxDate().getTime()).doesNotContain("t.hidden >=");
    }

    @Test
    public void relativeOneSidedRangesUseLocalDayBoundaries() {
        final ZoneId tokyo = setTimeZone("Asia/Tokyo");
        final DateFilter fromToday = new DateFilter();
        fromToday.setRelativeDays(0, Integer.MAX_VALUE);

        assertThat(toLocalDate(fromToday.getMinDate(), tokyo)).isEqualTo(LocalDate.now(tokyo));
        assertThat(fromToday.getMaxDate()).isNull();
        assertThat(fromToday.matches(new Date(fromToday.getMinDate().getTime() - 1))).isFalse();
        assertThat(fromToday.matches(fromToday.getMinDate())).isTrue();
        assertThat(sqlFor(fromToday)).contains("t.hidden >= " + fromToday.getMinDate().getTime()).doesNotContain("t.hidden <=");

        final ZoneId losAngeles = setTimeZone("America/Los_Angeles");
        final DateFilter throughToday = new DateFilter();
        throughToday.setRelativeDays(Integer.MIN_VALUE, 0);

        assertThat(throughToday.getMinDate()).isNull();
        assertThat(toLocalDate(throughToday.getMaxDate(), losAngeles)).isEqualTo(LocalDate.now(losAngeles));
        assertThat(throughToday.matches(throughToday.getMaxDate())).isTrue();
        assertThat(throughToday.matches(new Date(throughToday.getMaxDate().getTime() + 1))).isFalse();
        assertThat(sqlFor(throughToday)).contains("t.hidden <= " + throughToday.getMaxDate().getTime()).doesNotContain("t.hidden >=");
    }

    @Test
    public void relativeIgnoreYearUsesTheSameLocalMonthDayInMemoryAndSql() {
        final ZoneId zone = setTimeZone("Asia/Tokyo");
        final LocalDate today = LocalDate.now(zone);
        final DateFilter filter = new DateFilter();
        filter.setRelativeDays(0, 0);
        filter.setIgnoreYear(true);

        assertThat(filter.matches(atLocalTime(zone, today.minusDays(1), LocalTime.NOON))).isFalse();
        assertThat(filter.matches(atLocalTime(zone, today, LocalTime.MIDNIGHT))).isTrue();
        assertThat(filter.matches(atLocalTime(zone, today, LocalTime.of(23, 59, 59)))).isTrue();
        assertThat(filter.matches(atLocalTime(zone, today.plusDays(1), LocalTime.MIDNIGHT))).isFalse();

        final String monthDay = String.format(Locale.US, "%02d-%02d", today.getMonthValue(), today.getDayOfMonth());
        assertThat(sqlFor(filter))
                .contains("'unixepoch', 'localtime'")
                .contains(">= '" + monthDay + "'")
                .contains("<= '" + monthDay + "'");
    }

    @Test
    public void ignoreYearUsesLocalMonthDayForWrappedRangeAndSql() {
        final ZoneId zone = setTimeZone("Asia/Tokyo");
        final DateFilter filter = new DateFilter();
        filter.setMinMaxDate(atLocalTime(zone, LocalDate.of(2025, 11, 1), LocalTime.NOON),
                atLocalTime(zone, LocalDate.of(2025, 2, 28), LocalTime.NOON));
        filter.setIgnoreYear(true);

        assertThat(filter.matches(atLocalTime(zone, LocalDate.of(2023, 12, 15), LocalTime.MIDNIGHT))).isTrue();
        assertThat(filter.matches(atLocalTime(zone, LocalDate.of(2024, 1, 15), LocalTime.of(23, 59)))).isTrue();
        assertThat(filter.matches(atLocalTime(zone, LocalDate.of(2024, 3, 1), LocalTime.MIDNIGHT))).isFalse();

        assertThat(sqlFor(filter))
                .contains("'unixepoch', 'localtime'")
                .contains(">= '11-01'")
                .contains("<= '02-28'")
                .contains(" OR ");
    }

    @Test
    public void ignoreYearSupportsOneSidedRanges() {
        final ZoneId zone = setTimeZone("America/Los_Angeles");
        final Date boundary = atLocalTime(zone, LocalDate.of(2025, 4, 6), LocalTime.NOON);

        final DateFilter minOnly = new DateFilter();
        minOnly.setMinMaxDate(boundary, null);
        minOnly.setIgnoreYear(true);
        assertThat(minOnly.matches(atLocalTime(zone, LocalDate.of(2022, 4, 6), LocalTime.MIDNIGHT))).isTrue();
        assertThat(minOnly.matches(atLocalTime(zone, LocalDate.of(2022, 4, 5), LocalTime.of(23, 59)))).isFalse();
        assertThat(sqlFor(minOnly)).contains("'localtime'").contains(">= '04-06'").doesNotContain("<= '04-06'");

        final DateFilter maxOnly = new DateFilter();
        maxOnly.setMinMaxDate(null, boundary);
        maxOnly.setIgnoreYear(true);
        assertThat(maxOnly.matches(atLocalTime(zone, LocalDate.of(2022, 4, 6), LocalTime.of(23, 59)))).isTrue();
        assertThat(maxOnly.matches(atLocalTime(zone, LocalDate.of(2022, 4, 7), LocalTime.MIDNIGHT))).isFalse();
        assertThat(sqlFor(maxOnly)).contains("'localtime'").contains("<= '04-06'").doesNotContain(">= '04-06'");
    }

    @Test
    public void selectedDaySurvivesTokyoToLosAngelesChangeBeforeSerialization() {
        assertSelectedDaySurvivesTimeZoneChange("Asia/Tokyo", "America/Los_Angeles", LocalDate.of(2025, 1, 1));
    }

    @Test
    public void selectedDaySurvivesLosAngelesToTokyoChangeBeforeSerialization() {
        assertSelectedDaySurvivesTimeZoneChange("America/Los_Angeles", "Asia/Tokyo", LocalDate.of(2025, 1, 1));
    }

    @Test
    public void selectedDayGetsDstAwareBoundariesAfterTimeZoneChange() {
        assertSelectedDayDurationAfterTimeZoneChange("Asia/Tokyo", LocalDate.of(2024, 3, 10), Duration.ofHours(23));
        assertSelectedDayDurationAfterTimeZoneChange("Asia/Tokyo", LocalDate.of(2024, 11, 3), Duration.ofHours(25));
    }

    @Test
    public void exactDateFilterKeepsInstantsAfterTimeZoneChange() {
        setTimeZone("Asia/Tokyo");
        final Date exact = Date.from(Instant.parse("2025-01-01T00:00:00.123Z"));
        final DateFilter filter = new DateFilter(false);
        filter.setMinMaxDate(exact, exact);

        setTimeZone("America/Los_Angeles");
        assertThat(filter.getConfiguredMinDate()).isEqualTo(exact);
        assertThat(filter.getConfiguredMaxDate()).isEqualTo(exact);
        assertThat(filter.getMinDate()).isEqualTo(exact);
        assertThat(filter.getMaxDate()).isEqualTo(exact);
        assertThat(filter.matches(new Date(exact.getTime() - 1))).isFalse();
        assertThat(filter.matches(exact)).isTrue();
        assertThat(filter.matches(new Date(exact.getTime() + 1))).isFalse();
        assertThat(sqlFor(filter))
                .contains("t.hidden >= " + exact.getTime())
                .contains("t.hidden <= " + exact.getTime());
    }

    @Test
    public void legacyAndJsonConfigurationsUseCurrentTimeZoneWithoutCachedFormatters() {
        // Touch DateFilter while UTC is the default, then change the default time zone in the same process.
        setTimeZone("UTC");
        new DateFilter().getConfig();

        final ZoneId tokyo = setTimeZone("Asia/Tokyo");
        final LocalDate selectedDay = LocalDate.of(2025, 1, 1);
        final DateFilter configured = filterForDay(tokyo, selectedDay);
        final List<String> legacyConfig = configured.getConfig();
        final ObjectNode jsonConfig = configured.getJsonConfig();

        assertThat(legacyConfig).startsWith("2025-01-01", "2025-01-01");
        assertThat(jsonConfig.get("min").asText()).startsWith("2025-01-01T00:00:00");
        assertThat(jsonConfig.get("max").asText()).startsWith("2025-01-01T23:59:59");

        // Date-only configurations preserve their wall-clock date when restored in a new time zone.
        final ZoneId losAngeles = setTimeZone("America/Los_Angeles");
        final DateFilter fromLegacy = new DateFilter();
        fromLegacy.setConfig(legacyConfig);
        final DateFilter fromJson = new DateFilter();
        fromJson.setJsonConfig(jsonConfig);

        assertThat(toLocalDate(fromLegacy.getMinDate(), losAngeles)).isEqualTo(selectedDay);
        assertThat(toLocalDate(fromLegacy.getMaxDate(), losAngeles)).isEqualTo(selectedDay);
        assertThat(toLocalDate(fromJson.getMinDate(), losAngeles)).isEqualTo(selectedDay);
        assertThat(toLocalDate(fromJson.getMaxDate(), losAngeles)).isEqualTo(selectedDay);
        assertThat(sqlFor(fromJson)).contains("t.hidden >= " + fromJson.getMinDate().getTime())
                .contains("t.hidden <= " + fromJson.getMaxDate().getTime());
    }

    private void assertSelectedDaySurvivesTimeZoneChange(final String sourceZoneId, final String targetZoneId,
            final LocalDate selectedDay) {
        final ZoneId sourceZone = setTimeZone(sourceZoneId);
        final DateFilter filter = filterForDay(sourceZone, selectedDay);

        final ZoneId targetZone = setTimeZone(targetZoneId);
        final Instant expectedStart = selectedDay.atStartOfDay(targetZone).toInstant();
        final Instant expectedEnd = selectedDay.plusDays(1).atStartOfDay(targetZone).toInstant().minusMillis(1);
        assertThat(filter.getMinDate()).isEqualTo(Date.from(expectedStart));
        assertThat(filter.getMaxDate()).isEqualTo(Date.from(expectedEnd));
        assertThat(toLocalDate(filter.getConfiguredMinDate(), targetZone)).isEqualTo(selectedDay);
        assertThat(toLocalDate(filter.getConfiguredMaxDate(), targetZone)).isEqualTo(selectedDay);
        assertRangeBoundaries(filter);
        assertThat(filter.getUserDisplayableConfig()).isEqualTo("2025-01-01");
        assertThat(sqlFor(filter))
                .contains("t.hidden >= " + expectedStart.toEpochMilli())
                .contains("t.hidden <= " + expectedEnd.toEpochMilli());

        final List<String> legacyConfig = filter.getConfig();
        final ObjectNode jsonConfig = filter.getJsonConfig();
        assertThat(legacyConfig).startsWith("2025-01-01", "2025-01-01");
        assertThat(jsonConfig.get("min").asText()).isEqualTo("2025-01-01T00:00:00");
        assertThat(jsonConfig.get("max").asText()).isEqualTo("2025-01-01T23:59:59");

        final DateFilter fromLegacy = new DateFilter();
        fromLegacy.setConfig(legacyConfig);
        final DateFilter fromJson = new DateFilter();
        fromJson.setJsonConfig(jsonConfig);
        assertThat(toLocalDate(fromLegacy.getMinDate(), targetZone)).isEqualTo(selectedDay);
        assertThat(toLocalDate(fromLegacy.getMaxDate(), targetZone)).isEqualTo(selectedDay);
        assertThat(toLocalDate(fromJson.getMinDate(), targetZone)).isEqualTo(selectedDay);
        assertThat(toLocalDate(fromJson.getMaxDate(), targetZone)).isEqualTo(selectedDay);

        filter.setIgnoreYear(true);
        assertThat(filter.matches(atLocalTime(targetZone, selectedDay.minusYears(2), LocalTime.NOON))).isTrue();
        assertThat(filter.matches(atLocalTime(targetZone, selectedDay.plusDays(1), LocalTime.MIDNIGHT))).isFalse();
        assertThat(filter.getUserDisplayableConfig()).isEqualTo("01-01(y)");
        assertThat(sqlFor(filter))
                .contains("'unixepoch', 'localtime'")
                .contains(">= '01-01'")
                .contains("<= '01-01'");
    }

    private void assertSelectedDayDurationAfterTimeZoneChange(final String sourceZoneId, final LocalDate selectedDay,
            final Duration expectedDuration) {
        final ZoneId sourceZone = setTimeZone(sourceZoneId);
        final DateFilter filter = filterForDay(sourceZone, selectedDay);
        final ZoneId newYork = setTimeZone("America/New_York");

        assertThat(toLocalDate(filter.getMinDate(), newYork)).isEqualTo(selectedDay);
        assertThat(toLocalDate(filter.getMaxDate(), newYork)).isEqualTo(selectedDay);
        assertThat(filter.getMaxDate().getTime() - filter.getMinDate().getTime() + 1).isEqualTo(expectedDuration.toMillis());
        assertRangeBoundaries(filter);
        assertThat(sqlFor(filter))
                .contains("t.hidden >= " + filter.getMinDate().getTime())
                .contains("t.hidden <= " + filter.getMaxDate().getTime());
    }

    private void assertAbsoluteDayBoundaries(final String zoneId, final LocalDate selectedDay) {
        final ZoneId zone = setTimeZone(zoneId);
        final DateFilter filter = filterForDay(zone, selectedDay);

        assertThat(toLocalDate(filter.getMinDate(), zone)).isEqualTo(selectedDay);
        assertThat(toLocalDate(filter.getMaxDate(), zone)).isEqualTo(selectedDay);
        assertRangeBoundaries(filter);
        assertThat(sqlFor(filter))
                .contains("t.hidden >= " + filter.getMinDate().getTime())
                .contains("t.hidden <= " + filter.getMaxDate().getTime())
                .doesNotContain("date(")
                .doesNotContain("unixepoch");
    }

    private static void assertRangeBoundaries(final DateFilter filter) {
        assertThat(filter.matches(new Date(filter.getMinDate().getTime() - 1))).isFalse();
        assertThat(filter.matches(filter.getMinDate())).isTrue();
        assertThat(filter.matches(filter.getMaxDate())).isTrue();
        assertThat(filter.matches(new Date(filter.getMaxDate().getTime() + 1))).isFalse();
    }

    private static DateFilter filterForDay(final ZoneId zone, final LocalDate day) {
        final DateFilter filter = new DateFilter();
        final Date selected = atLocalTime(zone, day, LocalTime.NOON);
        filter.setMinMaxDate(selected, selected);
        return filter;
    }

    private static Date atLocalTime(final ZoneId zone, final LocalDate day, final LocalTime time) {
        return Date.from(day.atTime(time).atZone(zone).toInstant());
    }

    private static LocalDate toLocalDate(final Date date, final ZoneId zone) {
        return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDate();
    }

    private static ZoneId setTimeZone(final String zoneId) {
        final TimeZone timeZone = TimeZone.getTimeZone(zoneId);
        TimeZone.setDefault(timeZone);
        return timeZone.toZoneId();
    }

    private static String sqlFor(final DateFilter filter) {
        final SqlBuilder sqlBuilder = new SqlBuilder("caches", new String[] {"hidden"});
        filter.addToSql(sqlBuilder, "t.hidden");
        return sqlBuilder.getSql();
    }
}
