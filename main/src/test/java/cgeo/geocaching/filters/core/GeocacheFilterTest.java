package cgeo.geocaching.filters.core;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheFilterTest {

    @Test
    public void getFilterNameChanged() {
        final String filterName = "FilterName";
        final String purifiedName = "(FilterName)*";
        assertThat(GeocacheFilter.getFilterName(filterName, true)).isEqualTo(purifiedName);

        final String filterNameAsterix = "(FilterName*)*";
        final String purifiedNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix);

        final String filterNameBrackets = "((FilterName))*";
        final String purifiedNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets);
    }

    @Test
    public void getFilterNameUnchanged() {
        final String filterName = "FilterName";
        assertThat(GeocacheFilter.getFilterName(filterName, false)).isEqualTo(filterName);

        final String filterNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix);

        final String filterNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets);
    }

    @Test
    public void getPurifiedFilterNameChanged() {
        final String filterName = "(FilterName)*";
        final String purifiedName = "FilterName";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterName)).isEqualTo(purifiedName);

        final String filterNameAsterix = "(FilterName*)*";
        final String purifiedNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix);

        final String filterNameBrackets = "((FilterName))*";
        final String purifiedNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets);
    }

    @Test
    public void getPurifiedFilterNameUnchanged() {
        final String filterName = "FilterName";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterName)).isEqualTo(filterName);

        final String filterNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix);

        final String filterNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets);
    }
}
