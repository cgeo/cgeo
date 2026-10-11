package cgeo.geocaching.connector.oc;

import cgeo.geocaching.SearchCacheData;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class OkapiClientTest {

    private static final String AUTHOR = "Alice";

    @Test
    public void finderSearchMetadataReflectsFilterDirection() {
        final LogEntryGeocacheFilter positiveFilter = createFilter(false);
        final SearchCacheData positiveData = OkapiClient.createFinderSearchCacheData(positiveFilter);
        assertThat(positiveData).isNotNull();
        assertThat(positiveData.getFoundBy()).containsExactly(AUTHOR);
        assertThat(positiveData.getNotFoundBy()).isEmpty();

        final LogEntryGeocacheFilter inverseFilter = createFilter(true);
        final SearchCacheData inverseData = OkapiClient.createFinderSearchCacheData(inverseFilter);
        assertThat(inverseData).isNotNull();
        assertThat(inverseData.getFoundBy()).isEmpty();
        assertThat(inverseData.getNotFoundBy()).containsExactly(AUTHOR);
    }

    @Test
    public void emptyFinderFilterCreatesNoSearchMetadata() {
        assertThat(OkapiClient.createFinderSearchCacheData(null)).isNull();
        assertThat(OkapiClient.createFinderSearchCacheData(new LogEntryGeocacheFilter())).isNull();
    }

    private static LogEntryGeocacheFilter createFilter(final boolean inverse) {
        final LogEntryGeocacheFilter filter = new LogEntryGeocacheFilter();
        filter.setFoundByUser(AUTHOR);
        filter.setInverse(inverse);
        return filter;
    }
}
