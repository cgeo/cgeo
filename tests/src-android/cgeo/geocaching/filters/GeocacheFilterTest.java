package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheFilterTest {

    @Test
    public void emptyFilter() {
        assertFilterFromConfig("", "", null);
        assertFilterFromConfig(null, "", null);
        assertFilterFromConfig("   ", "", null);
    }

    @Test
    public void emptyFilterWithName() {
        assertFilterFromConfig("[myname]", "myname", null);
        assertFilterFromConfig("[myname", "myname", null);
        assertFilterFromConfig("[]", "", null);
        assertFilterFromConfig("[test\\]\\:test]", "test]:test", null);
    }

    @Test
    public void emptyNameWithFilter() {
        assertFilterFromConfig("name", "", NameGeocacheFilter.class);
    }

    @Test
    public void bothfilled() {
        assertFilterFromConfig("[myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertFilterFromConfig("[myfilter] name", "myfilter", NameGeocacheFilter.class);
        assertFilterFromConfig("[myfilter] AND(name)", "myfilter", AndGeocacheFilter.class);
    }

    @Test
    public void checkInconclusive() {
        GeocacheFilter filter = assertFilterFromConfig("[myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isIncludeInconclusive()).isFalse();
        filter = assertFilterFromConfig("[inconclusive=true:myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isIncludeInconclusive()).isTrue();
    }

    @Test
    public void checkAdvancedView() {
        GeocacheFilter filter = assertFilterFromConfig("[myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isOpenInAdvancedMode()).isFalse();
        filter = assertFilterFromConfig("[advanced=true:myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertThat(filter.isOpenInAdvancedMode()).isTrue();
    }

    private GeocacheFilter assertFilterFromConfig(final String config, final String expectedName, final Class<? extends IGeocacheFilter> expectedFilterClass) {
        final GeocacheFilter filter = GeocacheFilter.createFromConfig(config);
        assertThat(filter.getName()).as("name for ' " + config + "'").isEqualTo(expectedName);
        assertThat(filter.getTree() == null ? null : filter.getTree().getClass()).as("treeclass for ' " + config + "'").isEqualTo(expectedFilterClass);
        return filter;
    }
}
