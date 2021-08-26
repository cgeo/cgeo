package cgeo.geocaching.filters;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.DateFilter;
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter;
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.LastFoundGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.utils.functions.Action2;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

public class UserDisplayableStringTest {

    @Test
    public void testDifficulty() {
        final DifficultyGeocacheFilter f = GeocacheFilterType.DIFFICULTY.create();

        final float minValue = 2f;
        final float maxValue = 4f;
        final String minValueOutput = "2.0";
        final String maxValueOutput = "4.0";

        testUserDisplayStrings(f, (min, max) -> f.setMinMaxRange(min, max), minValue, maxValue, minValueOutput, maxValueOutput);
    }

    @Test
    public void testFavoritesPercentage() {
        final FavoritesGeocacheFilter f = GeocacheFilterType.FAVORITES.create();
        f.setPercentage(true);

        final float minValue = 2f;
        final float maxValue = 4f;
        final String minValueOutput = "200%";
        final String maxValueOutput = "400%";

        testUserDisplayStrings(f, (min, max) -> f.setMinMaxRange(min, max), minValue, maxValue, minValueOutput, maxValueOutput);
    }

    @Test
    public void testLastFound() {
        final LastFoundGeocacheFilter f = GeocacheFilterType.LAST_FOUND.create();

        Locale.setDefault(Locale.US);

        final Date minValue;
        final Date maxValue;
        try {
            minValue = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.parse("2022-04-06");
            maxValue = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.parse("2022-06-06");
        } catch (ParseException pe) {
            fail("Problem parsing the test dates");
            return;
        }

        final String minValueOutput = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.format(minValue);
        final String maxValueOutput = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.format(maxValue);

        testUserDisplayStrings(f, (min, max) -> f.setMinMaxDate(min, max), minValue, maxValue, minValueOutput, maxValueOutput);
    }

    @Test
    public void testAndFilter() {
        final TypeGeocacheFilter typeFilter = GeocacheFilterType.TYPE.create();

        final List<CacheType> testTypes = new Stack<>();
        testTypes.add(CacheType.TRADITIONAL);
        typeFilter.setValues(testTypes);

        final AndGeocacheFilter filterConfig = new AndGeocacheFilter();
        filterConfig.addChild(typeFilter);
        final StatusGeocacheFilter statusFilter = GeocacheFilterType.STATUS.create();
        statusFilter.setStatusFound(true);
        statusFilter.setStatusOwned(true);
        filterConfig.addChild(statusFilter);

        final GeocacheFilter gcFilter =  GeocacheFilter.create("", false, false, filterConfig );

        final String valueOutput = "Cache Type: Trad, Status: Found=Yes, Owned=Yes";

        assertThat(gcFilter.toUserDisplayableString()).as("display for filter").isEqualTo(valueOutput);
    }

    private <T> void testSingleUserDisplayString(final IGeocacheFilter filter, final Action2<T, T> filterSetter,
                                                 final T minValue, final T maxValue,
                                                 final String valueOutput) {
        final int displayLevel = 1;
        if (filterSetter != null) {
            filterSetter.call(minValue, maxValue);
        }

        final String filterName = filter.getType().getUserDisplayableName() + ": ";
        assertThat(filter.toUserDisplayableString(displayLevel)).as("display for filter").isEqualTo(valueOutput != null ? filterName + valueOutput : null);
    }

    private <T> void testUserDisplayStrings(final IGeocacheFilter filter, final Action2<T, T> filterSetter,
                                            final T minValue, final T maxValue,
                                            final String minValueOutput, final String maxValueOutput) {
        testSingleUserDisplayString(filter, filterSetter, null, null, null);
        testSingleUserDisplayString(filter, filterSetter, minValue, null, ">" + minValueOutput);
        testSingleUserDisplayString(filter, filterSetter, minValue, maxValue, minValueOutput + "-" + maxValueOutput);
        testSingleUserDisplayString(filter, filterSetter, null, maxValue, "<" + maxValueOutput);
        testSingleUserDisplayString(filter, filterSetter, maxValue, maxValue, maxValueOutput);
    }
}
