package cgeo.geocaching.connector.al;

import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.LastFoundGeocacheFilter;
import cgeo.geocaching.filters.core.StoredSinceGeocacheFilter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ALApiTest {

    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void noPublicationDateDoesNotRestrictSearch() throws ParseException {
        assertThat(ALApi.getRecentlyPublishedDays(filters(), date("2026-02-20 12:00"))).isNull();
    }

    @Test
    public void maxOnlyPublicationDateDoesNotRestrictSearch() throws ParseException {
        final HiddenGeocacheFilter hidden = GeocacheFilterType.HIDDEN.create();
        hidden.setMinMaxDate(null, date("2026-02-20 00:00"));

        assertThat(ALApi.getRecentlyPublishedDays(filters(hidden), date("2026-02-20 12:00"))).isNull();
    }

    @Test
    public void publicationMinDateUsesSafeLookback() throws ParseException {
        final HiddenGeocacheFilter hidden = GeocacheFilterType.HIDDEN.create();
        hidden.setMinMaxDate(date("2026-02-10 00:00"), null);

        // Round up to include the full first day instead of excluding its first twelve hours.
        assertThat(ALApi.getRecentlyPublishedDays(filters(hidden), date("2026-02-20 12:00"))).isEqualTo(11);
    }

    @Test
    public void nonPublicationDatesDoNotRestrictSearch() throws ParseException {
        final HiddenGeocacheFilter eventDate = GeocacheFilterType.EVENT_DATE.create();
        eventDate.setMinMaxDate(date("2026-02-10 00:00"), null);
        final LastFoundGeocacheFilter lastFound = GeocacheFilterType.LAST_FOUND.create();
        lastFound.setMinMaxDate(date("2026-02-10 00:00"), null);
        final StoredSinceGeocacheFilter storedSince = GeocacheFilterType.STORED_SINCE.create();
        storedSince.setMinMaxDate(date("2026-02-10 00:00"), null);
        final Date now = date("2026-02-20 12:00");

        assertThat(ALApi.getRecentlyPublishedDays(filters(eventDate), now)).isNull();
        assertThat(ALApi.getRecentlyPublishedDays(filters(lastFound), now)).isNull();
        assertThat(ALApi.getRecentlyPublishedDays(filters(storedSince), now)).isNull();
    }

    @Test
    public void actualPublicationDateIsUsedWhenOtherDateFiltersComeFirst() throws ParseException {
        final LastFoundGeocacheFilter lastFound = GeocacheFilterType.LAST_FOUND.create();
        lastFound.setMinMaxDate(date("2026-02-19 00:00"), null);
        final HiddenGeocacheFilter hidden = GeocacheFilterType.HIDDEN.create();
        hidden.setMinMaxDate(date("2026-02-10 00:00"), null);
        final Date now = date("2026-02-20 00:00");
        final List<BaseGeocacheFilter> filters = Arrays.asList(lastFound, hidden);

        assertThat(ALApi.getRecentlyPublishedDays(filters, now)).isEqualTo(10);
    }

    @Test
    public void unsupportedPublicationDateModesDoNotRestrictSearch() throws ParseException {
        final HiddenGeocacheFilter ignoreYear = GeocacheFilterType.HIDDEN.create();
        ignoreYear.setMinMaxDate(date("2026-02-10 00:00"), null);
        ignoreYear.getDateFilter().setIgnoreYear(true);
        final HiddenGeocacheFilter future = GeocacheFilterType.HIDDEN.create();
        future.setMinMaxDate(date("2026-02-21 00:00"), null);
        final Date now = date("2026-02-20 12:00");

        assertThat(ALApi.getRecentlyPublishedDays(filters(ignoreYear), now)).isNull();
        assertThat(ALApi.getRecentlyPublishedDays(filters(future), now)).isNull();
    }

    private static List<BaseGeocacheFilter> filters(final BaseGeocacheFilter... filters) {
        return Arrays.asList(filters);
    }

    private static Date date(final String value) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(value);
    }
}
