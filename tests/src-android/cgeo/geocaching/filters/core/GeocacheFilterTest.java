package cgeo.geocaching.filters.core;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheFilterTest {

    @Test
    public void emptyFilter() {
        assertFilterFromConfig("",  "", null);
        assertFilterFromConfig(null,  "", null);
        assertFilterFromConfig("   ",  "", null);
    }

    @Test
    public void emptyFilterWithName() {
        assertFilterFromConfig("[myname]",  "myname", null);
        assertFilterFromConfig("[myname",  "myname", null);
        assertFilterFromConfig("[]",  "", null);
        assertFilterFromConfig("[test\\]:test]",  "test]:test", null);
    }

    @Test
    public void emptyNameWithFilter() {
        assertFilterFromConfig("name", "", NameGeocacheFilter.class);
    }

    @Test
    public void bothfilled() {
        assertFilterFromConfig("[myfilter]name", "myfilter", NameGeocacheFilter.class);
        assertFilterFromConfig("[myfilter] name", "myfilter", NameGeocacheFilter.class);
        assertFilterFromConfig("[myfilter] AND(name)",  "myfilter", AndGeocacheFilter.class);
    }


    private void assertFilterFromConfig(final String config, final String expectedName, final Class<? extends IGeocacheFilter> expectedFilterClass) {
        final GeocacheFilter filter = GeocacheFilter.createFromConfig(config);
        assertThat(filter.getName()).as("name for ' " + config + "'").isEqualTo(expectedName);
        assertThat(filter.getTree() == null ? null : filter.getTree().getClass()).as("treeclass for ' " + config + "'").isEqualTo(expectedFilterClass);

    }
}
